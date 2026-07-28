package com.vano.n8nmobile.chat

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun ChatScreen(
    messages: MutableList<ChatMessage>,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onMessagesChanged: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var attachMenuExpanded by remember { mutableStateOf(false) }

    var pendingImageBase64 by remember { mutableStateOf<String?>(null) }
    var pendingImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingAttachmentName by remember { mutableStateOf<String?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun clearPendingAttachment() {
        pendingImageBase64 = null
        pendingImageBitmap = null
        pendingAttachmentName = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            uriToScaledJpegBase64(context, uri)?.let { (b64, bmp) ->
                pendingImageBase64 = b64
                pendingImageBitmap = bmp
                pendingAttachmentName = null
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageCaptureUri(context)
            pendingCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uriToScaledJpegBase64(context, uri)?.let { (b64, bmp) ->
                pendingImageBase64 = b64
                pendingImageBitmap = bmp
                pendingAttachmentName = null
            }
        }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pendingAttachmentName = queryFileName(context, uri) ?: "file"
            pendingImageBase64 = null
            pendingImageBitmap = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
            IconButton(onClick = {
                clearPendingAttachment()
                onNewChat()
            }) {
                Icon(Icons.Default.Add, contentDescription = "Chat Baru")
            }
        }

        if (messages.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Halo, ada yang bisa saya bantu?",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                reverseLayout = true
            ) {
                if (isLoading) {
                    item {
                        Row(modifier = Modifier.padding(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        }
                    }
                }
                items(messages.reversed()) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                msg.imageBase64?.let { b64 ->
                                    val bitmap = remember(b64) { decodeBase64ToBitmap(b64) }
                                    bitmap?.let {
                                        Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(180.dp))
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                                msg.attachmentName?.let { name ->
                                    Text("📎 $name", style = MaterialTheme.typography.bodySmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                if (msg.text.isNotBlank()) {
                                    Text(msg.text)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (pendingImageBitmap != null || pendingAttachmentName != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                pendingImageBitmap?.let { bmp ->
                    Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp))
                }
                pendingAttachmentName?.let { name ->
                    Text("📎 $name", modifier = Modifier.padding(start = 8.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { clearPendingAttachment() }) {
                    Icon(Icons.Default.Close, contentDescription = "Batal lampiran")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                IconButton(onClick = { attachMenuExpanded = true }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Lampirkan")
                }
                DropdownMenu(expanded = attachMenuExpanded, onDismissRequest = { attachMenuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Kamera") }, onClick = {
                        attachMenuExpanded = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    })
                    DropdownMenuItem(text = { Text("Galeri") }, onClick = {
                        attachMenuExpanded = false
                        galleryLauncher.launch("image/*")
                    })
                    DropdownMenuItem(text = { Text("File") }, onClick = {
                        attachMenuExpanded = false
                        fileLauncher.launch("*/*")
                    })
                }
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Tanya AI...") },
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.padding(4.dp))
            IconButton(
                onClick = {
                    val text = input.trim()
                    if ((text.isEmpty() && pendingImageBase64 == null && pendingAttachmentName == null) || isLoading) return@IconButton
                    messages.add(
                        ChatMessage(
                            role = "user",
                            text = text,
                            imageBase64 = pendingImageBase64,
                            imageMimeType = if (pendingImageBase64 != null) "image/jpeg" else null,
                            attachmentName = pendingAttachmentName
                        )
                    )
                    input = ""
                    clearPendingAttachment()
                    isLoading = true
                    onMessagesChanged()
                    AppLog.add("CHAT", "User: ${text.take(60)}")
                    scope.launch {
                        val historySnapshot = messages.toList()
                        val reply = AiClient.sendMessage(context, historySnapshot)
                        messages.add(ChatMessage(role = "ai", text = reply))
                        isLoading = false
                        onMessagesChanged()
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim")
            }
        }
    }
}

private fun createImageCaptureUri(context: Context): Uri {
    val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
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

private fun uriToScaledJpegBase64(context: Context, uri: Uri, maxDimension: Int = 1024): Pair<String, Bitmap>? {
    return try {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        val original = BitmapFactory.decodeStream(input)
        input.close()
        if (original == null) return null
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(original.width, original.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
        } else original
        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP) to scaled
    } catch (e: Exception) {
        null
    }
}

private fun decodeBase64ToBitmap(base64: String): Bitmap? {
    return try {
        val bytes = Base64.decode(base64, Base64.NO_WRAP)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}
