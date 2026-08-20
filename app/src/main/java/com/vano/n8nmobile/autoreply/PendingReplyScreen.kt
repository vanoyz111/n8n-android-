package com.vano.n8nmobile.autoreply

import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.logging.AppLog

@Composable
fun PendingReplyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    val items = remember(refreshTick) { PendingReplyStore.getAll() }

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
            Text("Pratinjau Balasan (${items.size})", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Balasan di bawah BELUM terkirim ke WhatsApp. Edit kalau perlu, lalu Kirim atau Tolak. " +
                "Catatan: daftar ini cuma tersimpan di memori — kalau Aiwa ditutup paksa, item yang belum di-review bisa hilang.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { refreshTick++ }) { Text("Muat Ulang") }

        Spacer(modifier = Modifier.height(16.dp))
        if (items.isEmpty()) {
            Text("Gak ada balasan yang nunggu review.", style = MaterialTheme.typography.bodySmall)
        } else {
            items.forEach { item ->
                var editedText by remember(item.id) { mutableStateOf(item.proposedReply) }
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Dari: ${item.sender}", style = MaterialTheme.typography.bodyMedium)
                        Text("Pesan masuk: ${item.originalMessage}", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editedText,
                            onValueChange = { editedText = it },
                            label = { Text("Balasan (bisa diedit)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            Button(onClick = {
                                sendPendingReply(context, item.id, editedText)
                                refreshTick++
                            }) { Text("Kirim") }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(onClick = {
                                PendingReplyStore.remove(item.id)
                                AppLog.add("AUTOREPLY", "Balasan buat ${item.sender} ditolak (dari pratinjau)")
                                refreshTick++
                            }) { Text("Tolak") }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun sendPendingReply(context: Context, id: String, text: String) {
    val item = PendingReplyStore.get(id) ?: return
    try {
        val remoteInputs = item.action.remoteInputs
        if (remoteInputs != null) {
            val intent = Intent()
            val bundle = Bundle()
            remoteInputs.forEach { ri -> bundle.putCharSequence(ri.resultKey, text) }
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
            item.action.actionIntent.send(context, 0, intent)
            AppLog.add("AUTOREPLY", "Balasan (setelah review) terkirim ke ${item.sender}: ${text.take(60)}")
        }
    } catch (e: PendingIntent.CanceledException) {
        AppLog.add("AUTOREPLY_ERROR", "Gagal kirim balasan review: ${e.message}")
    } finally {
        PendingReplyStore.remove(id)
    }
}
