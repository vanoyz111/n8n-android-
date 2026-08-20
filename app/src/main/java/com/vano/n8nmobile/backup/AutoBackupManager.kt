package com.vano.n8nmobile.backup

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AutoBackupManager {
    private const val MAX_KEPT = 5

    private fun backupDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "auto_backups")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun runBackupNow(context: Context): File? {
        return try {
            val json = BackupManager.exportAll(context)
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(backupDir(context), "auto_backup_$timestamp.json")
            file.writeText(json)
            AutoBackupStore.setLastBackupAt(context, System.currentTimeMillis())
            cleanup(context)
            file
        } catch (e: Exception) {
            null
        }
    }

    fun listBackups(context: Context): List<File> =
        backupDir(context).listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun restoreFromFile(context: Context, file: File): Boolean {
        return try {
            BackupManager.importAll(context, file.readText())
        } catch (e: Exception) {
            false
        }
    }

    private fun cleanup(context: Context) {
        listBackups(context).drop(MAX_KEPT).forEach { it.delete() }
    }
}
