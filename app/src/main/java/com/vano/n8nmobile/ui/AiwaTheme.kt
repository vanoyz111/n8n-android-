package com.vano.n8nmobile.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

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

class AiBubbleShape(
    private val cornerRadius: Dp,
    private val tailWidth: Dp,
    private val tailHeight: Dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val cr = with(density) { cornerRadius.toPx() }
        val tw = with(density) { tailWidth.toPx() }
        val th = with(density) { tailHeight.toPx() }
        val bodyHeight = size.height - th

        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = bodyHeight,
                    cornerRadius = CornerRadius(cr, cr)
                )
            )
            moveTo(cr * 0.5f, bodyHeight - 2f)
            lineTo(0f, bodyHeight + th)
            lineTo(cr * 0.5f + tw, bodyHeight - 2f)
            close()
        }
        return Outline.Generic(path)
    }
}
