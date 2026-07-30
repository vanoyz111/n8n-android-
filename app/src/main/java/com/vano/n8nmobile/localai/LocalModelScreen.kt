package com.vano.n8nmobile.localai

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val CURATED_NAME = "Qwen2.5 1.5B Instruct (± 1 GB)"
private const val CURATED_URL = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
private const val CURATED_FILENAME = "qwen2.5-1.5b-instruct-q4_k_m.gguf"

private enum class LocalRuntime { GGUF, LITERT }

@Composable
fun LocalModelScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRuntime by remember { mutableStateOf(LocalRuntime.GGUF) }

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
            Text("AI Lokal (Offline)", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Jalanin model AI langsung di HP, gak butuh internet, gak ada limit API. " +
                "Dipakai otomatis kalau AI online gagal/limit.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { selectedRuntime = LocalRuntime.GGUF },
                colors = if (selectedRuntime == LocalRuntime.GGUF) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) { Text("GGUF (Llamatik)") }
            Button(
                onClick = { selectedRuntime = LocalRuntime.LITERT },
                colors = if (selectedRuntime == LocalRuntime.LITERT) ButtonDefaults.buttonColors()
                    else ButtonDefaults.outlinedButtonColors()
            ) { Text("LiteRT-LM (Google)") }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (selectedRuntime == LocalRuntime.GGUF) {
            GgufSection(context, scope)
        } else {
            LiteRtSection(context, scope)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GgufSection(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var downloadedName by remember { mutableStateOf(LocalModelStore.getDownloadedModelName(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var customFileName by remember { mutableStateOf("") }
    var testPrompt by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isImporting = true
            statusMessage = null
            scope.launch {
                try {
                    val name = queryFileName(context, uri) ?: "model_lokal.gguf"
                    val destFile = copyUriToAppStorage(context, uri, name)
                    LocalModelStore.setDownloadedModel(context, destFile.absolutePath, name)
                    downloadedName = name
                    statusMessage = "Model \"$name\" berhasil diimport"
                } catch (e: Exception) {
                    statusMessage = "Gagal import: ${e.message}"
                } finally {
                    isImporting = false
                }
            }
        }
    }

    fun startDownload(url: String, name: String, fileName: String) {
        if (url.isBlank() || fileName.isBlank()) {
            statusMessage = "URL atau nama file kosong"
            return
        }
        isDownloading = true
        progress = 0f
        statusMessage = null
        scope.launch {
            try {
                val file = ModelDownloader.download(context, url, fileName) { p -> progress = p }
                LocalModelStore.setDownloadedModel(context, file.absolutePath, name)
                downloadedName = name
                statusMessage = "Model \"$name\" berhasil didownload"
            } catch (e: Exception) {
                statusMessage = "Gagal download: ${e.message}"
            } finally {
                isDownloading = false
            }
        }
    }

    Text("Status: ${downloadedName ?: "Belum ada model"}", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(16.dp))

    Text("Import File Lokal", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(if (isImporting) "Menyalin file..." else "Import File Lokal", color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Button(
        onClick = { importLauncher.launch(arrayOf("*/*")) },
        enabled = !isImporting && !isDownloading,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (isImporting) "Menyalin..." else "Pilih File .gguf dari HP") }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Model Rekomendasi", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(CURATED_NAME, style = MaterialTheme.typography.bodyMedium)
            Text("Cocok buat kebanyakan HP, respons cepat", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { startDownload(CURATED_URL, CURATED_NAME, CURATED_FILENAME) }, enabled = !isDownloading && !isImporting) {
                Text("Download")
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Atau Pakai URL GGUF Sendiri", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("URL langsung ke file .gguf") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = customFileName, onValueChange = { customFileName = it }, label = { Text("Nama file (contoh: model.gguf)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { startDownload(customUrl.trim(), customFileName.trim(), customFileName.trim()) }, enabled = !isDownloading && !isImporting) {
        Text("Download Model Kustom")
    }

    if (isDownloading) {
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
    statusMessage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Tes Model", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = testPrompt, onValueChange = { testPrompt = it }, label = { Text("Ketik pertanyaan buat tes") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = {
            val path = LocalModelStore.getDownloadedModelPath(context)
            if (path == null) {
                testResult = "Download atau import model dulu di atas."
                return@Button
            }
            isTesting = true
            testResult = null
            scope.launch {
                try {
                    val loaded = LocalModelRuntime.ensureLoaded(path)
                    if (!loaded) {
                        testResult = "Gagal memuat model. Cek RAM HP cukup atau coba model lebih kecil."
                    } else {
                        val prompt = LocalModelRuntime.applyChatTemplate(listOf("user" to testPrompt)) ?: testPrompt
                        testResult = LocalModelRuntime.generate(prompt)
                    }
                } catch (e: Exception) {
                    testResult = "Error: ${e.message}"
                } finally {
                    isTesting = false
                }
            }
        },
        enabled = !isTesting && testPrompt.isNotBlank()
    ) { Text(if (isTesting) "Memproses..." else "Tes Sekarang") }
    testResult?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it)
    }
}

@Composable
private fun LiteRtSection(context: Context, scope: kotlinx.coroutines.CoroutineScope) {
    var downloadedName by remember { mutableStateOf(LiteRtModelStore.getDownloadedModelName(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isImporting by remember { mutableStateOf(false) }
    var customUrl by remember { mutableStateOf("") }
    var customFileName by remember { mutableStateOf("") }
    var testPrompt by remember { mutableStateOf("") }
    var testResult by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isImporting = true
            statusMessage = null
            scope.launch {
                try {
                    val name = queryFileName(context, uri) ?: "model_lokal.litertlm"
                    val destFile = copyUriToAppStorage(context, uri, name)
                    LiteRtModelStore.setDownloadedModel(context, destFile.absolutePath, name)
                    downloadedName = name
                    statusMessage = "Model \"$name\" berhasil diimport"
                } catch (e: Exception) {
                    statusMessage = "Gagal import: ${e.message}"
                } finally {
                    isImporting = false
                }
            }
        }
    }

    fun startDownload(url: String, name: String, fileName: String) {
        if (url.isBlank() || fileName.isBlank()) {
            statusMessage = "URL atau nama file kosong"
            return
        }
        isDownloading = true
        progress = 0f
        statusMessage = null
        scope.launch {
            try {
                val file = ModelDownloader.download(context, url, fileName) { p -> progress = p }
                LiteRtModelStore.setDownloadedModel(context, file.absolutePath, name)
                downloadedName = name
                statusMessage = "Model \"$name\" berhasil didownload"
            } catch (e: Exception) {
                statusMessage = "Gagal download: ${e.message}"
            } finally {
                isDownloading = false
            }
        }
    }

    Text("Status: ${downloadedName ?: "Belum ada model"}", style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        "Model LiteRT-LM (.litertlm) didownload dari huggingface.co/litert-community. " +
            "Buka link itu di browser, pilih model (misal Gemma3-1B-IT), copy link file .litertlm-nya ke kolom bawah.",
        style = MaterialTheme.typography.bodySmall
    )

    Spacer(modifier = Modifier.height(16.dp))
    Text("Import File Lokal", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.padding(4.dp))
            Text(if (isImporting) "Menyalin file..." else "Import File Lokal", color = MaterialTheme.colorScheme.primary)
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Button(
        onClick = { importLauncher.launch(arrayOf("*/*")) },
        enabled = !isImporting && !isDownloading,
        modifier = Modifier.fillMaxWidth()
    ) { Text(if (isImporting) "Menyalin..." else "Pilih File .litertlm dari HP") }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Download dari URL", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = customUrl, onValueChange = { customUrl = it }, label = { Text("URL langsung ke file .litertlm") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = customFileName, onValueChange = { customFileName = it }, label = { Text("Nama file (contoh: gemma3-1b.litertlm)") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = { startDownload(customUrl.trim(), customFileName.trim(), customFileName.trim()) }, enabled = !isDownloading && !isImporting) {
        Text("Download Model")
    }

    if (isDownloading) {
        Spacer(modifier = Modifier.height(12.dp))
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    }
    statusMessage?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it, style = MaterialTheme.typography.bodySmall)
    }

    Spacer(modifier = Modifier.height(24.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))

    Text("Tes Model", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(value = testPrompt, onValueChange = { testPrompt = it }, label = { Text("Ketik pertanyaan buat tes") }, modifier = Modifier.fillMaxWidth())
    Spacer(modifier = Modifier.height(8.dp))
    Button(
        onClick = {
            val path = LiteRtModelStore.getDownloadedModelPath(context)
            if (path == null) {
                testResult = "Download atau import model dulu di atas."
                return@Button
            }
            isTesting = true
            testResult = null
            scope.launch {
                try {
                    val loaded = LiteRtRuntime.ensureLoaded(context, path, "")
                    testResult = if (!loaded) {
                        "Gagal memuat model. Cek RAM HP cukup atau coba model lebih kecil."
                    } else {
                        LiteRtRuntime.generate(testPrompt)
                    }
                } catch (e: Exception) {
                    testResult = "Error: ${e.message}"
                } finally {
                    isTesting = false
                }
            }
        },
        enabled = !isTesting && testPrompt.isNotBlank()
    ) { Text(if (isTesting) "Memproses..." else "Tes Sekarang") }
    testResult?.let {
        Spacer(modifier = Modifier.height(8.dp))
        Text(it)
    }
}

private fun queryFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    }
    return name
}

private suspend fun copyUriToAppStorage(context: Context, uri: Uri, fileName: String): File =
    withContext(Dispatchers.IO) {
        val destDir = context.getExternalFilesDir(null) ?: context.filesDir
        val destFile = File(destDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output, bufferSize = 65536)
            }
        } ?: throw IllegalStateException("Gak bisa buka file yang dipilih")
        destFile
    }
