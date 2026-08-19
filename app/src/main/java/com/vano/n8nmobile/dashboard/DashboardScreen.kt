package com.vano.n8nmobile.dashboard

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
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.canvas.FlowRunStatsStore
import com.vano.n8nmobile.canvas.FlowStore
import com.vano.n8nmobile.chat.ChatStore
import com.vano.n8nmobile.chat.QuotaTracker
import com.vano.n8nmobile.settings.AiProfileStore
import com.vano.n8nmobile.settings.SettingsStore

@Composable
fun DashboardScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val conversations = remember { ChatStore.loadAll(context) }
    val totalConversations = conversations.size
    val totalMessages = conversations.sumOf { it.messages.size }

    val flows = remember { FlowStore.listFlows(context) }
    val topFlows = remember {
        flows.map { it to FlowRunStatsStore.getRunCount(context, it.id) }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(5)
    }

    val settings = remember { SettingsStore(context) }
    val primaryProviderKey = if (settings.aiProvider == "gemini") "gemini" else "custom_primary"
    val primaryProviderLabel = if (settings.aiProvider == "gemini") "Gemini (Online utama)" else "Provider Kustom (Online utama)"
    val profiles = remember { AiProfileStore.getProfiles(context) }

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
            Text("Dashboard Statistik", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        StatCard("Total Percakapan", "$totalConversations")
        StatCard("Total Pesan", "$totalMessages")
        StatCard("Total Flow Tersimpan", "${flows.size}")

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Pemakaian AI Hari Ini", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        StatCard(primaryProviderLabel, "${QuotaTracker.getTodayCount(context, primaryProviderKey)}x")
        profiles.forEach { profile ->
            StatCard(profile.name, "${QuotaTracker.getTodayCount(context, "profile:${profile.id}")}x")
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Flow Paling Sering Dijalankan", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (topFlows.isEmpty()) {
            Text("Belum ada flow yang dijalankan.", style = MaterialTheme.typography.bodySmall)
        } else {
            topFlows.forEach { (summary, count) ->
                StatCard(summary.name, "${count}x dijalankan")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StatCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}
