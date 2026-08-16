package com.vano.n8nmobile.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun AiProvidersScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(AiProfileStore.getProfiles(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var prefillOpenRouter by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<AiProfile?>(null) }

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
            Text("Provider AI Tambahan", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Tambah provider AI custom. Atur Tier-nya: Tier 1 dicoba paling awal, kalau semua provider di " +
                "Tier 1 gagal/limit baru pindah ke Tier 2, lalu Tier 3. Kalau semuanya gagal, otomatis jatuh ke AI Lokal.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("OpenRouter", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Satu API key buat akses 40+ provider (Claude, GPT, Llama, Mistral, dll). " +
                        "Ambil key di openrouter.ai/keys, nama model di openrouter.ai/models.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    prefillOpenRouter = true
                    editingProfile = null
                    showAddDialog = true
                }) { Text("+ Tambah OpenRouter") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Provider Kustom Lainnya", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Button(onClick = {
                prefillOpenRouter = false
                editingProfile = null
                showAddDialog = true
            }) { Text("+ Tambah") }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (profiles.isEmpty()) {
            Text("Belum ada provider tambahan.", style = MaterialTheme.typography.bodySmall)
        } else {
            listOf(1, 2, 3).forEach { tierNum ->
                val tierProfiles = profiles.filter { it.tier == tierNum }
                if (tierProfiles.isNotEmpty()) {
                    Text("Tier $tierNum", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                    tierProfiles.forEach { profile ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            editingProfile = profile
                                            prefillOpenRouter = false
                                            showAddDialog = true
                                        }
                                ) {
                                    Text(profile.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${profile.model.ifBlank { "(model belum diisi)" }} · ${profile.apiKeys.size} key",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = {
                                    val updated = profiles.filterNot { it.id == profile.id }
                                    profiles = updated
                                    AiProfileStore.setProfiles(context, updated)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddDialog) {
        val existing = editingProfile
        var name by remember { mutableStateOf(existing?.name ?: if (prefillOpenRouter) "OpenRouter" else "") }
        var baseUrl by remember { mutableStateOf(existing?.baseUrl ?: if (prefillOpenRouter) "https://openrouter.ai/api" else "") }
        var apiKeysText by remember { mutableStateOf(existing?.apiKeys?.joinToString("\n") ?: "") }
        var model by remember { mutableStateOf(existing?.model ?: "") }
        var tier by remember { mutableStateOf(existing?.tier ?: 1) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (existing != null) "Edit Provider" else "Provider Baru") },
            text = {
                Column {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = baseUrl, onValueChange = { baseUrl = it }, label = { Text("Base URL") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeysText,
                        onValueChange = { apiKeysText = it },
                        label = { Text("API Key (satu per baris buat rotasi)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text(if (prefillOpenRouter) "Model (contoh: anthropic/claude-3.5-sonnet)" else "Model") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Tier", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        listOf(1, 2, 3).forEach { t ->
                            Button(
                                onClick = { tier = t },
                                colors = if (tier == t) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors(),
                                modifier = Modifier.padding(end = 8.dp)
                            ) { Text("Tier $t") }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank() && baseUrl.isNotBlank()) {
                        val keys = apiKeysText.lines().map { it.trim() }.filter { it.isNotBlank() }
                        val newProfile = AiProfile(
                            id = existing?.id ?: AiProfileStore.newId(),
                            name = name.trim(),
                            baseUrl = baseUrl.trim(),
                            apiKeys = keys,
                            model = model.trim(),
                            tier = tier
                        )
                        val updated = if (existing != null) {
                            profiles.map { if (it.id == existing.id) newProfile else it }
                        } else {
                            profiles + newProfile
                        }
                        profiles = updated
                        AiProfileStore.setProfiles(context, updated)
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
