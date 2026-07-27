package com.vano.n8nmobile.engine

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.vano.n8nmobile.model.WorkflowNode
import kotlin.random.Random

class NotificationExecutor(private val context: Context) : NodeExecutor {

    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        ensureChannel()
        val firstItem = input.firstOrNull() ?: emptyMap()
        val title = resolveValue(node.config["title"] ?: "Workflow", firstItem)
        val text = resolveValue(node.config["text"] ?: "", firstItem)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(Random.nextInt(), notification)
        } catch (e: SecurityException) {
            // izin notifikasi belum diizinkan user, dilewatin aja biar gak crash
        }
        return input
    }

    private fun resolveValue(raw: String, item: Map<String, Any?>): String {
        if (raw.startsWith("$")) {
            val key = raw.removePrefix("$")
            return item[key]?.toString() ?: raw
        }
        return raw
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Workflow Notifications", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "n8n_mobile_workflow"
    }
}
