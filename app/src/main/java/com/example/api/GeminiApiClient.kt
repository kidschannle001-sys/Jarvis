package com.example.api

import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.JarvisAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class GeminiResponse(
    val replyText: String,
    val action: JarvisAction? = null,
    val rawJson: String? = null
)

class GeminiApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val primaryModel = "gemini-2.5-flash"
    private val fallbackModel = "gemini-1.5-flash"

    private val systemInstructionText = """
You are JARVIS, my personal AI assistant.

Speak naturally and intelligently.
Understand Hindi, Hinglish, and English.
Respond in the language I use.
Be helpful, friendly, concise, and practical.

Understand my instructions before acting.
Maintain conversation context during the session.
Explain important actions before performing them.
Never claim that an action succeeded unless verified.
If an action fails, explain the reason.
Ask for confirmation before sensitive actions.
Respect privacy and permissions.
Stop the current task immediately when requested.

You are an assistant, not an unrestricted controller.
Only perform actions supported by the Android application and the permissions granted by the user.

Supported actions format (if an action is requested):
Allowed action types:
- OPEN_APP: target is app name (e.g. "youtube", "chrome", "whatsapp", "settings", "camera", "maps", "calculator", "calendar", "dialer")
- OPEN_SETTINGS: target is setting name ("accessibility", "notifications", "wireless", "main")
- READ_VISIBLE_SCREEN: target is empty
- TAP_ACCESSIBLE_CONTROL: target is button label or text to tap
- SCROLL_SCREEN: extra is "down" or "up"
- GO_BACK: target is empty
- PREPARE_MESSAGE: target is contact name, extra is message body
- START_CALL: target is contact name or phone number
- READ_SUPPORTED_NOTIFICATION: target is empty
- READ_CALENDAR_EVENT: target is empty
- STOP_TASK: target is empty

Always reply in strict JSON format:
{
  "speech_reply": "Your conversational answer in Hindi/Hinglish/English",
  "action": {
    "type": "ACTION_TYPE_NAME_OR_NONE",
    "target": "target identifier if any",
    "extra": "extra details if any",
    "requires_confirmation": boolean,
    "description": "Short explanation of the action"
  }
}
If no action is required, set "action": null.
""".trimIndent()

    suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$primaryModel:generateContent?key=${apiKey.trim()}"
        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Respond with 'OK' only."))
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseBody = response.body?.string().orEmpty()

                when (code) {
                    200 -> Result.success("Connection successful! Gemini API is active and ready.")
                    400 -> Result.failure(Exception("HTTP 400: Invalid API Key or malformed request. Please check your key from Google AI Studio."))
                    403 -> Result.failure(Exception("HTTP 403: API Key permission denied or unauthorized. Ensure Generative Language API is enabled."))
                    404 -> {
                        // Fallback test with gemini-1.5-flash
                        testFallback(apiKey)
                    }
                    429 -> Result.failure(Exception("HTTP 429: Rate limit or free quota exhausted. Please try again in a few moments."))
                    500, 503 -> Result.failure(Exception("HTTP $code: Google AI service temporary disruption. Try again later."))
                    else -> Result.failure(Exception("API Error (Code $code): $responseBody"))
                }
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Unable to reach Gemini servers. Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Connection test error: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private fun testFallback(apiKey: String): Result<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$fallbackModel:generateContent?key=${apiKey.trim()}"
        val bodyJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Test"))
                    })
                })
            })
        }
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success("Connection verified with Gemini 1.5 Flash fallback.")
                } else {
                    Result.failure(Exception("Gemini test failed with status ${response.code}."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Fallback test failed: ${e.message}"))
        }
    }

    suspend fun generateResponse(
        apiKey: String,
        prompt: String,
        history: List<ChatMessage> = emptyList(),
        screenContext: String? = null
    ): Result<GeminiResponse> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API Key is missing. Please set your Gemini API key in settings."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$primaryModel:generateContent?key=${apiKey.trim()}"

        try {
            val rootJson = JSONObject()

            // System Instruction
            rootJson.put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", systemInstructionText))
                })
            })

            // Contents array (Conversation History + Current Message)
            val contentsArray = JSONArray()

            // Add recent history (up to last 6 messages)
            for (msg in history.takeLast(6)) {
                val role = if (msg.role == "user") "user" else "model"
                contentsArray.put(JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", msg.content))
                    })
                })
            }

            // Current prompt with optional screen context
            val currentText = StringBuilder()
            if (!screenContext.isNullOrBlank()) {
                currentText.append("[Visible Screen Content:\n$screenContext\n]\n")
            }
            currentText.append(prompt)

            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().put("text", currentText.toString()))
                })
            })

            rootJson.put("contents", contentsArray)

            // Generation config with JSON output mode
            rootJson.put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.7)
            })

            val request = Request.Builder()
                .url(url)
                .post(rootJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val code = response.code
                val responseStr = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    val errMsg = when (code) {
                        400 -> "Invalid API key or bad request parameters."
                        403 -> "Permission denied. Check if Gemini API is enabled in your Google AI Studio project."
                        429 -> "Quota limit exceeded. Please wait a moment before sending next command."
                        else -> "Gemini API error (Status $code): $responseStr"
                    }
                    return@withContext Result.failure(Exception(errMsg))
                }

                // Parse candidates
                val parsedRoot = JSONObject(responseStr)
                val candidates = parsedRoot.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No response generated by model."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val textOutput = parts?.optJSONObject(0)?.optString("text").orEmpty()

                val geminiResponse = parseModelOutput(textOutput)
                Result.success(geminiResponse)
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: Please check your internet connection."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to generate response: ${e.localizedMessage ?: "Unknown error"}"))
        }
    }

    private fun parseModelOutput(rawText: String): GeminiResponse {
        val trimmed = rawText.trim()
        try {
            // Attempt JSON parse
            val jsonStart = trimmed.indexOf('{')
            val jsonEnd = trimmed.lastIndexOf('}')
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonString = trimmed.substring(jsonStart, jsonEnd + 1)
                val json = JSONObject(jsonString)

                val speechReply = json.optString("speech_reply", "")
                val actionObj = json.optJSONObject("action")

                var action: JarvisAction? = null
                if (actionObj != null) {
                    val typeStr = actionObj.optString("type", "NONE")
                    val actionType = try {
                        ActionType.valueOf(typeStr.uppercase())
                    } catch (_: Exception) {
                        ActionType.NONE
                    }

                    if (actionType != ActionType.NONE) {
                        action = JarvisAction(
                            type = actionType,
                            target = actionObj.optString("target", ""),
                            extra = actionObj.optString("extra", ""),
                            requiresConfirmation = actionObj.optBoolean("requires_confirmation", false),
                            description = actionObj.optString("description", "")
                        )
                    }
                }

                if (speechReply.isNotBlank()) {
                    return GeminiResponse(replyText = speechReply, action = action, rawJson = jsonString)
                }
            }
        } catch (_: Exception) {
            // Fallback to raw text
        }

        // Fallback: If not JSON, return cleaned text directly
        val cleaned = trimmed.replace("```json", "").replace("```", "").trim()
        return GeminiResponse(replyText = cleaned, action = null)
    }
}
