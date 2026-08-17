package com.vano.n8nmobile

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vano.n8nmobile.canvas.FlowScheduler
import com.vano.n8nmobile.server.HealthCheckScheduler

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            FlowScheduler.scheduleIfNeeded(context)
            HealthCheckScheduler.scheduleNext(context)
        }
    }
}
