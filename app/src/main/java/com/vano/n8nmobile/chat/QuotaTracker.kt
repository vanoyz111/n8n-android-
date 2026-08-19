package com.vano.n8nmobile.chat

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QuotaTracker {
    private const val PREFS_NAME = "n8n_mobile_quota"

    private fun todayKey(): String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun recordCall(context: Context, providerKey: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${providerKey}_${todayKey()}"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun getTodayCount(context: Context, providerKey: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("${providerKey}_${todayKey()}", 0)
    }
}
