package com.vano.n8nmobile.autoreply

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AutoReplyScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var enabled by remember { mutableStateOf(AutoReplyStore.isEnabled(context)) }
    var aiFallback by remember { mutableStateOf(AutoReplyStore.isAiFallbackEnabled(context)) }
    var personaPrompt by remember { mutableStateOf(AutoReplyStore.getPersonaPrompt(context)) }
    var rules by remember { mutableStateOf(AutoReplyStore.getRules(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

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
            Text("Auto-Reply WhatsApp", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Otomatis balas pesan WhatsApp pakai keyword atau AI. Butuh izin akses notifikasi, " +
                "dan cuma jalan selama Aiwa gak di-force-close.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }) {
            Text("Buka Pengaturan Izin Notifikasi")
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text("Cari \"Aiwa\" di daftar itu, lalu aktifkan.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aktifkan Auto-Reply", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = {
                enabled = it
                AutoReplyStore.setEnabled(context, it)
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Fallback ke AI kalau gak ada keyword cocok", modifier = Modifier.weight(1f))
            Switch(checked = aiFallback, onCheckedChange = {
                aiFallback = it
                AutoReplyStore.setAiFallbackEnabled(context, it)
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = personaPrompt,
            onValueChange = { personaPrompt = it },
            label = { Text("Instruksi AI buat balasan (opsional)") },
            placeholder = { Text("Contoh: Balas singkat, ramah, bahasa Indonesia santai.") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            AutoReplyStore.setPersonaPrompt(context, personaPrompt)
            savedMessage = "Instruksi AI disimpan"
        }) {
            Text("Simpan Instruksi AI")
        }
        savedMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aturan Keyword", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) {
                Text("+ Aturan")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rules.isEmpty()) {
            Text("Belum ada aturan keyword.", style = MaterialTheme.typography.bodySmall)
        } else {
            rules.forEach { rule ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Keyword: ${rule.keyword}", style = MaterialTheme.typography.bodyMedium)
                            Text("Balasan: ${rule.reply}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = {
                            val updated = rules.filterNot { it.id == rule.id }
                            rules = updated
                            AutoReplyStore.setRules(context, updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus aturan")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddDialog) {
        var keyword by remember { mutableStateOf("") }
        var reply by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Aturan Baru") },
            text = {
                Column {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Keyword (contoh: harga)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reply,
                        onValueChange = { reply = it },
                        label = { Text("Balasan otomatis") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (keyword.isNotBlank()) {
                        val newRule = AutoReplyRule(AutoReplyStore.newId(), keyword.trim(), reply.trim())
                        val updated = rules + newRule
                        rules = updated
                        AutoReplyStore.setRules(context, updated)
                    }
                    showAddDialog = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }
}
