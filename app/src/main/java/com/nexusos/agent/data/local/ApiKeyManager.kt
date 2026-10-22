package com.nexusos.agent.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyManager(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "nexus_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_PRIMARY_MODEL = "gemini_primary_model"
        private const val KEY_LITE_MODEL = "gemini_lite_model"
        private const val KEY_RPM_LIMIT = "free_tier_rpm_limit"
        const val DEFAULT_PRIMARY_MODEL = "gemini-2.5-flash"
        const val DEFAULT_LITE_MODEL = "gemini-2.5-flash-lite"
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API, apiKey.trim()).apply()
    }

    fun getApiKey(): String {
        val saved = prefs.getString(KEY_GEMINI_API, "") ?: ""
        if (saved.isNotBlank()) return saved
        return try {
            val buildConfigClass = Class.forName("com.example.BuildConfig")
            val field = buildConfigClass.getField("GEMINI_API_KEY")
            val value = field.get(null) as? String ?: ""
            if (value.isNotBlank() && value != "MY_GEMINI_API_KEY") value else ""
        } catch (_: Throwable) {
            ""
        }
    }

    fun setPrimaryModel(modelName: String) {
        prefs.edit().putString(KEY_PRIMARY_MODEL, modelName.trim()).apply()
    }

    fun getPrimaryModel(): String =
        prefs.getString(KEY_PRIMARY_MODEL, DEFAULT_PRIMARY_MODEL) ?: DEFAULT_PRIMARY_MODEL

    fun setLiteModel(modelName: String) {
        prefs.edit().putString(KEY_LITE_MODEL, modelName.trim()).apply()
    }

    fun getLiteModel(): String =
        prefs.getString(KEY_LITE_MODEL, DEFAULT_LITE_MODEL) ?: DEFAULT_LITE_MODEL

    // Free Tier Default: 10 Requests Per Minute (1 request every 6 seconds)
    fun getRpmLimit(): Int = prefs.getInt(KEY_RPM_LIMIT, 10)

    fun setRpmLimit(rpm: Int) {
        prefs.edit().putInt(KEY_RPM_LIMIT, rpm.coerceIn(1, 60)).apply()
    }
}
