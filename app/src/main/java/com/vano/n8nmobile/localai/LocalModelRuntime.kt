package com.vano.n8nmobile.localai

import android.content.Context
import com.llamatik.library.platform.LlamaBridge
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalModelRuntime {
    @Volatile private var loadedSignature: String? = null

    suspend fun ensureLoaded(context: Context, modelPath: String): Boolean = withContext(Dispatchers.IO) {
        val temperature = LocalAiSettingsStore.getGgufTemperature(context)
        val topP = LocalAiSettingsStore.getGgufTopP(context)
        val topK = LocalAiSettingsStore.getGgufTopK(context)
        val maxTokens = LocalAiSettingsStore.getGgufMaxTokens(context)
        val repeatPenalty = LocalAiSettingsStore.getGgufRepeatPenalty(context)
        var contextLength = LocalAiSettingsStore.getGgufContextLength(context)
        var gpuLayers = LocalAiSettingsStore.getGgufGpuLayers(context)
        var threads = LocalAiSettingsStore.getGgufThreads(context)

        if (LocalAiSettingsStore.isGgufBatterySaverEnabled(context)) {
            val batteryPct = BatteryHelper.getBatteryPercent(context)
            val threshold = LocalAiSettingsStore.getGgufBatterySaverThreshold(context)
            if (batteryPct in 0..threshold) {
                AppLog.add("BATTERY_SAVER", "Baterai $batteryPct% <= $threshold%, kurangi beban AI Lokal GGUF")
                contextLength = contextLength.coerceAtMost(1024)
                gpuLayers = 0
                threads = threads.coerceAtMost(2)
            }
        }

        LlamaBridge.updateGenerateParams(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            topK = topK,
            repeatPenalty = repeatPenalty,
            contextLength = contextLength,
            numThreads = threads,
            useMmap = true,
            flashAttention = false,
            gpuLayers = gpuLayers,
            batchSize = 512
        )

        val signature = "$modelPath|$contextLength|$gpuLayers"
        if (loadedSignature == signature) return@withContext true

        val ok = LlamaBridge.initGenerateModel(modelPath)
        if (ok) loadedSignature = signature
        ok
    }

    suspend fun applyChatTemplate(messages: List<Pair<String, String>>): String? = withContext(Dispatchers.IO) {
        LlamaBridge.applyChatTemplate(messages, addAssistantPrefix = true)
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        LlamaBridge.generate(prompt)
    }
}
