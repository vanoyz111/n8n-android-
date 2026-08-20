package com.vano.n8nmobile.chat

import android.content.Context

object ChatRetentionStore {
    private const val PREFS_NAME = "n8n_mobile_chat_retention"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("enabled", v).apply() }

    fun getDays(context: Context): Int = prefs(context).getInt("days", 30)
    fun setDays(context: Context, v: Int) { prefs(context).edit().putInt("days", v).apply() }
}
