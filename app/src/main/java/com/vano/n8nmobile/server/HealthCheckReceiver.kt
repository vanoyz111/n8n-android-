package com.vano.n8nmobile.server

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vano.n8nmobile.autoreply.AutoReplyStore
import com.vano.n8nmobile.autoreply.WhatsAppNotificationListener
import com.vano.n8nmobile.logging.AppLog

class HealthCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (AutoReplyStore.isEnabled(context) && !WhatsAppNotificationListener.isConnected) {
                AppLog.add("HEALTH_CHECK", "Auto-Reply aktif tapi listener terputus")
                HealthCheckNotifier.notify(
                    context,
                    "Auto-Reply WhatsApp berhenti",
                    "Listener notifikasi terputus. Buka Aiwa buat sambungkan ulang."
                )
            }
            if (LocalServerStore.isRunning(context) && !isServiceRunning(context, LocalServerService::class.java)) {
                AppLog.add("HEALTH_CHECK", "Server Lokal ditandai aktif tapi service gak jalan")
                LocalServerStore.setRunning(context, false)
                HealthCheckNotifier.notify(
                    context,
                    "Server AI Lokal berhenti",
                    "Kemungkinan dimatikan sistem. Buka Aiwa buat nyalain ulang."
                )
            }
        } finally {
            HealthCheckScheduler.scheduleNext(context)
        }
    }
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    @Suppress("DEPRECATION")
    return manager.getRunningServices(Integer.MAX_VALUE).any { it.service.className == serviceClass.name }
}
