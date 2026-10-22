package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.data.local.JarvisDatabase
import com.example.data.model.ActionType
import com.example.data.model.ChatMessage
import com.example.data.model.JarvisAction
import com.example.data.model.JarvisTaskStatus
import com.example.data.preferences.UserPreferences
import com.example.data.security.SecureStorage
import com.example.permission.PermissionManager
import com.example.permission.PermissionModel
import com.example.permission.PermissionState
import com.example.phone.HapticType
import com.example.phone.PhoneActionDispatcher
import com.example.voice.SpeechListener
import com.example.voice.SpeechRecognitionManager
import com.example.voice.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ApiConnectionStatus {
    object Idle : ApiConnectionStatus()
    object Testing : ApiConnectionStatus()
    data class Success(val message: String) : ApiConnectionStatus()
    data class Error(val message: String) : ApiConnectionStatus()
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val secureStorage = SecureStorage(application)
    private val geminiClient = GeminiApiClient()
    private val database = JarvisDatabase.getDatabase(application)
    private val chatDao = database.chatDao()
    val preferences = UserPreferences(application)
    val phoneDispatcher = PhoneActionDispatcher(application)
    val permissionManager = PermissionManager(application)

    // State Flows
    private val _taskStatus = MutableStateFlow(JarvisTaskStatus.IDLE)
    val taskStatus: StateFlow<JarvisTaskStatus> = _taskStatus.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _apiConnectionStatus = MutableStateFlow<ApiConnectionStatus>(ApiConnectionStatus.Idle)
    val apiConnectionStatus: StateFlow<ApiConnectionStatus> = _apiConnectionStatus.asStateFlow()

    private val _maskedApiKey = MutableStateFlow(
        secureStorage.getApiKey()?.let { secureStorage.maskApiKey(it) } ?: ""
    )
    val maskedApiKey: StateFlow<String> = _maskedApiKey.asStateFlow()

    private val _hasApiKey = MutableStateFlow(secureStorage.hasApiKey())
    val hasApiKey: StateFlow<Boolean> = _hasApiKey.asStateFlow()

    private val _pendingAction = MutableStateFlow<JarvisAction?>(null)
    val pendingAction: StateFlow<JarvisAction?> = _pendingAction.asStateFlow()

    private val _permissions = MutableStateFlow<List<PermissionModel>>(emptyList())
    val permissions: StateFlow<List<PermissionModel>> = _permissions.asStateFlow()

    private val _statusMessage = MutableStateFlow("JARVIS ready. Say \"Jarvis\" or press mic.")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    val chatMessages: StateFlow<List<ChatMessage>> = chatDao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Voice & Speech Engines
    private var speechRecognizerManager: SpeechRecognitionManager? = null
    private var ttsManager: TtsManager? = null
    private var activeJob: Job? = null

    init {
        initSpeechEngines()
        refreshPermissions()
    }

    private fun initSpeechEngines() {
        val app = getApplication<Application>()
        ttsManager = TtsManager(app) { speaking ->
            _isSpeaking.value = speaking
            if (!speaking && _taskStatus.value == JarvisTaskStatus.EXECUTING) {
                _taskStatus.value = JarvisTaskStatus.IDLE
            }
        }

        speechRecognizerManager = SpeechRecognitionManager(app, object : SpeechListener {
            override fun onReadyForSpeech() {
                _taskStatus.value = JarvisTaskStatus.LISTENING
                _statusMessage.value = "Sun raha hoon... (Listening...)"
                phoneDispatcher.vibrate(HapticType.START)
            }

            override fun onRmsChanged(rmsdB: Float) {
                _rmsLevel.value = rmsdB
            }

            override fun onPartialResults(partialText: String) {
                _currentTranscript.value = partialText
                // Quick check for "Jarvis, stop" in partial stream
                if (isStopCommand(partialText)) {
                    stopAll()
                }
            }

            override fun onFinalResults(resultText: String) {
                _currentTranscript.value = resultText
                _taskStatus.value = JarvisTaskStatus.THINKING
                if (isStopCommand(resultText)) {
                    stopAll()
                    return
                }
                processUserPrompt(resultText)
            }

            override fun onError(errorMessage: String) {
                _taskStatus.value = JarvisTaskStatus.IDLE
                _statusMessage.value = errorMessage
                _rmsLevel.value = 0f
            }

            override fun onSpeechEnded() {
                _rmsLevel.value = 0f
                if (_taskStatus.value == JarvisTaskStatus.LISTENING) {
                    _taskStatus.value = JarvisTaskStatus.THINKING
                }
            }
        })
    }

    private fun isStopCommand(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower == "jarvis stop" ||
                lower == "stop" ||
                lower == "ruk jao" ||
                lower == "jarvis ruk jao" ||
                lower == "jarvis ruko" ||
                lower.endsWith("stop")
    }

    fun refreshPermissions() {
        _permissions.value = permissionManager.checkAllPermissions(
            isTtsReady = ttsManager?.isEngineAvailable() ?: false
        )
    }

    // Voice Controls
    fun startListening() {
        if (!permissionManager.checkAllPermissions().any { it.id == "mic" && it.state == PermissionState.GRANTED }) {
            _statusMessage.value = "Microphone permission zaroori hai. Permission Manager se allow karein."
            return
        }
        ttsManager?.stop()
        _currentTranscript.value = ""
        speechRecognizerManager?.startListening(preferences.preferredLanguage)
    }

    fun stopListening() {
        speechRecognizerManager?.stopListening()
        if (_taskStatus.value == JarvisTaskStatus.LISTENING) {
            _taskStatus.value = JarvisTaskStatus.IDLE
        }
    }

    fun toggleListening() {
        if (_taskStatus.value == JarvisTaskStatus.LISTENING) {
            stopListening()
        } else {
            startListening()
        }
    }

    // Main AI Engine
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        if (isStopCommand(text)) {
            stopAll()
            return
        }
        processUserPrompt(text.trim())
    }

    private fun processUserPrompt(promptText: String) {
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            _taskStatus.value = JarvisTaskStatus.THINKING
            _statusMessage.value = "Soch raha hoon... (Thinking...)"

            // Store user message in Room
            chatDao.insertMessage(ChatMessage(role = "user", content = promptText))

            val apiKey = secureStorage.getApiKey()
            if (apiKey.isNullOrBlank()) {
                val errorMsg = "Gemini API key setup nahi hai. Kripya API Setup screen se apna API key save karein."
                _taskStatus.value = JarvisTaskStatus.FAILED
                _statusMessage.value = errorMsg
                speakResponse(errorMsg)
                chatDao.insertMessage(ChatMessage(role = "model", content = errorMsg))
                return@launch
            }

            // Optional screen reading context if user asks "screen par kya likha hai"
            val screenContext = if (promptText.lowercase().contains("screen")) {
                phoneDispatcher.readScreen().details
            } else null

            val history = chatDao.getRecentMessages(6).reversed()
            val result = geminiClient.generateResponse(apiKey, promptText, history, screenContext)

            result.fold(
                onSuccess = { response ->
                    val reply = response.replyText
                    val action = response.action

                    if (action != null && action.type != ActionType.NONE) {
                        if (action.requiresConfirmation) {
                            _pendingAction.value = action
                            _taskStatus.value = JarvisTaskStatus.WAITING_FOR_CONFIRMATION
                            _statusMessage.value = "Confirmation darkaar: ${action.description}"
                            val confirmationSpeech = "$reply. Kya aap yeh action execute karna chahte hain?"
                            speakResponse(confirmationSpeech)
                            chatDao.insertMessage(
                                ChatMessage(
                                    role = "model",
                                    content = confirmationSpeech,
                                    actionType = action.type.name,
                                    actionPayload = action.target,
                                    actionStatus = "WAITING_CONFIRMATION"
                                )
                            )
                        } else {
                            // Immediate safe action
                            _taskStatus.value = JarvisTaskStatus.EXECUTING
                            _statusMessage.value = "Action perform kiya jaa raha hai: ${action.description}"
                            speakResponse(reply)

                            val actionResult = phoneDispatcher.executeAction(action)
                            val finalFeedback = if (actionResult.success) {
                                _taskStatus.value = JarvisTaskStatus.SUCCESS
                                "${actionResult.feedbackMessage} (Success)"
                            } else {
                                _taskStatus.value = JarvisTaskStatus.FAILED
                                "${actionResult.feedbackMessage} (Failed)"
                            }
                            _statusMessage.value = finalFeedback

                            chatDao.insertMessage(
                                ChatMessage(
                                    role = "model",
                                    content = reply,
                                    actionType = action.type.name,
                                    actionPayload = "${action.target} | ${action.extra}",
                                    actionStatus = if (actionResult.success) "SUCCESS" else "FAILED"
                                )
                            )
                        }
                    } else {
                        // Standard conversational reply
                        _taskStatus.value = JarvisTaskStatus.SUCCESS
                        _statusMessage.value = "Jawaab ready hai."
                        speakResponse(reply)
                        chatDao.insertMessage(ChatMessage(role = "model", content = reply))
                    }
                },
                onFailure = { error ->
                    _taskStatus.value = JarvisTaskStatus.FAILED
                    val errorReply = error.localizedMessage ?: "Gemini API se connect karne mein samasya aayi."
                    _statusMessage.value = errorReply
                    speakResponse(errorReply)
                    chatDao.insertMessage(ChatMessage(role = "model", content = errorReply))
                }
            )
        }
    }

    fun confirmPendingAction() {
        val action = _pendingAction.value ?: return
        _pendingAction.value = null
        _taskStatus.value = JarvisTaskStatus.EXECUTING
        _statusMessage.value = "Executing: ${action.description}"

        viewModelScope.launch {
            val result = phoneDispatcher.executeAction(action)
            if (result.success) {
                _taskStatus.value = JarvisTaskStatus.SUCCESS
                _statusMessage.value = result.feedbackMessage
                speakResponse(result.feedbackMessage)
            } else {
                _taskStatus.value = JarvisTaskStatus.FAILED
                _statusMessage.value = result.feedbackMessage
                speakResponse(result.feedbackMessage)
            }
        }
    }

    fun cancelPendingAction() {
        _pendingAction.value = null
        _taskStatus.value = JarvisTaskStatus.IDLE
        _statusMessage.value = "Action cancel kar diya gaya."
        speakResponse("Theek hai, action cancel kar diya gaya hai.")
    }

    fun stopAll() {
        activeJob?.cancel()
        speechRecognizerManager?.stopListening()
        ttsManager?.stop()
        _pendingAction.value = null
        _taskStatus.value = JarvisTaskStatus.STOPPED
        _statusMessage.value = "Jarvis stopped. Sabhi tasks cancel ho gaye."
        _currentTranscript.value = ""
        _rmsLevel.value = 0f
        phoneDispatcher.vibrate(HapticType.ERROR)
    }

    private fun speakResponse(text: String) {
        if (preferences.ttsEnabled) {
            ttsManager?.speak(
                text = text,
                rate = preferences.speechRate,
                pitch = preferences.speechPitch
            )
        }
    }

    // API Key Management
    fun saveApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) return
        val saved = secureStorage.saveApiKey(trimmed)
        if (saved) {
            _hasApiKey.value = true
            _maskedApiKey.value = secureStorage.maskApiKey(trimmed)
            testApiConnection(trimmed)
        } else {
            _apiConnectionStatus.value = ApiConnectionStatus.Error("Failed to store API Key in Android Keystore.")
        }
    }

    fun deleteApiKey() {
        secureStorage.deleteApiKey()
        _hasApiKey.value = secureStorage.hasApiKey()
        _maskedApiKey.value = ""
        _apiConnectionStatus.value = ApiConnectionStatus.Idle
    }

    fun testApiConnection(keyToTest: String? = null) {
        val key = keyToTest ?: secureStorage.getApiKey()
        if (key.isNullOrBlank()) {
            _apiConnectionStatus.value = ApiConnectionStatus.Error("API Key is empty. Please enter your key.")
            return
        }

        viewModelScope.launch {
            _apiConnectionStatus.value = ApiConnectionStatus.Testing
            val res = geminiClient.testConnection(key)
            res.fold(
                onSuccess = { msg ->
                    _apiConnectionStatus.value = ApiConnectionStatus.Success(msg)
                    phoneDispatcher.vibrate(HapticType.SUCCESS)
                },
                onFailure = { err ->
                    _apiConnectionStatus.value = ApiConnectionStatus.Error(err.localizedMessage ?: "Connection error")
                    phoneDispatcher.vibrate(HapticType.ERROR)
                }
            )
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            chatDao.clearAllMessages()
            _statusMessage.value = "Conversation history saaf kar di gayi."
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerManager?.release()
        ttsManager?.release()
    }
}
