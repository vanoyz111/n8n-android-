package com.vano.n8nmobile.autoreply

import android.Manifest
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class FilterModeOption(val value: String, val label: String)

private val filterModeOptions = listOf(
    FilterModeOption("EVERYONE", "Semua orang"),
    FilterModeOption("WHITELIST", "Daftar kontak saya..."),
    FilterModeOption("BLACKLIST", "Kecuali daftar kontak saya..."),
    FilterModeOption("EXCEPT_PHONE_CONTACTS", "Kecuali kontak telepon saya")
)

private data class AiModeOption(val value: String, val label: String)

private val aiModeOptions = listOf(
    AiModeOption("auto", "Otomatis (online, fallback ke lokal kalau gagal)"),
    AiModeOption("online", "Online saja (API Key di Settings)"),
    AiModeOption("local_gguf", "AI Lokal - GGUF (Llamatik)"),
    AiModeOption("local_litert", "AI Lokal - LiteRT-LM")
)

@Composable
fun AutoReplyScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var enabled by remember { mutableStateOf(AutoReplyStore.isEnabled(context)) }
    var aiFallback by remember { mutableStateOf(AutoReplyStore.isAiFallbackEnabled(context)) }
    var personaPrompt by remember { mutableStateOf(AutoReplyStore.getPersonaPrompt(context)) }
    var rules by remember { mutableStateOf(AutoReplyStore.getRules(context)) }
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    var aiMode by remember { mutableStateOf(AutoReplyStore.getAiMode(context)) }

    var filterMode by remember { mutableStateOf(AutoReplyStore.getContactFilterMode(context)) }
    var groupEnabled by remember { mutableStateOf(AutoReplyStore.isGroupEnabled(context)) }
    var contactList by remember { mutableStateOf(AutoReplyStore.getContactList(context)) }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var showDeviceContactsDialog by remember { mutableStateOf(false) }
    var deviceContacts by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingContacts by remember { mutableStateOf(false) }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            isLoadingContacts = true
            scope.launch {
                deviceContacts = queryDeviceContacts(context)
                isLoadingContacts = false
                showDeviceContactsDialog = true
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

        Text("Provider AI buat Auto-Reply", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Ini terpisah dari pengaturan AI di layar Chat.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        aiModeOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = aiMode == option.value,
                        onClick = {
                            aiMode = option.value
                            AutoReplyStore.setAiMode(context, option.value)
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = aiMode == option.value,
                    onClick = {
                        aiMode = option.value
                        AutoReplyStore.setAiMode(context, option.value)
                    }
                )
                Text(option.label)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Balas Otomatis Ke", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        filterModeOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = filterMode == option.value,
                        onClick = {
                            filterMode = option.value
                            AutoReplyStore.setContactFilterMode(context, option.value)
                        }
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = filterMode == option.value,
                    onClick = {
                        filterMode = option.value
                        AutoReplyStore.setContactFilterMode(context, option.value)
                    }
                )
                Text(option.label)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectable(
                    selected = groupEnabled,
                    onClick = {
                        groupEnabled = !groupEnabled
                        AutoReplyStore.setGroupEnabled(context, groupEnabled)
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = groupEnabled, onCheckedChange = {
                groupEnabled = it
                AutoReplyStore.setGroupEnabled(context, it)
            })
            Text("Aktifkan Grup")
        }

        if (filterMode == "WHITELIST" || filterMode == "BLACKLIST") {
            Spacer(modifier = Modifier.height(16.dp))
            Text("Daftar Kontak", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = { showAddContactDialog = true }) {
                    Text("+ Manual")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                    Text(if (isLoadingContacts) "Memuat..." else "Dari Kontak HP")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (contactList.isEmpty()) {
                Text("Belum ada kontak.", style = MaterialTheme.typography.bodySmall)
            } else {
                contactList.forEach { contact ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(contact, modifier = Modifier.weight(1f))
                        IconButton(onClick = {
                            val updated = contactList.filterNot { it == contact }
                            contactList = updated
                            AutoReplyStore.setContactList(context, updated)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus kontak")
                        }
                    }
                }
            }
        }

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
            placeholder = { Text("Contoh: Selalu jawab pakai Bahasa Indonesia yang santai.") },
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
            Button(onClick = { showAddRuleDialog = true }) {
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

    if (showAddRuleDialog) {
        var keyword by remember { mutableStateOf("") }
        var reply by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRuleDialog = false },
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
                    showAddRuleDialog = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddRuleDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showAddContactDialog) {
        var contactName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Tambah Kontak Manual") },
            text = {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Nama persis seperti di WhatsApp") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (contactName.isNotBlank()) {
                        val updated = contactList + contactName.trim()
                        contactList = updated
                        AutoReplyStore.setContactList(context, updated)
                    }
                    showAddContactDialog = false
                }) { Text("Tambah") }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) { Text("Batal") }
            }
        )
    }

    if (showDeviceContactsDialog) {
        var searchQuery by remember { mutableStateOf("") }
        val filtered = deviceContacts.filter { it.contains(searchQuery, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { showDeviceContactsDialog = false },
            title = { Text("Pilih dari Kontak HP") },
            text = {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Cari kontak") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(filtered) { name ->
                            val alreadyAdded = contactList.contains(name)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(selected = alreadyAdded, onClick = {
                                        if (!alreadyAdded) {
                                            val updated = contactList + name
                                            contactList = updated
                                            AutoReplyStore.setContactList(context, updated)
                                        }
                                    })
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = alreadyAdded, onCheckedChange = { checked ->
                                    val updated = if (checked) contactList + name else contactList.filterNot { it == name }
                                    contactList = updated
                                    AutoReplyStore.setContactList(context, updated)
                                })
                                Text(name)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceContactsDialog = false }) { Text("Selesai") }
            }
        )
    }
}

private suspend fun queryDeviceContacts(context: Context): List<String> = withContext(Dispatchers.IO) {
    val names = mutableListOf<String>()
    val cursor = context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        arrayOf(ContactsContract.Contacts.DISPLAY_NAME),
        null,
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME} ASC"
    )
    cursor?.use {
        val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
        while (it.moveToNext()) {
            val name = it.getString(nameIndex)
            if (!name.isNullOrBlank()) names.add(name)
        }
    }
    names.distinct()
}
