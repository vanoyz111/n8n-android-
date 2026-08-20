package com.vano.n8nmobile.chat

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.settings.AiProfileStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class CompareResult(val label: String, val text: String, val elapsedMs: Long)

@Composable
fun CompareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var prompt by remember { mutableStateOf("") }
    val selectedModes = remember { mutableStateListOf<String>() }
    var isRunning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<CompareResult>>(emptyList()) }

    val builtIn = listOf("online" to "Online", "local_gguf" to "AI Lokal GGUF", "local_litert" to "AI Lokal LiteRT")
    val profiles = AiProfileStore.getProfiles(context).map { "profile:${it.id}" to it.name }
    val allOptions = builtIn + profiles

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
            Text("Bandingkan Provider", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Kirim 1 pertanyaan yang sama ke beberapa AI sekaligus, bandingin hasil dan kecepatannya.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Pilih Provider (minimal 2)", style = MaterialTheme.typography.titleMedium)
        allOptions.forEach { (value, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = selectedModes.contains(value),
                    onCheckedChange = { checked ->
                        if (checked) selectedModes.add(value) else selectedModes.remove(value)
                    }
                )
                Text(label)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            label = { Text("Pertanyaan") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                if (prompt.isBlank() || selectedModes.size < 2 || isRunning) return@Button
                isRunning = true
                results = emptyList()
                val modesSnapshot = selectedModes.toList()
                val labelsSnapshot = modesSnapshot.map { mode -> ChatModeStore.labelFor(context, mode) }
                scope.launch {
                    val computed = coroutineScope {
                        modesSnapshot.mapIndexed { index, mode ->
                            async {
                                val start = System.currentTimeMillis()
                                val reply = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", prompt)), mode)
                                val elapsed = System.currentTimeMillis() - start
                                CompareResult(labelsSnapshot[index], reply, elapsed)
                            }
                        }.map { it.await() }
                    }
                    results = computed
                    isRunning = false
                }
            },
            enabled = !isRunning && selectedModes.size >= 2 && prompt.isNotBlank()
        ) { Text(if (isRunning) "Memproses..." else "Bandingkan") }

        if (isRunning) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(16.dp))
        results.forEach { result ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${result.label} — ${result.elapsedMs}ms", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(result.text, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
