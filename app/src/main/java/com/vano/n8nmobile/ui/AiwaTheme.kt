package com.vano.n8nmobile.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

object AiwaColors {
    val Background = Color(0xFF0B0620)
    val PanelBlack = Color(0xFF07030F)
    val Pink = Color(0xFFFF2E93)
    val PurpleLight = Color(0xFF4E3FC7)
    val PurpleDark = Color(0xFF1B1147)
    val TextLight = Color(0xFFF3EEFF)
}

val AiwaColorScheme = darkColorScheme(
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

val AiwaHeaderGradient = Brush.verticalGradient(listOf(AiwaColors.PurpleLight, AiwaColors.Background))
val AiwaBubbleGradient = Brush.linearGradient(listOf(AiwaColors.PurpleDark, AiwaColors.PurpleLight))
val AiwaPillGradient = Brush.horizontalGradient(listOf(AiwaColors.Pink, AiwaColors.PurpleLight))
val AiwaDecorativeFont = FontFamily.Serif
