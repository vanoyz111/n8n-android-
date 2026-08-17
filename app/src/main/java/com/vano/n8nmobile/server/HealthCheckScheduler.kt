package com.vano.n8nmobile.server

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object HealthCheckScheduler {
    private const val REQUEST_CODE = 5502
    private const val INTERVAL_MS = 20 * 60_000L

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, HealthCheckReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + INTERVAL_MS
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
