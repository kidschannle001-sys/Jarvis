package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorage(private val context: Context) {

    private val keyStoreAlias = "jarvis_vault_key"
    private val prefName = "jarvis_secure_prefs"
    private val keyApiKey = "encrypted_gemini_api_key"
    private val keyIv = "encrypted_gemini_iv"

    private val sharedPreferences = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)

    init {
        ensureKeyStoreKey()
    }

    private fun ensureKeyStoreKey() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (!keyStore.containsAlias(keyStoreAlias)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    keyStoreAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Android Keystore initialization fallback
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.getKey(keyStoreAlias, null) as? SecretKey
        } catch (_: Exception) {
            null
        }
    }

    fun saveApiKey(apiKey: String): Boolean {
        if (apiKey.isBlank()) return false
        return try {
            val secretKey = getSecretKey() ?: return false
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))

            sharedPreferences.edit()
                .putString(keyApiKey, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(keyIv, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getApiKey(): String? {
        val encryptedBase64 = sharedPreferences.getString(keyApiKey, null)
        val ivBase64 = sharedPreferences.getString(keyIv, null)

        if (!encryptedBase64.isNullOrEmpty() && !ivBase64.isNullOrEmpty()) {
            try {
                val secretKey = getSecretKey()
                if (secretKey != null) {
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    val iv = Base64.decode(ivBase64, Base64.NO_WRAP)
                    val spec = GCMParameterSpec(128, iv)
                    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                    val encryptedBytes = Base64.decode(encryptedBase64, Base64.NO_WRAP)
                    val decrypted = cipher.doFinal(encryptedBytes)
                    val key = String(decrypted, Charsets.UTF_8)
                    if (key.isNotBlank()) return key
                }
            } catch (_: Exception) {
                // Key decryption error fallback
            }
        }

        // Check if injected via BuildConfig (from AI Studio runtime secrets or .env)
        try {
            val buildConfigKey = BuildConfig.GEMINI_API_KEY
            if (!buildConfigKey.isNullOrBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
                return buildConfigKey
            }
        } catch (_: Throwable) {
            // Ignore if field doesn't exist
        }

        return null
    }

    fun deleteApiKey(): Boolean {
        return sharedPreferences.edit().remove(keyApiKey).remove(keyIv).commit()
    }

    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    fun maskApiKey(key: String): String {
        if (key.length <= 8) return "••••••••"
        val prefix = key.take(6)
        val suffix = key.takeLast(4)
        return "$prefix••••••••$suffix"
    }
}
