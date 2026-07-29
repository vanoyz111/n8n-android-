package com.vano.n8nmobile.localai

import com.llamatik.library.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalModelRuntime {
    @Volatile private var loadedPath: String? = null

    suspend fun ensureLoaded(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        if (loadedPath == modelPath) return@withContext true
        val ok = LlamaBridge.initGenerateModel(modelPath)
        if (ok) loadedPath = modelPath
        ok
    }

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        LlamaBridge.generate(prompt)
    }

    suspend fun applyChatTemplate(messages: List<Pair<String, String>>): String? = withContext(Dispatchers.IO) {
        LlamaBridge.applyChatTemplate(messages, addAssistantPrefix = true)
    }
}
