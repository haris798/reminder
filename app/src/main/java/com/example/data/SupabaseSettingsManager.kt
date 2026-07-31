package com.example.data

import android.content.Context
import android.content.SharedPreferences
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
        get() = supabaseUrl.isNotBlank() && apiKey.isNotBlank()
}

class SupabaseSettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("supabase_preferences", Context.MODE_PRIVATE)

    private val _configState = MutableStateFlow(loadConfig())
    val configState: StateFlow<SupabaseConfig> = _configState.asStateFlow()

    fun loadConfig(): SupabaseConfig {
        return SupabaseConfig(
            supabaseUrl = prefs.getString(KEY_URL, "https://pcoyvfhcniscynjkndlw.supabase.co") ?: "",
            userEmail = prefs.getString(KEY_EMAIL, "haris443@gmail.com") ?: "",
            userPassword = prefs.getString(KEY_PASSWORD, "") ?: "",
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
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
