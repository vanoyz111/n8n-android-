package com.vano.n8nmobile.voice

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
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
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kotlinx.coroutines.launch
import java.util.Locale

private data class LanguageOption(val label: String, val speechTag: String, val locale: Locale)

private val languageOptions = listOf(
    LanguageOption("Indonesia", "id-ID", Locale("id", "ID")),
    LanguageOption("Inggris", "en-US", Locale.US),
    LanguageOption("Jepang", "ja-JP", Locale.JAPAN),
    LanguageOption("Korea", "ko-KR", Locale.KOREA),
    LanguageOption("Mandarin", "zh-CN", Locale.CHINA),
    LanguageOption("Arab", "ar-SA", Locale("ar", "SA")),
    LanguageOption("Spanyol", "es-ES", Locale("es", "ES")),
    LanguageOption("Prancis", "fr-FR", Locale.FRANCE)
)

private data class TranslateTurn(val original: String, val translated: String)

@Composable
fun TranslateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fromLang by remember { mutableStateOf(languageOptions[0]) }
    var toLang by remember { mutableStateOf(languageOptions[1]) }
    var fromMenuExpanded by remember { mutableStateOf(false) }
    var toMenuExpanded by remember { mutableStateOf(false) }

    var isBusy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Tap mic buat mulai") }
    val transcript = remember { mutableStateListOf<TranslateTurn>() }

    val ttsRef = remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(Unit) {
        val instance = TextToSpeech(context) { }
        ttsRef.value = instance
        onDispose { instance.stop(); instance.shutdown() }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!recognized.isNullOrBlank()) {
                status = "Menerjemahkan..."
                scope.launch {
                    val prompt = "Terjemahkan teks berikut dari Bahasa ${fromLang.label} ke Bahasa ${toLang.label}. " +
                        "Jawab HANYA hasil terjemahannya, tanpa penjelasan tambahan:\n\n$recognized"
                    val translated = AiClient.sendMessageWithMode(context, listOf(ChatMessage("user", prompt)), "auto")
                    transcript.add(0, TranslateTurn(recognized, translated))
                    status = "Menjawab..."
                    ttsRef.value?.language = toLang.locale
                    ttsRef.value?.speak(translated, TextToSpeech.QUEUE_FLUSH, null, "aiwa_translate")
                    status = "Tap mic buat lanjut"
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
            status = "Mendengarkan (${fromLang.label})..."
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, fromLang.speechTag)
            }
            try {
                voiceLauncher.launch(intent)
            } catch (e: Exception) {
                status = "Speech recognition gak tersedia"
                isBusy = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
            }
            Text("Terjemahan Langsung", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Ngomong dalam satu bahasa, AI terjemahin dan langsung dibacain.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                Text(
                    fromLang.label,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { fromMenuExpanded = true }
                        .padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                DropdownMenu(expanded = fromMenuExpanded, onDismissRequest = { fromMenuExpanded = false }) {
                    languageOptions.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt.label) }, onClick = { fromLang = opt; fromMenuExpanded = false })
                    }
                }
            }
            IconButton(onClick = { val tmp = fromLang; fromLang = toLang; toLang = tmp }) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Tukar bahasa")
            }
            Box {
                Text(
                    toLang.label,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { toMenuExpanded = true }
                        .padding(12.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                DropdownMenu(expanded = toMenuExpanded, onDismissRequest = { toMenuExpanded = false }) {
                    languageOptions.forEach { opt ->
                        DropdownMenuItem(text = { Text(opt.label) }, onClick = { toLang = opt; toMenuExpanded = false })
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(transcript) { turn ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(turn.original, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(turn.translated, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(status, modifier = Modifier.padding(bottom = 8.dp))

        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(if (isBusy) Color.Gray else MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable(enabled = !isBusy) {
                        isBusy = true
                        micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) CircularProgressIndicator(color = Color.White)
                else Icon(Icons.Default.Mic, contentDescription = "Bicara", tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
