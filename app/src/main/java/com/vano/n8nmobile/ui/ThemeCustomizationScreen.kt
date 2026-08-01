package com.vano.n8nmobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun ThemeCustomizationScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { AiwaThemeStore(context) }

    var backgroundHex by remember { mutableStateOf(colorToHex(AiwaColors.Background)) }
    var panelHex by remember { mutableStateOf(colorToHex(AiwaColors.PanelBlack)) }
    var pinkHex by remember { mutableStateOf(colorToHex(AiwaColors.Pink)) }
    var purpleLightHex by remember { mutableStateOf(colorToHex(AiwaColors.PurpleLight)) }
    var purpleDarkHex by remember { mutableStateOf(colorToHex(AiwaColors.PurpleDark)) }
    var textHex by remember { mutableStateOf(colorToHex(AiwaColors.TextLight)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

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
            Text("Kustomisasi Tampilan", style = MaterialTheme.typography.titleLarge)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Isi kode warna 6 digit heksadesimal (tanpa #). Contoh: FF2E93",
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text("Pratinjau", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        val previewBackground = parseHexColor(backgroundHex) ?: AiwaColors.Background
        val previewPink = parseHexColor(pinkHex) ?: AiwaColors.Pink
        val previewPurpleLight = parseHexColor(purpleLightHex) ?: AiwaColors.PurpleLight
        val previewPurpleDark = parseHexColor(purpleDarkHex) ?: AiwaColors.PurpleDark
        val previewText = parseHexColor(textHex) ?: AiwaColors.TextLight

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(previewBackground)
                .padding(16.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(previewPink, previewPurpleLight)))
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Tombol Contoh", color = Color.White)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(previewPurpleDark, previewPurpleLight)))
                        .border(1.dp, previewPink, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text("Contoh balasan AI", color = previewText)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        ColorField("Warna Latar Belakang", backgroundHex) { backgroundHex = it }
        ColorField("Warna Panel Riwayat", panelHex) { panelHex = it }
        ColorField("Warna Aksen / Tombol (Pink)", pinkHex) { pinkHex = it }
        ColorField("Warna Gradient Terang", purpleLightHex) { purpleLightHex = it }
        ColorField("Warna Gradient Gelap", purpleDarkHex) { purpleDarkHex = it }
        ColorField("Warna Teks", textHex) { textHex = it }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }
        savedMessage?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val bg = parseHexColor(backgroundHex)
                val panel = parseHexColor(panelHex)
                val pink = parseHexColor(pinkHex)
                val purpleLight = parseHexColor(purpleLightHex)
                val purpleDark = parseHexColor(purpleDarkHex)
                val text = parseHexColor(textHex)

                if (bg == null || panel == null || pink == null || purpleLight == null || purpleDark == null || text == null) {
                    errorMessage = "Ada kode warna yang gak valid. Pastiin 6 digit heksadesimal, contoh: FF2E93."
                    savedMessage = null
                } else {
                    store.saveAndApply(bg, panel, pink, purpleLight, purpleDark, text)
                    errorMessage = null
                    savedMessage = "Tema disimpan"
                }
            }) { Text("Terapkan Tema") }

            OutlinedButton(onClick = {
                store.resetToDefault()
                backgroundHex = colorToHex(AiwaColors.Background)
                panelHex = colorToHex(AiwaColors.PanelBlack)
                pinkHex = colorToHex(AiwaColors.Pink)
                purpleLightHex = colorToHex(AiwaColors.PurpleLight)
                purpleDarkHex = colorToHex(AiwaColors.PurpleDark)
                textHex = colorToHex(AiwaColors.TextLight)
                errorMessage = null
                savedMessage = "Dikembalikan ke default"
            }) { Text("Reset ke Default") }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun ColorField(label: String, hexValue: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val previewColor = parseHexColor(hexValue) ?: Color.Gray
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(previewColor)
                    .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = hexValue,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Contoh: FF2E93") },
                singleLine = true
            )
        }
    }
}

private fun colorToHex(color: Color): String {
    val argb = color.toArgb()
    return String.format("%06X", argb and 0xFFFFFF)
}

private fun parseHexColor(input: String): Color? {
    val cleaned = input.trim().removePrefix("#")
    if (cleaned.length != 6) return null
    return try {
        Color("FF$cleaned".toLong(16))
    } catch (e: NumberFormatException) {
        null
    }
}
