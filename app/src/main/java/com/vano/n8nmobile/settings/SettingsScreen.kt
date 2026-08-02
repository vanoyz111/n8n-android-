package com.vano.n8nmobile.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
fun SettingsScreen(onOpenDrawer: () -> Unit, onThemeChanged: (Boolean) -> Unit, onOpenAutoReply: () -> Unit, onOpenLocalAi: () -> Unit, onOpenThemeCustomization: () -> Unit, onOpenImageGen: () -> Unit) {
    val context = LocalContext.current
    val store = remember { SettingsStore(context) }

    var provider by remember { mutableStateOf(store.aiProvider) }
    var geminiKey by remember { mutableStateOf(store.geminiApiKey) }
    var geminiModel by remember { mutableStateOf(store.geminiModel) }
    var customUrl by remember { mutableStateOf(store.customBaseUrl) }
    var customKey by remember { mutableStateOf(store.customApiKey) }
    var customModel by remember { mutableStateOf(store.customModel) }
    var systemPrompt by remember { mutableStateOf(store.systemPrompt) }
    var darkTheme by remember { mutableStateOf(store.darkTheme) }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            Text("Settings", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Perizinan", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Notifikasi dibutuhkan buat node Notification. Kalau gak muncul, buka pengaturan app manual.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = {
            val intent = Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) {
            Text("Buka Pengaturan Izin App")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Provider AI", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        Box {
            Button(onClick = { providerMenuExpanded = true }) {
                Text(if (provider == "gemini") "Gemini API" else "OpenAI-Compatible (URL kustom)")
            }
            DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }) {
                DropdownMenuItem(text = { Text("Gemini API") }, onClick = {
                    provider = "gemini"
                    providerMenuExpanded = false
                })
                DropdownMenuItem(text = { Text("OpenAI-Compatible (URL kustom)") }, onClick = {
                    provider = "openai_compatible"
                    providerMenuExpanded = false
                })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (provider == "gemini") {
            OutlinedTextField(
                value = geminiKey,
                onValueChange = { geminiKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = geminiModel,
                onValueChange = { geminiModel = it },
                label = { Text("Model (default: gemini-flash-latest)") },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedTextField(
                value = customUrl,
                onValueChange = { customUrl = it },
                label = { Text("Base URL (contoh: http://127.0.0.1:8080)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customKey,
                onValueChange = { customKey = it },
                label = { Text("API Key (kosongin kalau gak butuh)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = customModel,
                onValueChange = { customModel = it },
                label = { Text("Nama model") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { systemPrompt = it },
            label = { Text("System Prompt (instruksi dasar buat AI)") },
            placeholder = { Text("Contoh: Selalu jawab pakai Bahasa Indonesia yang santai.") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = {
            store.aiProvider = provider
            store.geminiApiKey = geminiKey
            store.geminiModel = geminiModel
            store.customBaseUrl = customUrl
            store.customApiKey = customKey
            store.customModel = customModel
            store.systemPrompt = systemPrompt
            savedMessage = "Pengaturan AI disimpan"
            AppLog.add("SETTINGS", "Provider AI disimpan: $provider")
        }) {
            Text("Simpan Pengaturan AI")
        }
        savedMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Tampilan", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
            Text("Mode Gelap")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = darkTheme, onCheckedChange = {
                darkTheme = it
                store.darkTheme = it
                onThemeChanged(it)
            })
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onOpenThemeCustomization) {
            Text("Kustomisasi Warna Tampilan")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("AI Lokal (Offline)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Download & jalanin model AI langsung di HP. Dipakai otomatis kalau AI online gagal.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenLocalAi) {
            Text("Buka Pengaturan AI Lokal")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenImageGen) {
            Text("Buka Image Generator (Offline)")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Chat Bot Otomatis (WhatsApp)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Balas pesan WhatsApp otomatis pakai keyword atau AI, mirip WhatAuto.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onOpenAutoReply) {
            Text("Buka Pengaturan Auto-Reply")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Log & Bantuan", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val logText = AppLog.exportText()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, logText.ifBlank { "Belum ada log." })
                putExtra(Intent.EXTRA_SUBJECT, "Aiwa - Log")
            }
            context.startActivity(Intent.createChooser(intent, "Kirim Log"))
        }) {
            Text("Kirim Log (buat lapor bug)")
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
