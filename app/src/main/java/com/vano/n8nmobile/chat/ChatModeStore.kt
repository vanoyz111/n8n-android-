package com.vano.n8nmobile.chat

import android.content.Context

object ChatModeStore {
    private const val PREFS_NAME = "n8n_mobile_chat_mode"
    private const val KEY_MODE = "mode"

    fun getMode(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MODE, "auto") ?: "auto"

    fun setMode(context: Context, mode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_MODE, mode).apply()
    }

    fun labelFor(mode: String): String = when (mode) {
        "online" -> "Online"
        "local_gguf" -> "AI Lokal (GGUF)"
        "local_litert" -> "AI Lokal (LiteRT)"
        else -> "Otomatis"
    }

    val allModes = listOf("auto", "online", "local_gguf", "local_litert")
}
