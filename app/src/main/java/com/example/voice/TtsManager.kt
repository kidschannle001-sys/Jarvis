package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class TtsManager(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            val ttsInstance = tts ?: return

            // Prefer Hindi or English (India)
            val hindi = Locale("hi", "IN")
            val langResult = ttsInstance.isLanguageAvailable(hindi)
            if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                ttsInstance.language = hindi
            } else {
                ttsInstance.language = Locale.getDefault()
            }

            ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeakingStateChanged(false)
                }
            })
        }
    }

    fun isEngineAvailable(): Boolean {
        return isInitialized
    }

    fun speak(text: String, rate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized) return
        val ttsInstance = tts ?: return

        // Clean out markdown markers or json artifacts from spoken text
        val cleaned = text
            .replace(Regex("""[*_#`\[\]]"""), "")
            .replace(Regex("""https?://\S+"""), "link")
            .trim()

        if (cleaned.isBlank()) return

        ttsInstance.setSpeechRate(rate)
        ttsInstance.setPitch(pitch)

        // Auto-switch language if predominantly Devanagari Hindi
        val hasHindiScript = cleaned.any { it in '\u0900'..'\u097F' }
        if (hasHindiScript) {
            val hindi = Locale("hi", "IN")
            if (ttsInstance.isLanguageAvailable(hindi) >= TextToSpeech.LANG_AVAILABLE) {
                ttsInstance.language = hindi
            }
        } else {
            ttsInstance.language = Locale.ENGLISH
        }

        val utteranceId = "jarvis_reply_${System.currentTimeMillis()}"
        ttsInstance.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {
            // Ignore
        }
        onSpeakingStateChanged(false)
    }

    fun release() {
        stop()
        try {
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
            // Ignore
        }
    }
}
