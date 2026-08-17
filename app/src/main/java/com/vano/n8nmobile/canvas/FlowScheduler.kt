package com.vano.n8nmobile.canvas

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object FlowScheduler {
    private const val REQUEST_CODE = 5501

    fun scheduleIfNeeded(context: Context) {
        val flow = FlowStore.load(context)
        val triggerNode = flow.nodes.firstOrNull { it.type == "scheduleTrigger" }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FlowAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (triggerNode == null) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val config = parseConfig(triggerNode.configText)
        val intervalMinutes = (config["intervalMinutes"]?.toLongOrNull() ?: 60L).coerceAtLeast(5L)
        val triggerAt = System.currentTimeMillis() + intervalMinutes * 60_000L

        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

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
