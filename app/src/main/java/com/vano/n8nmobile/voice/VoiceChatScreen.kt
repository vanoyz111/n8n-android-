package com.vano.n8nmobile.voice

import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.chat.ChatModeStore
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

private data class VoiceTurn(val speaker: String, val text: String)

@Composable
fun VoiceChatScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var status by remember { mutableStateOf("Tap mic buat mulai") }
    var isBusy by remember { mutableStateOf(false) }
    var continuousMode by remember { mutableStateOf(true) }
    val transcript = remember { mutableStateListOf<VoiceTurn>() }
    var launchListenSignal by remember { mutableStateOf(0) }

    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                transcript.add(VoiceTurn("Kamu", recognized))
                status = "Memproses..."
                scope.launch {
                    val mode = ChatModeStore.getMode(context)
                    val history = transcript.map { ChatMessage(if (it.speaker == "Kamu") "user" else "ai", it.text) }
                    val reply = AiClient.sendMessageWithMode(context, history, mode)
                    transcript.add(VoiceTurn("AI", reply))
                    status = "Menjawab..."
                    val utteranceId = UUID.randomUUID().toString()
                    ttsRef.value?.speak(reply.take(4000), TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                    isBusy = false
                }
            } else {
                status = "Gak kedengeran, coba lagi"
                isBusy = false
            }
        } else {
            status = "Dibatalkan"
            isBusy = false
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            status = "Mendengarkan..."
            isBusy = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            }
            try {
                voiceLauncher.launch(intent)
            } catch (e: Exception) {
                status = "Speech recognition gak tersedia di HP ini"
                isBusy = false
            }
        }
    }

    fun startListening() {
        status = "Mendengarkan..."
        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
    }

    DisposableEffect(Unit) {
        val instance = TextToSpeech(context) { }
        instance.language = Locale("id", "ID")
        instance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { status = "Menjawab..." }
            }
            override fun onDone(utteranceId: String?) {
                mainHandler.post {
                    status = "Tap mic buat lanjut"
                    if (continuousMode) launchListenSignal++
                }
            }
            override fun onError(utteranceId: String?) {
                mainHandler.post { status = "Error suara" }
            }
        })
        ttsRef.value = instance
        onDispose { instance.stop(); instance.shutdown() }
    }

    LaunchedEffect(launchListenSignal) {
        if (launchListenSignal > 0) startListening()
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Mode Suara Penuh", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Ngobrol pakai suara — tap mic, ngomong, AI jawab & langsung dibacain. " +
                "Percakapan ini gak tersimpan ke riwayat chat.",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mode Berkelanjutan (otomatis dengerin lagi abis AI jawab)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            Switch(checked = continuousMode, onCheckedChange = { continuousMode = it })
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(transcript) { turn ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(turn.speaker, style = MaterialTheme.typography.labelLarge)
                        Text(turn.text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(status, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(if (isBusy) Color.Gray else MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
                    .clickable(enabled = !isBusy) { isBusy = true; startListening() },
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Icon(Icons.Default.Mic, contentDescription = "Bicara", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
