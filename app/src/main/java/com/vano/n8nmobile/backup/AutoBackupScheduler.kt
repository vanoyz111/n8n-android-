package com.vano.n8nmobile.backup

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AutoBackupScheduler {
    private const val REQUEST_CODE = 5503

    fun scheduleNext(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AutoBackupReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (!AutoBackupStore.isEnabled(context)) {
            alarmManager.cancel(pendingIntent)
            return
        }
        val intervalDays = AutoBackupStore.getIntervalDays(context).coerceAtLeast(1)
        val triggerAt = System.currentTimeMillis() + intervalDays * 24 * 60 * 60_000L
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }
}
