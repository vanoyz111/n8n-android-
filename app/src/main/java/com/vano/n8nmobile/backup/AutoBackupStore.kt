package com.vano.n8nmobile.backup

import android.content.Context

object AutoBackupStore {
    private const val PREFS_NAME = "n8n_mobile_auto_backup"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean("enabled", false)
    fun setEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("enabled", v).apply() }

    fun getIntervalDays(context: Context): Int = prefs(context).getInt("interval_days", 7)
    fun setIntervalDays(context: Context, v: Int) { prefs(context).edit().putInt("interval_days", v).apply() }

    fun getLastBackupAt(context: Context): Long = prefs(context).getLong("last_backup_at", 0L)
    fun setLastBackupAt(context: Context, v: Long) { prefs(context).edit().putLong("last_backup_at", v).apply() }
}
