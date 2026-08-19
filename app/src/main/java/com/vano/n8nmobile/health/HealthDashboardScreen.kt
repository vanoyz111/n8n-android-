package com.vano.n8nmobile.health

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.vano.n8nmobile.autoreply.AutoReplyStore
import com.vano.n8nmobile.autoreply.WhatsAppNotificationListener
import com.vano.n8nmobile.server.LocalServerStore

@Composable
fun HealthDashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableStateOf(0) }

    val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val batteryUnrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)

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
            Text("Cek Kesehatan Background", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Layanan background (Auto-Reply, Server Lokal, Flow terjadwal) sering dimatikan HyperOS buat hemat baterai. " +
                "Pastiin 3 hal di bawah ini semuanya aktif biar Aiwa gak berhenti sendiri.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(24.dp))

        HealthRow(
            title = "Battery Optimization",
            status = if (batteryUnrestricted) "Aman (tanpa batasan)" else "Belum diizinkan — berisiko dimatikan",
            ok = batteryUnrestricted
        )
        Button(onClick = {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        }) { Text("Buka Pengaturan Baterai") }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        HealthRow(
            title = "Autostart (khusus HyperOS/MIUI)",
            status = "Gak bisa dicek otomatis — pastiin manual diaktifkan",
            ok = null
        )
        Button(onClick = {
            try {
                val intent = Intent().apply {
                    component = ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        }) { Text("Buka Pengaturan Autostart") }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        HealthRow(
            title = "Listener Notifikasi (Auto-Reply)",
            status = when {
                !AutoReplyStore.isEnabled(context) -> "Auto-Reply gak aktif, gak perlu"
                WhatsAppNotificationListener.isConnected -> "Tersambung"
                else -> "Terputus"
            },
            ok = if (!AutoReplyStore.isEnabled(context)) null else WhatsAppNotificationListener.isConnected
        )
        Button(onClick = {
            try {
                NotificationListenerService.requestRebind(
                    ComponentName(context, WhatsAppNotificationListener::class.java)
                )
                refreshTrigger++
            } catch (e: Exception) { }
        }) { Text("Sambungkan Ulang") }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(20.dp))

        HealthRow(
            title = "Server AI Lokal",
            status = if (LocalServerStore.isRunning(context)) "Aktif" else "Tidak aktif",
            ok = if (LocalServerStore.isRunning(context)) true else null
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun HealthRow(title: String, status: String, ok: Boolean?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (ok) { true -> "✅"; false -> "⚠️"; null -> "ℹ️" },
                modifier = Modifier.padding(end = 10.dp)
            )
            Column {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
