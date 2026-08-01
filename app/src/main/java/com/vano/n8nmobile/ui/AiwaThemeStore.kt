package com.vano.n8nmobile.ui

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

class AiwaThemeStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveAndApply(
        background: Color,
        panelBlack: Color,
        pink: Color,
        purpleLight: Color,
        purpleDark: Color,
        textLight: Color
    ) {
        prefs.edit()
            .putInt(KEY_BACKGROUND, background.toArgb())
            .putInt(KEY_PANEL, panelBlack.toArgb())
            .putInt(KEY_PINK, pink.toArgb())
            .putInt(KEY_PURPLE_LIGHT, purpleLight.toArgb())
            .putInt(KEY_PURPLE_DARK, purpleDark.toArgb())
            .putInt(KEY_TEXT, textLight.toArgb())
            .apply()

        AiwaColors.Background = background
        AiwaColors.PanelBlack = panelBlack
        AiwaColors.Pink = pink
        AiwaColors.PurpleLight = purpleLight
        AiwaColors.PurpleDark = purpleDark
        AiwaColors.TextLight = textLight
    }

    fun loadIntoMemory() {
        if (!prefs.contains(KEY_PINK)) return
        AiwaColors.Background = Color(prefs.getInt(KEY_BACKGROUND, AiwaColors.Background.toArgb()))
        AiwaColors.PanelBlack = Color(prefs.getInt(KEY_PANEL, AiwaColors.PanelBlack.toArgb()))
        AiwaColors.Pink = Color(prefs.getInt(KEY_PINK, AiwaColors.Pink.toArgb()))
        AiwaColors.PurpleLight = Color(prefs.getInt(KEY_PURPLE_LIGHT, AiwaColors.PurpleLight.toArgb()))
        AiwaColors.PurpleDark = Color(prefs.getInt(KEY_PURPLE_DARK, AiwaColors.PurpleDark.toArgb()))
        AiwaColors.TextLight = Color(prefs.getInt(KEY_TEXT, AiwaColors.TextLight.toArgb()))
    }

    fun resetToDefault() {
        prefs.edit().clear().apply()
        AiwaColors.resetToDefault()
    }

    companion object {
        private const val PREFS_NAME = "n8n_mobile_theme"
        private const val KEY_BACKGROUND = "bg"
        private const val KEY_PANEL = "panel"
        private const val KEY_PINK = "pink"
        private const val KEY_PURPLE_LIGHT = "purple_light"
        private const val KEY_PURPLE_DARK = "purple_dark"
        private const val KEY_TEXT = "text"
    }
}
