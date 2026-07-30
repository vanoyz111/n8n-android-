package com.vano.n8nmobile.localai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LiteRtRuntime {
    @Volatile private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null
    @Volatile private var loadedPath: String? = null

    suspend fun ensureLoaded(context: Context, modelPath: String, systemPrompt: String): Boolean =
        withContext(Dispatchers.IO) {
            if (loadedPath == modelPath && conversation != null) return@withContext true
            try {
                conversation?.close()
                engine?.close()

                val engineConfig = EngineConfig(
                    modelPath = modelPath,
                    backend = Backend.CPU(),
                    cacheDir = context.cacheDir.path
                )
                val newEngine = Engine(engineConfig)
                newEngine.initialize()

                val conversationConfig = if (systemPrompt.isNotBlank()) {
                    ConversationConfig(systemInstruction = Contents.of(systemPrompt))
                } else {
                    ConversationConfig()
                }
                val newConversation = newEngine.createConversation(conversationConfig)

                engine = newEngine
                conversation = newConversation
                loadedPath = modelPath
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val conv = conversation ?: throw IllegalStateException("Model LiteRT belum dimuat")
        conv.sendMessage(prompt).toString()
    }
}
