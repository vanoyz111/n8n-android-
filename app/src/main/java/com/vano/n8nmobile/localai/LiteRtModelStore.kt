package com.vano.n8nmobile.localai

import android.content.Context
import java.io.File

object LiteRtModelStore {
    private const val PREFS_NAME = "n8n_mobile_litert"
    private const val KEY_MODEL_PATH = "model_path"
    private const val KEY_MODEL_NAME = "model_name"

    fun getDownloadedModelPath(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val path = prefs.getString(KEY_MODEL_PATH, null) ?: return null
        return if (File(path).exists()) path else null
    }

    fun getDownloadedModelName(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_MODEL_NAME, null)

    fun setDownloadedModel(context: Context, path: String?, name: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_MODEL_PATH, path)
            .putString(KEY_MODEL_NAME, name)
            .apply()
    }
}
