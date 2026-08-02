package com.vano.n8nmobile.localai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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

private enum class SettingsRuntime { GGUF, LITERT }

@Composable
fun ModelSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var selectedRuntime by remember { mutableStateOf(SettingsRuntime.GGUF) }

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
            Text("Pengaturan Model", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedRuntime = SettingsRuntime.GGUF },
                colors = if (selectedRuntime == SettingsRuntime.GGUF) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) { Text("GGUF (Llamatik)") }
            Button(
                onClick = { selectedRuntime = SettingsRuntime.LITERT },
                colors = if (selectedRuntime == SettingsRuntime.LITERT) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) { Text("LiteRT-LM") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedRuntime == SettingsRuntime.GGUF) {
            GgufSettingsSection(context)
        } else {
            LiteRtSettingsSection(context)
        }
    }
}

@Composable
private fun GgufSettingsSection(context: android.content.Context) {
    var temperature by remember { mutableStateOf(LocalAiSettingsStore.getGgufTemperature(context)) }
    var topP by remember { mutableStateOf(LocalAiSettingsStore.getGgufTopP(context)) }
    var topK by remember { mutableStateOf(LocalAiSettingsStore.getGgufTopK(context).toFloat()) }
    var maxTokens by remember { mutableStateOf(LocalAiSettingsStore.getGgufMaxTokens(context).toFloat()) }
    var repeatPenalty by remember { mutableStateOf(LocalAiSettingsStore.getGgufRepeatPenalty(context)) }
    var contextLength by remember { mutableStateOf(LocalAiSettingsStore.getGgufContextLength(context).toFloat()) }
    var gpuLayers by remember { mutableStateOf(LocalAiSettingsStore.getGgufGpuLayers(context).toFloat()) }
    var threads by remember { mutableStateOf(LocalAiSettingsStore.getGgufThreads(context).toFloat()) }

    Text("Atur cara model GGUF menjawab.", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(16.dp))

    SettingSlider("Temperature", "Lebih tinggi = lebih kreatif, lebih rendah = lebih fokus",
        temperature, "%.2f".format(temperature), 0f, 2f) {
        temperature = it; LocalAiSettingsStore.setGgufTemperature(context, it)
    }
    SettingSlider("Top P", "Nucleus sampling threshold", topP, "%.2f".format(topP), 0f, 1f) {
        topP = it; LocalAiSettingsStore.setGgufTopP(context, it)
    }
    SettingSlider("Top K", "Jumlah kandidat kata teratas yang dipertimbangkan", topK, topK.toInt().toString(), 1f, 100f) {
        topK = it; LocalAiSettingsStore.setGgufTopK(context, it.toInt())
    }
    SettingSlider("Max Tokens", "Panjang maksimal jawaban", maxTokens, maxTokens.toInt().toString(), 64f, 4096f) {
        maxTokens = it; LocalAiSettingsStore.setGgufMaxTokens(context, it.toInt())
    }
    SettingSlider("Repeat Penalty", "Lebih tinggi = kurangi pengulangan kata", repeatPenalty, "%.2f".format(repeatPenalty), 1f, 2f) {
        repeatPenalty = it; LocalAiSettingsStore.setGgufRepeatPenalty(context, it)
    }
    SettingSlider("Context Length", "Total memori percakapan (butuh reload model)", contextLength, contextLength.toInt().toString(), 512f, 8192f) {
        contextLength = it; LocalAiSettingsStore.setGgufContextLength(context, it.toInt())
    }
    SettingSlider("GPU Layers", "0 = CPU saja, lebih tinggi = lebih banyak dibebankan ke GPU (butuh reload model)", gpuLayers, gpuLayers.toInt().toString(), 0f, 100f) {
        gpuLayers = it; LocalAiSettingsStore.setGgufGpuLayers(context, it.toInt())
    }
    SettingSlider("Threads", "Jumlah thread CPU dipakai", threads, threads.toInt().toString(), 1f, 8f) {
        threads = it; LocalAiSettingsStore.setGgufThreads(context, it.toInt())
    }

    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(onClick = {
        LocalAiSettingsStore.setGgufTemperature(context, 0.7f); temperature = 0.7f
        LocalAiSettingsStore.setGgufTopP(context, 0.95f); topP = 0.95f
        LocalAiSettingsStore.setGgufTopK(context, 40); topK = 40f
        LocalAiSettingsStore.setGgufMaxTokens(context, 512); maxTokens = 512f
        LocalAiSettingsStore.setGgufRepeatPenalty(context, 1.1f); repeatPenalty = 1.1f
        LocalAiSettingsStore.setGgufContextLength(context, 4096); contextLength = 4096f
        LocalAiSettingsStore.setGgufGpuLayers(context, 0); gpuLayers = 0f
        LocalAiSettingsStore.setGgufThreads(context, 4); threads = 4f
    }) { Text("Reset ke Default") }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun LiteRtSettingsSection(context: android.content.Context) {
    var temperature by remember { mutableStateOf(LocalAiSettingsStore.getLitertTemperature(context)) }
    var topP by remember { mutableStateOf(LocalAiSettingsStore.getLitertTopP(context)) }
    var topK by remember { mutableStateOf(LocalAiSettingsStore.getLitertTopK(context).toFloat()) }
    var useGpu by remember { mutableStateOf(LocalAiSettingsStore.isLitertGpuEnabled(context)) }

    Text("Atur cara model LiteRT-LM menjawab.", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(16.dp))

    SettingSlider("Temperature", "Lebih tinggi = lebih kreatif, lebih rendah = lebih fokus",
        temperature, "%.2f".format(temperature), 0f, 2f) {
        temperature = it; LocalAiSettingsStore.setLitertTemperature(context, it)
    }
    SettingSlider("Top P", "Nucleus sampling threshold", topP, "%.2f".format(topP), 0f, 1f) {
        topP = it; LocalAiSettingsStore.setLitertTopP(context, it)
    }
    SettingSlider("Top K", "Jumlah kandidat kata teratas yang dipertimbangkan", topK, topK.toInt().toString(), 1f, 100f) {
        topK = it; LocalAiSettingsStore.setLitertTopK(context, it.toInt())
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text("Akselerasi", style = MaterialTheme.typography.titleMedium)
    Text("Jalankan lewat GPU. Performa terbaik di kebanyakan device (butuh reload model).", style = MaterialTheme.typography.bodySmall)
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = { useGpu = false; LocalAiSettingsStore.setLitertGpuEnabled(context, false) },
            colors = if (!useGpu) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) { Text("CPU") }
        Button(
            onClick = { useGpu = true; LocalAiSettingsStore.setLitertGpuEnabled(context, true) },
            colors = if (useGpu) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
        ) { Text("GPU") }
    }

    Spacer(modifier = Modifier.height(20.dp))
    OutlinedButton(onClick = {
        LocalAiSettingsStore.setLitertTemperature(context, 0.8f); temperature = 0.8f
        LocalAiSettingsStore.setLitertTopP(context, 0.95f); topP = 0.95f
        LocalAiSettingsStore.setLitertTopK(context, 40); topK = 40f
        LocalAiSettingsStore.setLitertGpuEnabled(context, false); useGpu = false
    }) { Text("Reset ke Default") }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
private fun SettingSlider(
    label: String,
    description: String,
    value: Float,
    valueLabel: String,
    min: Float,
    max: Float,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(valueLabel, style = MaterialTheme.typography.bodyMedium)
        }
        Text(description, style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = min..max)
    }
}
