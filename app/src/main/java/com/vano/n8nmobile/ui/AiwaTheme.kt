package com.vano.n8nmobile.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object AiwaColors {
    var Background by mutableStateOf(Color(0xFF0B0620))
    var PanelBlack by mutableStateOf(Color(0xFF07030F))
    var Pink by mutableStateOf(Color(0xFFFF2E93))
    var PurpleLight by mutableStateOf(Color(0xFF4E3FC7))
    var PurpleDark by mutableStateOf(Color(0xFF1B1147))
    var TextLight by mutableStateOf(Color(0xFFF3EEFF))

    fun resetToDefault() {
        Background = Color(0xFF0B0620)
        PanelBlack = Color(0xFF07030F)
        Pink = Color(0xFFFF2E93)
        PurpleLight = Color(0xFF4E3FC7)
        PurpleDark = Color(0xFF1B1147)
        TextLight = Color(0xFFF3EEFF)
    }
}

val AiwaColorScheme: ColorScheme
    get() = darkColorScheme(
        primary = AiwaColors.Pink,
        onPrimary = Color.White,
        primaryContainer = AiwaColors.PurpleLight,
        onPrimaryContainer = AiwaColors.TextLight,
        secondary = AiwaColors.PurpleLight,
        background = AiwaColors.Background,
        onBackground = AiwaColors.TextLight,
        surface = AiwaColors.PanelBlack,
        onSurface = AiwaColors.TextLight,
        surfaceVariant = AiwaColors.PurpleDark,
        onSurfaceVariant = AiwaColors.TextLight,
        outline = AiwaColors.Pink
    )

val AiwaHeaderGradient: Brush
    get() = Brush.verticalGradient(listOf(AiwaColors.PurpleLight, AiwaColors.Background))

val AiwaBubbleGradient: Brush
    get() = Brush.linearGradient(listOf(AiwaColors.PurpleDark, AiwaColors.PurpleLight))

val AiwaPillGradient: Brush
    get() = Brush.horizontalGradient(listOf(AiwaColors.Pink, AiwaColors.PurpleLight))

val AiwaDecorativeFont = FontFamily.Serif
