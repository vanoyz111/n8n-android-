package com.vano.n8nmobile.backup

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoBackupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = AutoBackupManager.runBackupNow(context)
                AppLog.add(
                    if (file != null) "AUTO_BACKUP" else "AUTO_BACKUP_ERROR",
                    if (file != null) "Backup otomatis berhasil: ${file.name}" else "Backup otomatis gagal"
                )
            } finally {
                AutoBackupScheduler.scheduleNext(context)
                pendingResult.finish()
            }
        }
    }
}
