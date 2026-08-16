package com.vano.n8nmobile.chat

import android.content.Context
import com.vano.n8nmobile.settings.AiProfileStore

object ChatModeStore {
    private const val PREFS_NAME = "n8n_mobile_chat_mode"
    private const val KEY_MODE = "mode"
    private const val KEY_THINKING = "thinking_enabled"

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MODE, "auto") ?: "auto"

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply()
    }

    fun isThinkingEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_THINKING, false)

    fun setThinkingEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_THINKING, enabled).apply()
    }

    fun labelFor(context: Context, mode: String): String {
        if (mode.startsWith("profile:")) {
            val id = mode.removePrefix("profile:")
            return AiProfileStore.getProfile(context, id)?.name ?: "Provider Terhapus"
        }
        return when (mode) {
            "online" -> "Online (Gemini)"
            "local_gguf" -> "AI Lokal (GGUF)"
            "local_litert" -> "AI Lokal (LiteRT)"
            else -> "Otomatis"
        }
    }

    val builtInModes = listOf("auto", "online", "local_gguf", "local_litert")
}
