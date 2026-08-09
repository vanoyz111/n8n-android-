package com.vano.n8nmobile.localai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LiteRtRuntime {
    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    @Volatile private var loadedSignature: String? = null

    suspend fun ensureLoaded(context: Context, modelPath: String, systemPrompt: String): Boolean =
        withContext(Dispatchers.IO) {
            val useGpuPreference = LocalAiSettingsStore.isLitertGpuEnabled(context)
            val temperature = LocalAiSettingsStore.getLitertTemperature(context)
            val topP = LocalAiSettingsStore.getLitertTopP(context)
            val topK = LocalAiSettingsStore.getLitertTopK(context)
            val signature = "$modelPath|$useGpuPreference|$temperature|$topP|$topK|$systemPrompt"

            if (loadedSignature == signature && conversation != null) return@withContext true

            val samplerConfig = SamplerConfig(topK = topK, topP = topP.toDouble(), temperature = temperature.toDouble())
            val conversationConfig = if (systemPrompt.isNotBlank()) {
                ConversationConfig(systemInstruction = Contents.of(systemPrompt), samplerConfig = samplerConfig)
            } else {
                ConversationConfig(samplerConfig = samplerConfig)
            }

            fun tryLoad(useGpu: Boolean): Boolean {
                return try {
                    AppLog.add("LITERT", "Mulai load model: $modelPath (GPU=$useGpu)")
                    conversation?.close()
                    engine?.close()

                    val engineConfig = EngineConfig(
                        modelPath = modelPath,
                        backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                        cacheDir = context.cacheDir.path
                    )
                    val newEngine = Engine(engineConfig)
                    newEngine.initialize()
                    val newConversation = newEngine.createConversation(conversationConfig)

                    engine = newEngine
                    conversation = newConversation
                    loadedSignature = signature
                    AppLog.add("LITERT", "Model berhasil dimuat (GPU=$useGpu)")
                    true
                } catch (e: Throwable) {
                    AppLog.add("LITERT_ERROR", "${e.javaClass.simpleName}: ${e.message}")
                    false
                }
            }

            var ok = tryLoad(useGpuPreference)
            if (!ok && useGpuPreference) {
                AppLog.add("LITERT", "GPU gagal, coba ulang pakai CPU...")
                ok = tryLoad(false)
            }
            ok
        }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val conv = conversation ?: throw IllegalStateException("Model LiteRT belum dimuat")
        conv.sendMessage(prompt).toString()
    }
}
