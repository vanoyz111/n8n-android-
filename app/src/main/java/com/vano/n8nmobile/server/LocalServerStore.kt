package com.vano.n8nmobile.server

import android.content.Context

object LocalServerStore {
    private const val PREFS_NAME = "n8n_mobile_local_server"
    private const val KEY_PORT = "port"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_RUNNING = "running"

    fun getPort(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_PORT, 8080)

    fun setPort(context: Context, port: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_PORT, port).apply()
    }

    fun getApiKey(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_API_KEY, key).apply()
    }

    fun isRunning(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_RUNNING, false)

    fun setRunning(context: Context, running: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_RUNNING, running).apply()
    }
}
