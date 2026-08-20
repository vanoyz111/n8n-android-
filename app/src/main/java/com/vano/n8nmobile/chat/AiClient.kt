package com.vano.n8nmobile.chat

import android.content.Context
import com.vano.n8nmobile.localai.LiteRtModelStore
import com.vano.n8nmobile.localai.LiteRtRuntime
import com.vano.n8nmobile.localai.LocalModelRuntime
import com.vano.n8nmobile.localai.LocalModelStore
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.settings.AiProfile
import com.vano.n8nmobile.settings.AiProfileStore
import com.vano.n8nmobile.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {

    private val errorPrefixes = listOf(
        "Terjadi error", "Gagal manggil Gemini", "API key Gemini belum diisi",
        "Gemini gak ngasih jawaban", "Base URL AI belum diisi", "Gagal manggil AI",
        "AI gak ngasih jawaban", "Model lokal belum didownload", "Gagal memuat model lokal",
        "Gagal menjalankan AI lokal", "Model LiteRT belum didownload", "Provider gak ketemu",
        "Semua API key"
    )

    private const val THINKING_INSTRUCTION =
        "Sebelum menjawab, tulis proses berpikirmu secara singkat di antara tag <thinking> dan </thinking>, " +
            "lalu lanjutkan dengan jawaban akhir di luar tag itu."

    fun isFailureMessage(text: String): Boolean = errorPrefixes.any { text.startsWith(it) }

    suspend fun testConnection(providerType: String, baseUrl: String, apiKey: String, model: String): Boolean {
        return withContext(Dispatchers.IO) {
            val testMsg = listOf(ChatMessage("user", "ping"))
            val result = if (providerType == "gemini") {
                callGeminiRaw(apiKey, model.ifBlank { "gemini-flash-latest" }, testMsg, "")
            } else {
                callOpenAiCompatibleRaw(baseUrl, apiKey, model, testMsg, "")
            }
            !isFailureMessage(result)
        }
    }

    private fun effectiveSystemPrompt(context: Context, settings: SettingsStore): String {
        val base = settings.systemPrompt
        return if (ChatModeStore.isThinkingEnabled(context)) {
            if (base.isBlank()) THINKING_INSTRUCTION else "$base\n\n$THINKING_INSTRUCTION"
        } else base
    }

    private fun compressHistory(history: List<ChatMessage>, enabled: Boolean): List<ChatMessage> {
        if (!enabled || history.size <= 4) return history
        val cutoff = history.size - 4
        return history.mapIndexed { index, msg ->
            if (index < cutoff && msg.text.length > 200) msg.copy(text = msg.text.take(200) + "... [dipotong buat hemat token]")
            else msg
        }
    }

    suspend fun sendMessage(context: Context, historyRaw: List<ChatMessage>): String {
        val settings = SettingsStore(context)
        val history = compressHistory(historyRaw, settings.tokenSaverEnabled)
        val prompt = effectiveSystemPrompt(context, settings)
        val profiles = AiProfileStore.getProfiles(context)

        val primaryResult = callPrimaryProvider(context, history, settings)
        if (!isFailureMessage(primaryResult)) return primaryResult

        for (tierNum in 1..3) {
            val tierProfiles = profiles.filter { it.tier == tierNum }
            if (tierProfiles.isEmpty()) continue
            AppLog.add("TIER_FALLBACK", "Coba provider Tier $tierNum...")
            for (p in tierProfiles) {
                val r = withContext(Dispatchers.IO) { callProfileWithKeyRotation(context, p, history, prompt) }
                if (!isFailureMessage(r)) return r
            }
        }

        AppLog.add("TIER_FALLBACK", "Semua provider online gagal, coba AI Lokal...")
        val ggufPath = LocalModelStore.getActiveModelPath(context)
        val litertPath = LiteRtModelStore.getActiveModelPath(context)
        return when {
            ggufPath != null -> callLocalLlamatik(context, history, settings)
            litertPath != null -> callLocalLiteRt(context, history, settings, litertPath)
            else -> primaryResult
        }
    }

    suspend fun sendMessageWithMode(context: Context, historyRaw: List<ChatMessage>, mode: String): String {
        val settings = SettingsStore(context)
        val history = compressHistory(historyRaw, settings.tokenSaverEnabled)
        return when {
            mode == "local_gguf" -> withContext(Dispatchers.IO) { callLocalLlamatik(context, history, settings) }
            mode == "local_litert" -> {
                val litertPath = LiteRtModelStore.getActiveModelPath(context)
                    ?: return "Model LiteRT belum didownload. Buka Settings > AI Lokal."
                withContext(Dispatchers.IO) { callLocalLiteRt(context, history, settings, litertPath) }
            }
            mode == "online" -> callPrimaryProvider(context, history, settings)
            mode.startsWith("profile:") -> {
                val profileId = mode.removePrefix("profile:")
                val profile = AiProfileStore.getProfile(context, profileId) ?: return "Provider gak ketemu, mungkin udah dihapus."
                withContext(Dispatchers.IO) { callProfileWithKeyRotation(context, profile, history, effectiveSystemPrompt(context, settings)) }
            }
            else -> sendMessage(context, historyRaw)
        }
    }

    suspend fun sendMessageStreaming(context: Context, historyRaw: List<ChatMessage>, mode: String, onChunk: (String) -> Unit): String {
        val settings = SettingsStore(context)
        val history = compressHistory(historyRaw, settings.tokenSaverEnabled)
        val prompt = effectiveSystemPrompt(context, settings)
        return withContext(Dispatchers.IO) {
            try {
                when {
                    mode == "online" -> {
                        QuotaTracker.recordCall(context, if (settings.aiProvider == "openai_compatible") "custom_primary" else "gemini")
                        if (settings.aiProvider == "openai_compatible") {
                            callOpenAiCompatibleStreamingRaw(settings.customBaseUrl, settings.customApiKey, settings.customModel, history, prompt, onChunk)
                        } else {
                            callGeminiStreaming(settings, history, prompt, onChunk)
                        }
                    }
                    mode.startsWith("profile:") -> {
                        val profileId = mode.removePrefix("profile:")
                        val profile = AiProfileStore.getProfile(context, profileId) ?: return@withContext "Provider gak ketemu, mungkin udah dihapus."
                        QuotaTracker.recordCall(context, "profile:${profile.id}")
                        if (profile.apiKeys.isEmpty()) {
                            callOpenAiCompatibleStreamingRaw(profile.baseUrl, "", profile.model, history, prompt, onChunk)
                        } else {
                            var result = "Semua API key buat ${profile.name} gagal."
                            for (key in profile.apiKeys) {
                                result = callOpenAiCompatibleStreamingRaw(profile.baseUrl, key, profile.model, history, prompt, onChunk)
                                if (!isFailureMessage(result)) break
                            }
                            result
                        }
                    }
                    else -> sendMessageWithMode(context, historyRaw, mode)
                }
            } catch (e: Exception) {
                AppLog.add("AI_ERROR", e.message ?: "Unknown error")
                "Terjadi error: ${e.message}"
            }
        }
    }

    private fun callProfileWithKeyRotation(context: Context, profile: AiProfile, history: List<ChatMessage>, systemPrompt: String): String {
        QuotaTracker.recordCall(context, "profile:${profile.id}")
        if (profile.apiKeys.isEmpty()) return callOpenAiCompatibleRaw(profile.baseUrl, "", profile.model, history, systemPrompt)
        var lastError = ""
        for (key in profile.apiKeys) {
            val result = callOpenAiCompatibleRaw(profile.baseUrl, key, profile.model, history, systemPrompt)
            if (!isFailureMessage(result)) return result
            lastError = result
            AppLog.add("AI_ROTATION", "Key gagal buat ${profile.name}, coba key berikutnya...")
        }
        return "Semua API key buat ${profile.name} gagal. Terakhir: $lastError"
    }

    private suspend fun callPrimaryProvider(context: Context, history: List<ChatMessage>, settings: SettingsStore): String {
        return withContext(Dispatchers.IO) {
            try {
                QuotaTracker.recordCall(context, if (settings.aiProvider == "openai_compatible") "custom_primary" else "gemini")
                val prompt = effectiveSystemPrompt(context, settings)
                if (settings.aiProvider == "openai_compatible") callOpenAiCompatible(settings, history, prompt)
                else callGemini(settings, history, prompt)
            } catch (e: Exception) {
                AppLog.add("AI_ERROR", e.message ?: "Unknown error")
                "Terjadi error: ${e.message}"
            }
        }
    }

    private suspend fun callLocalLlamatik(context: Context, history: List<ChatMessage>, settings: SettingsStore): String {
        return withContext(Dispatchers.IO) {
            try {
                val modelPath = LocalModelStore.getActiveModelPath(context)
                    ?: return@withContext "Model lokal belum didownload. Buka Settings > AI Lokal."
                val loaded = LocalModelRuntime.ensureLoaded(context, modelPath)
                if (!loaded) return@withContext "Gagal memuat model lokal."
                val prompt = effectiveSystemPrompt(context, settings)
                val messages = mutableListOf<Pair<String, String>>()
                if (prompt.isNotBlank()) messages.add("system" to prompt)
                history.forEach { msg -> messages.add((if (msg.role == "user") "user" else "assistant") to msg.text) }
                val finalPrompt = LocalModelRuntime.applyChatTemplate(messages) ?: history.lastOrNull { it.role == "user" }?.text ?: ""
                val result = LocalModelRuntime.generate(finalPrompt)
                AppLog.add("CHAT", "AI Lokal (GGUF) merespons (${result.length} karakter)")
                result
            } catch (e: Exception) {
                AppLog.add("LOCAL_AI_ERROR", e.message ?: "unknown")
                "Gagal menjalankan AI lokal: ${e.message}"
            }
        }
    }

    private suspend fun callLocalLiteRt(context: Context, history: List<ChatMessage>, settings: SettingsStore, modelPath: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = effectiveSystemPrompt(context, settings)
                val loaded = LiteRtRuntime.ensureLoaded(context, modelPath, prompt)
                if (!loaded) return@withContext "Gagal memuat model lokal."
                val lastUserMessage = history.lastOrNull { it.role == "user" }?.text ?: ""
                val result = LiteRtRuntime.generate(lastUserMessage)
                AppLog.add("CHAT", "AI Lokal (LiteRT) merespons (${result.length} karakter)")
                result
            } catch (e: Exception) {
                AppLog.add("LOCAL_AI_ERROR", e.message ?: "unknown")
                "Gagal menjalankan AI lokal: ${e.message}"
            }
        }
    }

    private fun buildGeminiContents(history: List<ChatMessage>): JSONArray {
        val contents = JSONArray()
        history.forEachIndexed { index, msg ->
            val role = if (msg.role == "user") "user" else "model"
            val parts = JSONArray()
            val isLastMessage = index == history.lastIndex

            var textForTurn = msg.text
            if (msg.fileAttachmentNames.isNotEmpty()) {
                val note = msg.fileAttachmentNames.joinToString(", ") { "[Lampiran file: $it]" }
                textForTurn = if (textForTurn.isBlank()) note else "$textForTurn\n$note"
            }

            if (msg.imageAttachments.isNotEmpty()) {
                if (isLastMessage) {
                    msg.imageAttachments.forEach { img ->
                        parts.put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", img.mimeType); put("data", img.base64)
                            })
                        })
                    }
                    if (textForTurn.isNotBlank()) parts.put(JSONObject().apply { put("text", textForTurn) })
                } else {
                    val placeholder = if (textForTurn.isNotBlank()) textForTurn else "[${msg.imageAttachments.size} gambar terlampir]"
                    parts.put(JSONObject().apply { put("text", placeholder) })
                }
            } else if (textForTurn.isNotBlank()) {
                parts.put(JSONObject().apply { put("text", textForTurn) })
            }

            if (parts.length() > 0) contents.put(JSONObject().apply { put("role", role); put("parts", parts) })
        }
        return contents
    }

    private fun callGeminiRaw(apiKey: String, model: String, history: List<ChatMessage>, systemPrompt: String): String {
        if (apiKey.isBlank()) return "API key Gemini belum diisi. Buka Settings buat masukin API key."
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val bodyJson = JSONObject().apply {
            put("contents", buildGeminiContents(history))
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                })
            }
        }
        connection.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()

        if (code !in 200..299) {
            AppLog.add("GEMINI_ERROR", "HTTP $code: ${responseText.take(200)}")
            return "Gagal manggil Gemini (HTTP $code). Cek API key atau koneksi internet."
        }

        val json = JSONObject(responseText)
        val text = json.optJSONArray("candidates")
            ?.optJSONObject(0)?.optJSONObject("content")
            ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")

        AppLog.add("CHAT", "Gemini merespons (${text?.length ?: 0} karakter)")
        return text ?: "Gemini gak ngasih jawaban (response kosong)."
    }

    private fun callGemini(settings: SettingsStore, history: List<ChatMessage>, systemPrompt: String): String {
        val model = settings.geminiModel.ifBlank { "gemini-flash-latest" }
        return callGeminiRaw(settings.geminiApiKey, model, history, systemPrompt)
    }

    private fun callGeminiStreaming(settings: SettingsStore, history: List<ChatMessage>, systemPrompt: String, onChunk: (String) -> Unit): String {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) return "API key Gemini belum diisi. Buka Settings buat masukin API key."
        val model = settings.geminiModel.ifBlank { "gemini-flash-latest" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?alt=sse&key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val bodyJson = JSONObject().apply {
            put("contents", buildGeminiContents(history))
            if (systemPrompt.isNotBlank()) {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
                })
            }
        }
        connection.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

        val code = connection.responseCode
        if (code !in 200..299) {
            val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            AppLog.add("GEMINI_ERROR", "HTTP $code: ${errText.take(200)}")
            connection.disconnect()
            return "Gagal manggil Gemini (HTTP $code). Cek API key atau koneksi internet."
        }

        val fullText = StringBuilder()
        connection.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    if (jsonPart.isNotBlank() && jsonPart != "[DONE]") {
                        try {
                            val chunkJson = JSONObject(jsonPart)
                            val text = chunkJson.optJSONArray("candidates")
                                ?.optJSONObject(0)?.optJSONObject("content")
                                ?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                            if (!text.isNullOrEmpty()) { fullText.append(text); onChunk(fullText.toString()) }
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        connection.disconnect()
        AppLog.add("CHAT", "Gemini streaming selesai (${fullText.length} karakter)")
        return fullText.toString().ifBlank { "Gemini gak ngasih jawaban (response kosong)." }
    }

    private fun buildOpenAiMessages(history: List<ChatMessage>, systemPrompt: String): JSONArray {
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) messages.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        history.forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            var content = msg.text
            if (msg.fileAttachmentNames.isNotEmpty()) {
                val note = msg.fileAttachmentNames.joinToString(", ") { "[Lampiran file: $it]" }
                content = if (content.isBlank()) note else "$content\n$note"
            }
            if (msg.imageAttachments.isNotEmpty() && content.isBlank()) {
                content = "[${msg.imageAttachments.size} gambar terlampir - provider ini belum mendukung analisis gambar]"
            }
            messages.put(JSONObject().apply { put("role", role); put("content", content) })
        }
        return messages
    }

    private fun callOpenAiCompatibleRaw(baseUrl: String, apiKey: String, model: String, history: List<ChatMessage>, systemPrompt: String): String {
        val trimmedBase = baseUrl.trimEnd('/')
        if (trimmedBase.isBlank()) return "Base URL AI belum diisi. Buka Settings buat masukin URL-nya."
        val url = URL("$trimmedBase/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val bodyJson = JSONObject().apply {
            put("model", model.ifBlank { "default" })
            put("messages", buildOpenAiMessages(history, systemPrompt))
        }
        connection.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()

        if (code !in 200..299) {
            AppLog.add("AI_PROVIDER_ERROR", "HTTP $code: ${responseText.take(200)}")
            return "Gagal manggil AI (HTTP $code). Cek API key, model, atau URL-nya."
        }

        val json = JSONObject(responseText)
        val text = json.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
        AppLog.add("CHAT", "AI merespons (${text?.length ?: 0} karakter)")
        return text ?: "AI gak ngasih jawaban (response kosong)."
    }

    private fun callOpenAiCompatible(settings: SettingsStore, history: List<ChatMessage>, systemPrompt: String): String =
        callOpenAiCompatibleRaw(settings.customBaseUrl, settings.customApiKey, settings.customModel, history, systemPrompt)

    private fun callOpenAiCompatibleStreamingRaw(baseUrl: String, apiKey: String, model: String, history: List<ChatMessage>, systemPrompt: String, onChunk: (String) -> Unit): String {
        val trimmedBase = baseUrl.trimEnd('/')
        if (trimmedBase.isBlank()) return "Base URL AI belum diisi. Buka Settings buat masukin URL-nya."
        val url = URL("$trimmedBase/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val bodyJson = JSONObject().apply {
            put("model", model.ifBlank { "default" })
            put("messages", buildOpenAiMessages(history, systemPrompt))
            put("stream", true)
        }
        connection.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

        val code = connection.responseCode
        if (code !in 200..299) {
            val errText = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            AppLog.add("AI_PROVIDER_ERROR", "HTTP $code: ${errText.take(200)}")
            connection.disconnect()
            return "Gagal manggil AI (HTTP $code). Cek API key, model, atau URL-nya."
        }

        val fullText = StringBuilder()
        connection.inputStream.bufferedReader().use { reader ->
            reader.forEachLine { line ->
                if (line.startsWith("data: ")) {
                    val jsonPart = line.removePrefix("data: ").trim()
                    if (jsonPart.isNotBlank() && jsonPart != "[DONE]") {
                        try {
                            val chunkJson = JSONObject(jsonPart)
                            val delta = chunkJson.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("delta")?.optString("content")
                            if (!delta.isNullOrEmpty()) { fullText.append(delta); onChunk(fullText.toString()) }
                        } catch (e: Exception) { }
                    }
                }
            }
        }
        connection.disconnect()
        AppLog.add("CHAT", "AI streaming selesai (${fullText.length} karakter)")
        return fullText.toString().ifBlank { "AI gak ngasih jawaban (response kosong)." }
    }
}
