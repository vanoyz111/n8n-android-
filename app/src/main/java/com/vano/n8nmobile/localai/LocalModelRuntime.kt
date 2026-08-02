package com.vano.n8nmobile.localai

import android.content.Context
import com.llamatik.library.platform.LlamaBridge
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
        val contextLength = LocalAiSettingsStore.getGgufContextLength(context)
        val gpuLayers = LocalAiSettingsStore.getGgufGpuLayers(context)
        val threads = LocalAiSettingsStore.getGgufThreads(context)

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
            gpuLayers = gpuLayers
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
