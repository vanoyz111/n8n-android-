package com.vano.n8nmobile.imagegen

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private const val CURATED_NAME = "Stable Diffusion 1.5 Q8 (± 1,3 GB)"
private const val CURATED_URL = "https://huggingface.co/second-state/stable-diffusion-v1-5-GGUF/resolve/main/stable-diffusion-v1-5-pruned-emaonly-Q8_0.gguf"
private const val CURATED_FILENAME = "stable-diffusion-v1-5-pruned-emaonly-Q8_0.gguf"

@Composable
fun ImageGenScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloadedName by remember { mutableStateOf(ImageGenModelStore.getDownloadedModelName(context)) }
    var isDownloading by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var customUrl by remember { mutableStateOf("") }
    var customFileName by remember { mutableStateOf("") }

    var prompt by remember { mutableStateOf("") }
    var negativePrompt by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf(20f) }
    var cfgScale by remember { mutableStateOf(7.0f) }
    var isGenerating by remember { mutableStateOf(false) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) resultBitmap?.let { saveMessage = if (saveBitmapToGallery(context, it)) "Tersimpan ke Galeri" else "Gagal menyimpan" }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isImporting = true
            statusMessage = null
            scope.launch {
                try {
                    val name = queryFileName(context, uri) ?: "sd_model.gguf"
                    val destFile = copyUriToAppStorage(context, uri, name)
                    ImageGenModelStore.setDownloadedModel(context, destFile.absolutePath, name)
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
                val file = com.vano.n8nmobile.localai.ModelDownloader.download(context, url, fileName) { p -> progress = p }
                ImageGenModelStore.setDownloadedModel(context, file.absolutePath, name)
                downloadedName = name
                statusMessage = "Model \"$name\" berhasil didownload"
            } catch (e: Exception) {
                statusMessage = "Gagal download: ${e.message}"
            } finally {
                isDownloading = false
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
            Text("Image Generator (Offline)", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Bikin gambar dari teks langsung di HP, gak butuh internet. Butuh model Stable Diffusion format GGUF.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: ${downloadedName ?: "Belum ada model"}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
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
                Text("Stable Diffusion 1.5, hasil 512x512", style = MaterialTheme.typography.bodySmall)
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
        OutlinedTextField(value = customFileName, onValueChange = { customFileName = it }, label = { Text("Nama file") }, modifier = Modifier.fillMaxWidth())
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

        Text("Bikin Gambar", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt (deskripsi gambar)") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(value = negativePrompt, onValueChange = { negativePrompt = it }, label = { Text("Negative prompt (opsional)") }, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))
        Text("Steps: ${steps.toInt()} (lebih tinggi = lebih detail, lebih lama)", style = MaterialTheme.typography.bodySmall)
        Slider(value = steps, onValueChange = { steps = it }, valueRange = 4f..50f)

        Text("CFG Scale: %.1f".format(cfgScale), style = MaterialTheme.typography.bodySmall)
        Slider(value = cfgScale, onValueChange = { cfgScale = it }, valueRange = 1f..15f)

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val path = ImageGenModelStore.getDownloadedModelPath(context)
                if (path == null) {
                    statusMessage = "Download atau import model dulu di atas."
                    return@Button
                }
                if (prompt.isBlank()) return@Button
                isGenerating = true
                resultBitmap = null
                saveMessage = null
                scope.launch {
                    try {
                        val loaded = ImageGenRuntime.ensureLoaded(path)
                        if (!loaded) {
                            statusMessage = "Gagal memuat model. Cek RAM HP cukup."
                        } else {
                            resultBitmap = ImageGenRuntime.generate(
                                prompt = prompt,
                                negativePrompt = negativePrompt,
                                width = 512,
                                height = 512,
                                steps = steps.toInt(),
                                cfgScale = cfgScale
                            )
                            if (resultBitmap == null) statusMessage = "Gagal membuat gambar."
                        }
                    } catch (e: Exception) {
                        statusMessage = "Error: ${e.message}"
                    } finally {
                        isGenerating = false
                    }
                }
            },
            enabled = !isGenerating && prompt.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (isGenerating) "Membuat gambar..." else "Generate") }

        if (isGenerating) {
            Spacer(modifier = Modifier.height(12.dp))
            CircularProgressIndicator()
        }

        resultBitmap?.let { bmp ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveMessage = if (saveBitmapToGallery(context, bmp)) "Tersimpan ke Galeri" else "Gagal menyimpan"
                } else {
                    storagePermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }) { Text("Simpan ke Galeri") }
            saveMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    return try {
        val filename = "aiwa_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Aiwa")
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } else false
    } catch (e: Exception) {
        false
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
