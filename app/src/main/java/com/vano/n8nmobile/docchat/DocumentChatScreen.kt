package com.vano.n8nmobile.docchat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.chat.ChatModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class DocQaPair(val question: String, val answer: String)

@Composable
fun DocumentChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var listVersion by remember { mutableStateOf(0) }
    val documents = remember(listVersion) { DocumentStore.getDocuments(context) }
    val selectedIds = remember { mutableStateListOf<String>() }

    var showPasteDialog by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf("auto") }
    var question by remember { mutableStateOf("") }
    val transcript = remember { mutableStateListOf<DocQaPair>() }
    var isRunning by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }
                    if (!content.isNullOrBlank()) {
                        val name = queryFileName(context, uri) ?: "dokumen.txt"
                        DocumentStore.addDocument(context, name, content)
                        listVersion++
                    }
                } catch (e: Exception) { }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Chat dengan Dokumen", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Upload file .txt/.md atau tempel teks, terus tanya apa aja soal isinya. " +
                "PDF belum didukung (Android gak punya cara ekstrak teks PDF tanpa library tambahan yang beresiko).",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(onClick = { importLauncher.launch(arrayOf("text/plain", "text/markdown", "application/octet-stream")) }) {
                Text("+ Import File")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showPasteDialog = true }) {
                Text("+ Tempel Teks")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        if (documents.isEmpty()) {
            Text("Belum ada dokumen.", style = MaterialTheme.typography.bodySmall)
        } else {
            documents.forEach { doc ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedIds.contains(doc.id),
                        onCheckedChange = { checked ->
                            if (checked) selectedIds.add(doc.id) else selectedIds.remove(doc.id)
                        }
                    )
                    Text("${doc.name} (${doc.chunkCount} bagian)", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        DocumentStore.removeDocument(context, doc.id)
                        selectedIds.remove(doc.id)
                        listVersion++
                    }) { Icon(Icons.Default.Delete, contentDescription = "Hapus") }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Mode AI", style = MaterialTheme.typography.bodySmall)
        Row {
            listOf("auto" to "Otomatis", "online" to "Online", "local_gguf" to "AI Lokal GGUF", "local_litert" to "AI Lokal LiteRT").forEach { (value, label) ->
                TextButton(onClick = { mode = value }) {
                    Text(if (mode == value) "[$label]" else label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(transcript) { pair ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Kamu: ${pair.question}", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(pair.answer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            if (isRunning) {
                item {
                    Row(modifier = Modifier.padding(8.dp)) { CircularProgressIndicator(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                placeholder = { Text("Tanya soal dokumen...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val q = question.trim()
                    val activeDocs = documents.filter { selectedIds.contains(it.id) }
                    if (q.isBlank() || activeDocs.isEmpty() || isRunning) return@Button
                    question = ""
                    isRunning = true
                    scope.launch {
                        val allChunks = activeDocs.flatMap { DocumentChunker.chunk(DocumentStore.getContent(it)) }
                        val relevant = DocumentChunker.retrieveRelevantChunks(allChunks, q)
                        val prompt = buildString {
                            append("Berikut potongan isi dokumen:\n\n")
                            relevant.forEachIndexed { i, chunk -> append("[Bagian ${i + 1}]\n$chunk\n\n") }
                            append("Pertanyaan: $q\n\n")
                            append("Jawab HANYA berdasarkan isi dokumen di atas. Kalau jawabannya gak ada di situ, bilang jawabannya gak ditemukan di dokumen.")
                        }
                        val answer = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", prompt)), mode)
                        transcript.add(DocQaPair(q, answer))
                        isRunning = false
                    }
                },
                enabled = !isRunning
            ) { Text("Tanya") }
        }
    }

    if (showPasteDialog) {
        var pasteName by remember { mutableStateOf("") }
        var pasteContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = { Text("Tempel Teks") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = pasteName, onValueChange = { pasteName = it }, label = { Text("Nama dokumen") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = pasteContent, onValueChange = { pasteContent = it }, label = { Text("Isi teks") }, modifier = Modifier.fillMaxWidth(), minLines = 6)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pasteName.isNotBlank() && pasteContent.isNotBlank()) {
                        DocumentStore.addDocument(context, pasteName.trim(), pasteContent)
                        listVersion++
                    }
                    showPasteDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showPasteDialog = false }) { Text("Batal") } }
        )
    }
}

private fun queryFileName(context: android.content.Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    }
    return name
}
