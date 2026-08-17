package com.vano.n8nmobile.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AppLockSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lockEnabled by remember { mutableStateOf(AppLockStore.isLockEnabled(context)) }
    var biometricEnabled by remember { mutableStateOf(AppLockStore.isBiometricEnabled(context)) }
    var hasPinSet by remember { mutableStateOf(AppLockStore.hasPinSet(context)) }
    var showSetPinDialog by remember { mutableStateOf(false) }

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
            Text("Kunci App", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Kalau aktif, Aiwa minta PIN/sidik jari tiap dibuka. Berguna kalau HP sering dipinjem orang lain " +
                "(soalnya app ini nyimpen API key dan bisa balas chat WhatsApp otomatis).",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { showSetPinDialog = true }) {
            Text(if (hasPinSet) "Ganti PIN" else "Atur PIN")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aktifkan Kunci", modifier = Modifier.weight(1f))
            Switch(
                checked = lockEnabled,
                onCheckedChange = { checked ->
                    if (checked && !hasPinSet) {
                        showSetPinDialog = true
                    } else {
                        lockEnabled = checked
                        AppLockStore.setLockEnabled(context, checked)
                    }
                }
            )
        }

        if (hasPinSet) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pakai Sidik Jari juga", modifier = Modifier.weight(1f))
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = {
                        biometricEnabled = it
                        AppLockStore.setBiometricEnabled(context, it)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("Atur PIN (6 digit)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("PIN Baru") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) confirmPin = it },
                        label = { Text("Ulangi PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newPin.length != 6) {
                        error = "PIN harus 6 digit"
                    } else if (newPin != confirmPin) {
                        error = "PIN gak sama"
                    } else {
                        AppLockStore.setPin(context, newPin)
                        AppLockStore.setLockEnabled(context, true)
                        hasPinSet = true
                        lockEnabled = true
                        showSetPinDialog = false
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) { Text("Batal") }
            }
        )
    }
}
