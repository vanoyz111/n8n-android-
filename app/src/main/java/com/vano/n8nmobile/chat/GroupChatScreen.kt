package com.vano.n8nmobile.chat

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.settings.AiProfileStore
import kotlinx.coroutines.launch

private data class TranscriptEntry(val speaker: String, val text: String)

@Composable
fun GroupChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var personas by remember { mutableStateOf(PersonaStore.getPersonas(context)) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingPersona by remember { mutableStateOf<Persona?>(null) }

    var input by remember { mutableStateOf("") }
    val transcript = remember { mutableStateListOf<TranscriptEntry>() }
    var isRunning by remember { mutableStateOf(false) }

    var deliberationMode by remember { mutableStateOf(true) }
    var roundsText by remember { mutableStateOf("2") }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Grup AI", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Bikin beberapa persona AI (karakter beda-beda, bisa pakai AI Lokal atau provider kustom kayak OpenRouter), " +
                "pilih yang aktif, terus mulai diskusi. Sesi ini gak tersimpan otomatis, ilang kalau keluar layar.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(12.dp))
        Row {
            Button(onClick = { editingPersona = null; showAddDialog = true }) {
                Text("+ Persona")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (personas.isEmpty()) {
            Text("Belum ada persona.", style = MaterialTheme.typography.bodySmall)
        } else {
            personas.forEach { persona ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = selectedIds.contains(persona.id),
                        onCheckedChange = { checked ->
                            if (checked) selectedIds.add(persona.id) else selectedIds.remove(persona.id)
                        }
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(persona.name, style = MaterialTheme.typography.bodyMedium)
                        Text(ChatModeStore.labelFor(context, persona.mode), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { editingPersona = persona; showAddDialog = true }) {
                        Text("✎")
                    }
                    IconButton(onClick = {
                        val updated = personas.filterNot { it.id == persona.id }
                        personas = updated
                        PersonaStore.setPersonas(context, updated)
                        selectedIds.remove(persona.id)
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mode Musyawarah", style = MaterialTheme.typography.bodyMedium)
                Text("Tiap persona wajib nanggepin pendapat yang lain, diakhiri 1 kesimpulan final", style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = deliberationMode, onCheckedChange = { deliberationMode = it })
        }
        if (deliberationMode) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Jumlah ronde diskusi: ", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = roundsText,
                    onValueChange = { v -> if (v.length <= 1 && v.all { it.isDigit() }) roundsText = v },
                    modifier = Modifier.width(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(transcript) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(entry.speaker, style = MaterialTheme.typography.labelLarge)
                        Text(entry.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (isRunning) {
                item {
                    Row(modifier = Modifier.padding(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        Row(modifier = Modifier.padding(top = 8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Mulai topik diskusi...") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val text = input.trim()
                    val activePersonas = personas.filter { selectedIds.contains(it.id) }
                    if (text.isBlank() || activePersonas.isEmpty() || isRunning) return@Button
                    transcript.add(TranscriptEntry("Kamu", text))
                    input = ""
                    isRunning = true
                    scope.launch {
                        if (deliberationMode && activePersonas.size > 1) {
                            val rounds = (roundsText.toIntOrNull() ?: 2).coerceIn(1, 5)
                            repeat(rounds) { roundIndex ->
                                val isLastRound = roundIndex == rounds - 1
                                for (persona in activePersonas) {
                                    val historyText = transcript.joinToString("\n") { "${it.speaker}: ${it.text}" }
                                    val prompt = buildString {
                                        if (persona.systemPrompt.isNotBlank()) append("${persona.systemPrompt}\n\n")
                                        append("Ini forum musyawarah beberapa AI buat cari jawaban paling tepat bareng-bareng, bukan cuma ngasih pendapat sendiri-sendiri.\n")
                                        append("Topik: $text\n\n")
                                        append("Diskusi sejauh ini:\n$historyText\n\n")
                                        if (isLastRound) {
                                            append("Ini ronde terakhir. Balas sebagai ${persona.name}: mulai arahkan ke kesepakatan bareng peserta lain, ")
                                            append("sebutkan bagian mana yang kamu setuju dan kenapa. Singkat, maksimal 3 kalimat.")
                                        } else {
                                            append("Balas sebagai ${persona.name}: tanggapi langsung pendapat peserta lain yang barusan ngomong ")
                                            append("(setuju/gak setuju dan kenapa, atau sempurnain jawabannya). Jangan cuma ulang pendapat sendiri. Singkat, maksimal 3 kalimat.")
                                        }
                                    }
                                    val reply = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", prompt)), persona.mode)
                                    transcript.add(TranscriptEntry(persona.name, reply))
                                }
                            }

                            val fullTranscript = transcript.joinToString("\n") { "${it.speaker}: ${it.text}" }
                            val synthesisPrompt = buildString {
                                append("Berikut hasil musyawarah beberapa AI soal topik: \"$text\"\n\n")
                                append(fullTranscript)
                                append("\n\nRangkum jadi SATU jawaban final yang paling tepat, hasil sintesis dari diskusi di atas. ")
                                append("Sebutkan poin yang disepakati bersama. Jawab jelas dan terstruktur.")
                            }
                            val consensus = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", synthesisPrompt)), "auto")
                            transcript.add(TranscriptEntry("📋 Kesimpulan Musyawarah", consensus))
                        } else {
                            for (persona in activePersonas) {
                                val historyText = transcript.joinToString("\n") { "${it.speaker}: ${it.text}" }
                                val prompt = buildString {
                                    if (persona.systemPrompt.isNotBlank()) append("${persona.systemPrompt}\n\n")
                                    append("Berikut percakapan grup sejauh ini:\n$historyText\n\n")
                                    append("Balas sebagai ${persona.name}, singkat (maksimal 3 kalimat) dan sesuai karaktermu.")
                                }
                                val reply = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", prompt)), persona.mode)
                                transcript.add(TranscriptEntry(persona.name, reply))
                            }
                        }
                        isRunning = false
                    }
                },
                enabled = !isRunning
            ) { Text("Kirim") }
        }
    }

    if (showAddDialog) {
        val existing = editingPersona
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var systemPrompt by remember { mutableStateOf(existing?.systemPrompt ?: "") }
        var mode by remember { mutableStateOf(existing?.mode ?: "auto") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (existing != null) "Edit Persona" else "Persona Baru") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = systemPrompt,
                        onValueChange = { systemPrompt = it },
                        label = { Text("Karakter/instruksi") },
                        placeholder = { Text("Contoh: Kamu kritis dan suka nanya balik") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Mode AI", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    val builtInOptions = listOf(
                        "auto" to "Otomatis",
                        "online" to "Online",
                        "local_gguf" to "AI Lokal GGUF",
                        "local_litert" to "AI Lokal LiteRT"
                    )
                    val profileOptions = AiProfileStore.getProfiles(context).map { "profile:${it.id}" to it.name }
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        (builtInOptions + profileOptions).forEach { (value, label) ->
                            TextButton(onClick = { mode = value }) {
                                Text(if (mode == value) "[$label]" else label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val newPersona = Persona(existing?.id ?: PersonaStore.newId(), name.trim(), systemPrompt.trim(), mode)
                        val updated = if (existing != null) personas.map { if (it.id == existing.id) newPersona else it }
                        else personas + newPersona
                        personas = updated
                        PersonaStore.setPersonas(context, updated)
                    }
                    showAddDialog = false
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Batal") }
            }
        )
    }
}
