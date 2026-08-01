package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SupabaseConfig(
    val supabaseUrl: String = "https://pcoyvfhcniscynjkndlw.supabase.co",
    val userEmail: String = "haris443@gmail.com",
    val userPassword: String = "",
    val apiKey: String = "",
    val autoUpload: Boolean = true,
    val uploadIntervalMinutes: Int = 15
) {
    val isConnected: Boolean
        get() = supabaseUrl.isNotBlank() && apiKey.isNotBlank() && apiKey != "YOUR_SUPABASE_API_KEY_HERE"
}

class SupabaseSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_preferences", Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<SupabaseConfig> = _configState.asStateFlow()

    fun loadConfig(): SupabaseConfig {
        val buildConfigUrl = BuildConfig.SUPABASE_URL.ifBlank { "https://pcoyvfhcniscynjkndlw.supabase.co" }
        val buildConfigKey = BuildConfig.SUPABASE_API_KEY

        val savedUrl = prefs.getString(KEY_URL, buildConfigUrl) ?: buildConfigUrl
        val savedEmail = prefs.getString(KEY_EMAIL, "haris443@gmail.com") ?: "haris443@gmail.com"
        val savedPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        val savedApiKey = prefs.getString(KEY_API_KEY, "") ?: ""

        val effectiveUrl = if (savedUrl.isBlank()) buildConfigUrl else savedUrl
        val effectiveApiKey = if (savedApiKey.isBlank()) {
            if (buildConfigKey.isNotBlank() && buildConfigKey != "YOUR_SUPABASE_API_KEY_HERE") buildConfigKey else ""
        } else {
            savedApiKey
        }

        return SupabaseConfig(
            supabaseUrl = effectiveUrl,
            userEmail = savedEmail,
            userPassword = savedPassword,
            apiKey = effectiveApiKey,
            autoUpload = prefs.getBoolean(KEY_AUTO_UPLOAD, true),
            uploadIntervalMinutes = prefs.getInt(KEY_INTERVAL, 15)
        )
    }

    fun saveConfig(config: SupabaseConfig) {
        prefs.edit()
            .putString(KEY_URL, config.supabaseUrl)
            .putString(KEY_EMAIL, config.userEmail)
            .putString(KEY_PASSWORD, config.userPassword)
            .putString(KEY_API_KEY, config.apiKey)
            .putBoolean(KEY_AUTO_UPLOAD, config.autoUpload)
            .putInt(KEY_INTERVAL, config.uploadIntervalMinutes)
            .apply()
        _configState.value = config
    }

    companion object {
        private const val KEY_URL = "supabase_url"
        private const val KEY_EMAIL = "supabase_email"
        private const val KEY_PASSWORD = "supabase_password"
        private const val KEY_API_KEY = "supabase_api_key"
        private const val KEY_AUTO_UPLOAD = "supabase_auto_upload"
        private const val KEY_INTERVAL = "supabase_upload_interval"
    }
}

