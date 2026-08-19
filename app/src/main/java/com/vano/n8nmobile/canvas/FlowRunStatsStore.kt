package com.vano.n8nmobile.canvas

import android.content.Context

object FlowRunStatsStore {
    private const val PREFS_NAME = "n8n_mobile_flow_stats"

    fun recordRun(context: Context, flowId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "runs_$flowId"
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    fun getRunCount(context: Context, flowId: String): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("runs_$flowId", 0)
    }
}
