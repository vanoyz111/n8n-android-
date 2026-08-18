package com.vano.n8nmobile.canvas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object FlowScheduler {
    private const val REQUEST_CODE = 5501
    private const val PREFS_NAME = "n8n_mobile_flow_scheduler"
    private const val KEY_SCHEDULED_FLOW_ID = "scheduled_flow_id"

    fun onFlowSaved(context: Context, flowId: String, nodes: List<CanvasNode>) {
        val triggerNode = nodes.firstOrNull { it.type == "scheduleTrigger" }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlowAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentScheduledId = prefs.getString(KEY_SCHEDULED_FLOW_ID, null)

        if (triggerNode == null) {
            if (currentScheduledId == flowId) {
                alarmManager.cancel(pendingIntent)
                prefs.edit().remove(KEY_SCHEDULED_FLOW_ID).apply()
            }
            return
        }

        prefs.edit().putString(KEY_SCHEDULED_FLOW_ID, flowId).apply()
        val config = parseConfig(triggerNode.configText)
        val intervalMinutes = (config["intervalMinutes"]?.toLongOrNull() ?: 60L).coerceAtLeast(5L)
        val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    fun scheduleIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val flowId = prefs.getString(KEY_SCHEDULED_FLOW_ID, null) ?: return
        val flowState = FlowStore.loadFlow(context, flowId) ?: return
        onFlowSaved(context, flowId, flowState.nodes)
    }

    fun getScheduledFlowId(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_SCHEDULED_FLOW_ID, null)

    fun parseConfig(text: String): Map<String, String> {
        return text.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || !trimmed.contains("=")) return@mapNotNull null
            val idx = trimmed.indexOf("=")
            val key = trimmed.substring(0, idx).trim()
            val value = trimmed.substring(idx + 1).trim()
            if (key.isEmpty()) null else key to value
        }.toMap()
    }
}
