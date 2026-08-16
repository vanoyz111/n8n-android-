package com.vano.n8nmobile.settings

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var aiProvider: String
        get() = prefs.getString(KEY_PROVIDER, "gemini") ?: "gemini"
        set(value) = prefs.edit().putString(KEY_PROVIDER, value).apply()

    var geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GEMINI_KEY, value).apply()

    var geminiModel: String
        get() = prefs.getString(KEY_GEMINI_MODEL, "gemini-flash-latest") ?: "gemini-flash-latest"
        set(value) = prefs.edit().putString(KEY_GEMINI_MODEL, value).apply()

    var customBaseUrl: String
        get() = prefs.getString(KEY_CUSTOM_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_URL, value).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_KEY, value).apply()

    var customModel: String
        get() = prefs.getString(KEY_CUSTOM_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_MODEL, value).apply()

    var systemPrompt: String
        get() = prefs.getString(KEY_SYSTEM_PROMPT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SYSTEM_PROMPT, value).apply()

    var tokenSaverEnabled: Boolean
        get() = prefs.getBoolean(KEY_TOKEN_SAVER, false)
        set(value) = prefs.edit().putBoolean(KEY_TOKEN_SAVER, value).apply()

    var darkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    companion object {
        private const val PREFS_NAME = "n8n_mobile_settings"
        private const val KEY_PROVIDER = "ai_provider"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_CUSTOM_URL = "custom_base_url"
        private const val KEY_CUSTOM_KEY = "custom_api_key"
        private const val KEY_CUSTOM_MODEL = "custom_model"
        private const val KEY_SYSTEM_PROMPT = "system_prompt"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_TOKEN_SAVER = "token_saver_enabled"
    }
}
