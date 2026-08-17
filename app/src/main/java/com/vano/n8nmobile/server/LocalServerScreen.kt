package com.vano.n8nmobile.server

import android.content.Intent
import android.os.Build
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import java.net.Inet4Address
import java.net.NetworkInterface

@Composable
fun LocalServerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var isRunning by remember { mutableStateOf(LocalServerStore.isRunning(context)) }
    var portText by remember { mutableStateOf(LocalServerStore.getPort(context).toString()) }
    var apiKey by remember { mutableStateOf(LocalServerStore.getApiKey(context)) }
    var localIp by remember { mutableStateOf(getLocalIpAddress()) }

    val fullUrl = "http://${localIp ?: "?.?.?.?"}:${portText.toIntOrNull() ?: 8080}/v1"

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
            Text("Server AI Lokal", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Jadiin HP kamu server AI yang bisa diakses laptop/HP lain di WiFi yang sama, format kompatibel OpenAI. " +
                "Semua request lewat sini otomatis ikut sistem 3-tier fallback yang udah kamu atur.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "⚠️ Siapapun yang tau URL & key di bawah bisa pakai kuota AI kamu. Wajib isi API Key kalau dipakai di WiFi bareng orang lain (kampus, kantor, dll).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Aktifkan Server", modifier = Modifier.weight(1f))
            Switch(checked = isRunning, onCheckedChange = { checked ->
                isRunning = checked
                if (checked) {
                    LocalServerStore.setPort(context, portText.toIntOrNull() ?: 8080)
                    LocalServerStore.setApiKey(context, apiKey)
                    localIp = getLocalIpAddress()
                    val intent = Intent(context, LocalServerService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } else {
                    context.stopService(Intent(context, LocalServerService::class.java))
                }
            })
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = portText,
            onValueChange = { portText = it },
            label = { Text("Port") },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text("API Key (kosongin = tanpa proteksi, gak disaranin)") },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth()
        )

        if (isRunning) {
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Server Aktif", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(fullUrl, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { clipboardManager.setText(AnnotatedString(fullUrl)) }) {
                Text("Salin URL")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("Contoh pemakaian (curl):", style = MaterialTheme.typography.bodySmall)
            val curlExample = "curl $fullUrl/chat/completions \\\n  -H \"Authorization: Bearer ${apiKey.ifBlank { "TANPA_KEY" }}\" \\\n  -d '{\"messages\":[{\"role\":\"user\",\"content\":\"halo\"}]}'"
            Text(curlExample, style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Catatan: kalau IP di atas gak muncul/salah, pastiin HP kamu nyambung WiFi (bukan data seluler).",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun getLocalIpAddress(): String? {
    return try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        while (interfaces.hasMoreElements()) {
            val iface = interfaces.nextElement()
            if (iface.isLoopback || !iface.isUp) continue
            val addresses = iface.inetAddresses
            while (addresses.hasMoreElements()) {
                val addr = addresses.nextElement()
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}
