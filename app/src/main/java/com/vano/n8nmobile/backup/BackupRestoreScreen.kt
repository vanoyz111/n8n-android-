package com.vano.n8nmobile.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.chat.ChatRetentionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showRestartDialog by remember { mutableStateOf(false) }

    var autoBackupEnabled by remember { mutableStateOf(AutoBackupStore.isEnabled(context)) }
    var intervalDaysText by remember { mutableStateOf(AutoBackupStore.getIntervalDays(context).toString()) }
    var autoBackupListVersion by remember { mutableStateOf(0) }
    val autoBackups = remember(autoBackupListVersion) { AutoBackupManager.listBackups(context) }

    var retentionEnabled by remember { mutableStateOf(ChatRetentionStore.isEnabled(context)) }
    var retentionDaysText by remember { mutableStateOf(ChatRetentionStore.getDays(context).toString()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) { BackupManager.exportAll(context) }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    }
                    statusMessage = "Backup berhasil disimpan"
                } catch (e: Exception) {
                    statusMessage = "Gagal backup: ${e.message}"
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val json = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (json.isNullOrBlank()) {
                        statusMessage = "File kosong atau gak bisa dibaca"
                        return@launch
                    }
                    val ok = withContext(Dispatchers.IO) { BackupManager.importAll(context, json) }
                    if (ok) showRestartDialog = true else statusMessage = "Gagal restore: format file gak sesuai"
                } catch (e: Exception) {
                    statusMessage = "Gagal restore: ${e.message}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Nyimpen semua pengaturan Aiwa (chat, flow, provider AI, tema, auto-reply, dll) ke satu file. " +
                "File model AI Lokal (GGUF/LiteRT) yang gede TIDAK ikut ke-backup — perlu didownload ulang manual setelah restore.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Backup Manual", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            exportLauncher.launch("aiwa_backup_$timestamp.json")
        }) { Text("Simpan Backup ke File") }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Restore", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("⚠️ Restore bakal timpa semua pengaturan yang ada sekarang.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("Pilih File Backup") }

        statusMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Jadwal Auto Backup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Backup otomatis tersimpan di penyimpanan app sendiri, gak perlu pilih lokasi tiap kali.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Aktifkan", modifier = Modifier.weight(1f))
            Switch(checked = autoBackupEnabled, onCheckedChange = {
                autoBackupEnabled = it
                AutoBackupStore.setEnabled(context, it)
                AutoBackupScheduler.scheduleNext(context)
            })
        }
        if (autoBackupEnabled) {
            OutlinedTextField(
                value = intervalDaysText,
                onValueChange = { v ->
                    if (v.length <= 3 && v.all { it.isDigit() }) {
                        intervalDaysText = v
                        v.toIntOrNull()?.let {
                            AutoBackupStore.setIntervalDays(context, it.coerceAtLeast(1))
                            AutoBackupScheduler.scheduleNext(context)
                        }
                    }
                },
                label = { Text("Setiap berapa hari") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            scope.launch {
                val file = withContext(Dispatchers.IO) { AutoBackupManager.runBackupNow(context) }
                statusMessage = if (file != null) "Backup otomatis berhasil dibuat" else "Gagal bikin backup"
                autoBackupListVersion++
            }
        }) { Text("Backup Sekarang") }

        Spacer(modifier = Modifier.height(12.dp))
        if (autoBackups.isEmpty()) {
            Text("Belum ada backup otomatis.", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Backup Tersimpan (maks 5 terbaru)", style = MaterialTheme.typography.bodySmall)
            autoBackups.forEach { file ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(file.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { AutoBackupManager.restoreFromFile(context, file) }
                                if (ok) showRestartDialog = true else statusMessage = "Gagal restore dari file ini"
                            }
                        }) { Text("Restore") }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Pengelolaan Data", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text("Hapus otomatis percakapan lama yang gak di-pin, biar riwayat gak numpuk.", style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Aktifkan Auto-Hapus", modifier = Modifier.weight(1f))
            Switch(checked = retentionEnabled, onCheckedChange = {
                retentionEnabled = it
                ChatRetentionStore.setEnabled(context, it)
            })
        }
        if (retentionEnabled) {
            OutlinedTextField(
                value = retentionDaysText,
                onValueChange = { v ->
                    if (v.length <= 3 && v.all { it.isDigit() }) {
                        retentionDaysText = v
                        v.toIntOrNull()?.let { ChatRetentionStore.setDays(context, it.coerceAtLeast(1)) }
                    }
                },
                label = { Text("Hapus setelah (hari)") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showRestartDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Restore Berhasil") },
            text = { Text("App perlu dibuka ulang biar semua perubahan kepake.") },
            confirmButton = {
                TextButton(onClick = {
                    (context as? Activity)?.let {
                        it.finishAffinity()
                        Runtime.getRuntime().exit(0)
                    }
                }) { Text("Tutup App Sekarang") }
            }
        )
    }
}
