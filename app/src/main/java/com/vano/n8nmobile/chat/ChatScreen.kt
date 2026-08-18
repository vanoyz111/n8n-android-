package com.vano.n8nmobile.chat

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.speech.RecognizerIntent
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.vano.n8nmobile.R
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.settings.AiProfileStore
import com.vano.n8nmobile.ui.AiwaBubbleGradient
import com.vano.n8nmobile.ui.AiwaColors
import com.vano.n8nmobile.ui.AiwaDecorativeFont
import com.vano.n8nmobile.ui.AiwaPillGradient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
fun ChatScreen(
    messages: MutableList<ChatMessage>,
    onOpenDrawer: () -> Unit,
    onNewChat: () -> Unit,
    onMessagesChanged: () -> Unit,
    onOpenAiProviders: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var attachMenuExpanded by remember { mutableStateOf(false) }
    var chatMode by remember { mutableStateOf(ChatModeStore.getMode(context)) }
    var thinkingEnabled by remember { mutableStateOf(ChatModeStore.isThinkingEnabled(context)) }
    var modeMenuExpanded by remember { mutableStateOf(false) }
    var animatedUpTo by remember { mutableStateOf(messages.size - 1) }

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

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = results?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                input = if (input.isBlank()) recognized else "$input $recognized"
            }
        }
    }

    val voicePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang...")
            }
            try {
                voiceLauncher.launch(intent)
            } catch (e: Exception) {
                AppLog.add("VOICE_ERROR", "Speech recognition gak tersedia: ${e.message}")
            }
        }
    }

    fun sendCurrentInput() {
        val text = input.trim()
        if ((text.isEmpty() && pendingImageBase64 == null && pendingAttachmentName == null) || isLoading) return
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
            val aiMessageIndex = messages.size
            messages.add(ChatMessage(role = "ai", text = ""))
            var streamed = false
            val finalReply = AiClient.sendMessageStreaming(context, historySnapshot, chatMode) { partial ->
                streamed = true
                if (aiMessageIndex < messages.size) {
                    messages[aiMessageIndex] = messages[aiMessageIndex].copy(text = partial)
                }
            }
            if (aiMessageIndex < messages.size) {
                messages[aiMessageIndex] = messages[aiMessageIndex].copy(text = finalReply)
            }
            if (streamed) animatedUpTo = aiMessageIndex
            isLoading = false
            onMessagesChanged()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().systemBarsPadding().imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AiwaColors.Pink)
                    .clickable(onClick = onOpenDrawer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }

            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(AiwaPillGradient)
                        .clickable { modeMenuExpanded = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ChatModeStore.labelFor(context, chatMode) + if (thinkingEnabled) " 💭" else "",
                            color = Color.White,
                            fontFamily = AiwaDecorativeFont,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
                DropdownMenu(expanded = modeMenuExpanded, onDismissRequest = { modeMenuExpanded = false }) {
                    ChatModeStore.builtInModes.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(ChatModeStore.labelFor(context, mode)) },
                            onClick = {
                                chatMode = mode
                                ChatModeStore.setMode(context, mode)
                                modeMenuExpanded = false
                            }
                        )
                    }
                    val profiles = AiProfileStore.getProfiles(context)
                    if (profiles.isNotEmpty()) {
                        HorizontalDivider()
                        profiles.forEach { profile ->
                            val modeValue = "profile:${profile.id}"
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = {
                                    chatMode = modeValue
                                    ChatModeStore.setMode(context, modeValue)
                                    modeMenuExpanded = false
                                }
                            )
                        }
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(if (thinkingEnabled) "💭 Mode Thinking: ON" else "💭 Mode Thinking: OFF") },
                        onClick = {
                            thinkingEnabled = !thinkingEnabled
                            ChatModeStore.setThinkingEnabled(context, thinkingEnabled)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("⚙ Kelola Provider AI") },
                        onClick = {
                            modeMenuExpanded = false
                            onOpenAiProviders()
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AiwaColors.Pink)
                    .clickable {
                        messages.clear()
                        onNewChat()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Chat Baru", tint = Color.White)
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Halo, ada yang bisa saya bantu?",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                reverseLayout = true
            ) {
                itemsIndexed(messages.reversed()) { reversedIndex, msg ->
                    val originalIndex = messages.size - 1 - reversedIndex
                    val isUser = msg.role == "user"
                    val shouldAnimate = !isUser && originalIndex == messages.lastIndex && originalIndex > animatedUpTo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        verticalAlignment = Alignment.Top
                    ) {
                        if (!isUser) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.5.dp, AiwaColors.Pink),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    if (isUser) SolidColor(AiwaColors.Pink) else AiwaBubbleGradient,
                                    RoundedCornerShape(20.dp)
                                )
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                MessageContent(
                                    msg = msg,
                                    isLatestAi = !isUser && originalIndex == messages.lastIndex,
                                    animate = shouldAnimate,
                                    onAnimationDone = { animatedUpTo = originalIndex }
                                )
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
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AiwaColors.Pink)
                        .clickable { clearPendingAttachment() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Batal lampiran", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AiwaColors.Pink)
                        .clickable { attachMenuExpanded = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Lampirkan", tint = Color.White)
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

            Spacer(modifier = Modifier.width(6.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AiwaColors.Pink)
                    .clickable { voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Bicara", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(6.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).heightIn(max = 120.dp),
                placeholder = { Text("Tanya AI...") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AiwaColors.Pink)
                    .clickable { sendCurrentInput() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Send, contentDescription = "Kirim", tint = Color.White)
            }
        }
    }
}

@Composable
private fun MessageContent(
    msg: ChatMessage,
    isLatestAi: Boolean = false,
    animate: Boolean = false,
    onAnimationDone: () -> Unit = {}
) {
    msg.imageBase64?.let { b64 ->
        val bitmap = remember(b64) { decodeBase64ToBitmap(b64) }
        bitmap?.let {
            Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.size(180.dp))
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    msg.attachmentName?.let { name ->
        Text("\ud83d\udcce $name", color = Color.White, style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(4.dp))
    }

    if (msg.role != "user" && msg.text.isBlank() && isLatestAi) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
    } else if (msg.text.isNotBlank()) {
        val (thinking, mainText) = remember(msg.text) { extractThinking(msg.text) }

        thinking?.let { ThinkingBlock(it) }

        if (animate) {
            var visibleChars by remember(mainText) { mutableStateOf(0) }
            LaunchedEffect(mainText) {
                val step = maxOf(1, mainText.length / 100)
                while (visibleChars < mainText.length) {
                    visibleChars = minOf(mainText.length, visibleChars + step)
                    delay(12L)
                }
                onAnimationDone()
            }
            RenderMessageBody(mainText.substring(0, visibleChars))
        } else {
            RenderMessageBody(mainText)
        }
    }
}

@Composable
private fun ThinkingBlock(thinking: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable { expanded = !expanded }
            .padding(10.dp)
    ) {
        Text(
            if (expanded) "💭 Proses berpikir (tap buat tutup)" else "💭 Proses berpikir (tap buat lihat)",
            color = Color.Gray,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )
        if (expanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(thinking, color = Color.Gray, fontSize = 12.sp, fontStyle = FontStyle.Italic)
        }
    }
}

@Composable
private fun RenderMessageBody(text: String) {
    val segments = remember(text) { parseSegments(text) }
    Column {
        segments.forEach { seg ->
            if (seg.isCode) {
                CodeBlock(seg.content, seg.language)
            } else if (seg.content.isNotBlank()) {
                Text(seg.content.trim('\n'), color = Color.White)
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String, language: String?) {
    val clipboardManager = LocalClipboardManager.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0D0D))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(language ?: "code", color = Color.Gray, fontSize = 11.sp)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .clickable { clipboardManager.setText(AnnotatedString(code)) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Salin kode", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                code.trim('\n'),
                color = Color(0xFF7CFC9A),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                modifier = Modifier.horizontalScroll(rememberScrollState())
            )
        }
    }
}

private data class TextSegment(val isCode: Boolean, val language: String?, val content: String)

private fun parseSegments(text: String): List<TextSegment> {
    val segments = mutableListOf<TextSegment>()
    val regex = Regex("```(\\w*)\\n?([\\s\\S]*?)```")
    var lastEnd = 0
    regex.findAll(text).forEach { match ->
        if (match.range.first > lastEnd) {
            segments.add(TextSegment(false, null, text.substring(lastEnd, match.range.first)))
        }
        val lang = match.groupValues[1].ifBlank { null }
        val code = match.groupValues[2]
        segments.add(TextSegment(true, lang, code))
        lastEnd = match.range.last + 1
    }
    if (lastEnd < text.length) {
        segments.add(TextSegment(false, null, text.substring(lastEnd)))
    }
    if (segments.isEmpty()) segments.add(TextSegment(false, null, text))
    return segments
}

private fun extractThinking(text: String): Pair<String?, String> {
    val regex = Regex("<thinking>([\\s\\S]*?)</thinking>", RegexOption.IGNORE_CASE)
    val match = regex.find(text)
    return if (match != null) {
        val thinkingText = match.groupValues[1].trim()
        val rest = text.removeRange(match.range).trim()
        thinkingText to rest
    } else {
        null to text
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
