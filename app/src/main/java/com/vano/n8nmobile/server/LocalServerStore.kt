package com.vano.n8nmobile.server

import android.content.Context

object LocalServerStore {
    private const val PREFS_NAME = "n8n_mobile_local_server"
    private const val KEY_PORT = "port"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_RUNNING = "running"

    fun getPort(context: Context): Int =
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).getInt(KEY_PORT, 8080)

    fun setPort(context: Context, port: Int) {
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).edit().putInt(KEY_PORT, port).apply()
    }

    fun getApiKey(context: Context): String =
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).edit().putString(KEY_API_KEY, key).apply()
    }

    fun isRunning(context: Context): Boolean =
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) {
        com.vano.n8nmobile.security.SecurePrefs.get(context, PREFS_NAME).edit().putBoolean(KEY_RUNNING, running).apply()
    }
}
