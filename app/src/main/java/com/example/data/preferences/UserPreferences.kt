package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("jarvis_user_prefs", Context.MODE_PRIVATE)

    var preferredLanguage: String
        get() = prefs.getString("pref_language", "hinglish") ?: "hinglish"
        set(value) = prefs.edit().putString("pref_language", value).apply()

    var speechRate: Float
        get() = prefs.getFloat("pref_speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("pref_speech_rate", value).apply()

    var speechPitch: Float
        get() = prefs.getFloat("pref_speech_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("pref_speech_pitch", value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean("pref_tts_enabled", true)
        set(value) = prefs.edit().putBoolean("pref_tts_enabled", value).apply()

    var hapticFeedbackEnabled: Boolean
        get() = prefs.getBoolean("pref_haptics_enabled", true)
        set(value) = prefs.edit().putBoolean("pref_haptics_enabled", value).apply()
}
