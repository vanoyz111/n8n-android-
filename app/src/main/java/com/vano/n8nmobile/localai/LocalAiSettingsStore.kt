package com.vano.n8nmobile.localai

import android.content.Context

object LocalAiSettingsStore {
    private const val PREFS_NAME = "n8n_mobile_local_ai_settings"
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getGgufTemperature(context: Context) = prefs(context).getFloat("gguf_temp", 0.7f)
    fun setGgufTemperature(context: Context, v: Float) { prefs(context).edit().putFloat("gguf_temp", v).apply() }

    fun getGgufTopP(context: Context) = prefs(context).getFloat("gguf_top_p", 0.95f)
    fun setGgufTopP(context: Context, v: Float) { prefs(context).edit().putFloat("gguf_top_p", v).apply() }

    fun getGgufTopK(context: Context) = prefs(context).getInt("gguf_top_k", 40)
    fun setGgufTopK(context: Context, v: Int) { prefs(context).edit().putInt("gguf_top_k", v).apply() }

    fun getGgufMaxTokens(context: Context) = prefs(context).getInt("gguf_max_tokens", 512)
    fun setGgufMaxTokens(context: Context, v: Int) { prefs(context).edit().putInt("gguf_max_tokens", v).apply() }

    fun getGgufRepeatPenalty(context: Context) = prefs(context).getFloat("gguf_repeat_penalty", 1.1f)
    fun setGgufRepeatPenalty(context: Context, v: Float) { prefs(context).edit().putFloat("gguf_repeat_penalty", v).apply() }

    fun getGgufContextLength(context: Context) = prefs(context).getInt("gguf_context_length", 4096)
    fun setGgufContextLength(context: Context, v: Int) { prefs(context).edit().putInt("gguf_context_length", v).apply() }

    fun getGgufGpuLayers(context: Context) = prefs(context).getInt("gguf_gpu_layers", 0)
    fun setGgufGpuLayers(context: Context, v: Int) { prefs(context).edit().putInt("gguf_gpu_layers", v).apply() }

    fun getGgufThreads(context: Context) = prefs(context).getInt("gguf_threads", 4)
    fun setGgufThreads(context: Context, v: Int) { prefs(context).edit().putInt("gguf_threads", v).apply() }

    fun getLitertTemperature(context: Context) = prefs(context).getFloat("litert_temp", 0.8f)
    fun setLitertTemperature(context: Context, v: Float) { prefs(context).edit().putFloat("litert_temp", v).apply() }

    fun getLitertTopP(context: Context) = prefs(context).getFloat("litert_top_p", 0.95f)
    fun setLitertTopP(context: Context, v: Float) { prefs(context).edit().putFloat("litert_top_p", v).apply() }

    fun getLitertTopK(context: Context) = prefs(context).getInt("litert_top_k", 40)
    fun setLitertTopK(context: Context, v: Int) { prefs(context).edit().putInt("litert_top_k", v).apply() }

    fun isLitertGpuEnabled(context: Context) = prefs(context).getBoolean("litert_gpu", false)
    fun setLitertGpuEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("litert_gpu", v).apply() }

    fun isGgufBatterySaverEnabled(context: Context) = prefs(context).getBoolean("gguf_battery_saver", false)
    fun setGgufBatterySaverEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("gguf_battery_saver", v).apply() }
    fun getGgufBatterySaverThreshold(context: Context) = prefs(context).getInt("gguf_battery_saver_threshold", 20)
    fun setGgufBatterySaverThreshold(context: Context, v: Int) { prefs(context).edit().putInt("gguf_battery_saver_threshold", v).apply() }

    fun isLitertBatterySaverEnabled(context: Context) = prefs(context).getBoolean("litert_battery_saver", false)
    fun setLitertBatterySaverEnabled(context: Context, v: Boolean) { prefs(context).edit().putBoolean("litert_battery_saver", v).apply() }
    fun getLitertBatterySaverThreshold(context: Context) = prefs(context).getInt("litert_battery_saver_threshold", 20)
    fun setLitertBatterySaverThreshold(context: Context, v: Int) { prefs(context).edit().putInt("litert_battery_saver_threshold", v).apply() }
}
