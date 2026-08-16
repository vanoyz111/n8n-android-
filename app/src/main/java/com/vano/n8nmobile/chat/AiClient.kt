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

    private fun effectiveSystemPrompt(context: Context, settings: SettingsStore): String {
        val base = settings.systemPrompt
        return if (ChatModeStore.isThinkingEnabled(context)) {
            if (base.isBlank()) THINKING_INSTRUCTION else "$base\n\n$THINKING_INSTRUCTION"
        } else {
            base
        }
    }

    private fun compressHistory(history: List<ChatMessage>, enabled: Boolean): List<ChatMessage> {
        if (!enabled || history.size <= 4) return history
        val cutoff = history.size - 4
        return history.mapIndexed { index, msg ->
            if (index < cutoff && msg.text.length > 200) {
                msg.copy(text = msg.text.take(200) + "... [dipotong buat hemat token]")
            } else msg
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
                val r = withContext(Dispatchers.IO) { callProfileWithKeyRotation(p, history, prompt) }
                if (!isFailureMessage(r)) return r
            }
        }

        AppLog.add("TIER_FALLBACK", "Semua provider online gagal, coba AI Lokal...")
        val ggufPath = LocalModelStore.getDownloadedModelPath(context)
        val litertPath = LiteRtModelStore.getDownloadedModelPath(context)
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
                val litertPath = LiteRtModelStore.getDownloadedModelPath(context)
                    ?: return "Model LiteRT belum didownload. Buka Settings > AI Lokal."
                withContext(Dispatchers.IO) { callLocalLiteRt(context, history, settings, litertPath) }
            }
            mode == "online" -> callPrimaryProvider(context, history, settings)
            mode.startsWith("profile:") -> {
                val profileId = mode.removePrefix("profile:")
                val profile = AiProfileStore.getProfile(context, profileId)
                    ?: return "Provider gak ketemu, mungkin udah dihapus."
                withContext(Dispatchers.IO) { callProfileWithKeyRotation(profile, history, effectiveSystemPrompt(context, settings)) }
            }
            else -> sendMessage(context, historyRaw)
        }
    }

    private fun callProfileWithKeyRotation(profile: AiProfile, history: List<ChatMessage>, systemPrompt: String): String {
        if (profile.apiKeys.isEmpty()) {
            return callOpenAiCompatibleRaw(profile.baseUrl, "", profile.model, history, systemPrompt)
        }
        var lastError = ""
        for (key in profile.apiKeys) {
            val result = callOpenAiCompatibleRaw(profile.baseUrl, key, profile.model, history, systemPrompt)
            if (!isFailureMessage(result)) return result
            lastError = result
            AppLog.add("AI_ROTATION", "Key gagal buat ${profile.name}, coba key berikutnya...")
        }
        return "Semua API key buat ${profile.name} gagal. Terakhir: $lastError"
    }

    private suspend fun callPrimaryProvider(
        context: Context,
        history: List<ChatMessage>,
        settings: SettingsStore
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val prompt = effectiveSystemPrompt(context, settings)
                if (settings.aiProvider == "openai_compatible") {
                    callOpenAiCompatible(settings, history, prompt)
                } else {
                    callGemini(settings, history, prompt)
                }
            } catch (e: Exception) {
                AppLog.add("AI_ERROR", e.message ?: "Unknown error")
                "Terjadi error: ${e.message}"
            }
        }
    }

    private suspend fun callLocalLlamatik(
        context: Context,
        history: List<ChatMessage>,
        settings: SettingsStore
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val modelPath = LocalModelStore.getDownloadedModelPath(context)
                    ?: return@withContext "Model lokal belum didownload. Buka Settings > AI Lokal."

                val loaded = LocalModelRuntime.ensureLoaded(context, modelPath)
                if (!loaded) return@withContext "Gagal memuat model lokal."

                val prompt = effectiveSystemPrompt(context, settings)
                val messages = mutableListOf<Pair<String, String>>()
                if (prompt.isNotBlank()) {
                    messages.add("system" to prompt)
                }
                history.forEach { msg ->
                    messages.add((if (msg.role == "user") "user" else "assistant") to msg.text)
                }

                val finalPrompt = LocalModelRuntime.applyChatTemplate(messages)
                    ?: history.lastOrNull { it.role == "user" }?.text
                    ?: ""

                val result = LocalModelRuntime.generate(finalPrompt)
                AppLog.add("CHAT", "AI Lokal (GGUF) merespons (${result.length} karakter)")
                result
            } catch (e: Exception) {
                AppLog.add("LOCAL_AI_ERROR", e.message ?: "unknown")
                "Gagal menjalankan AI lokal: ${e.message}"
            }
        }
    }

    private suspend fun callLocalLiteRt(
        context: Context,
        history: List<ChatMessage>,
        settings: SettingsStore,
        modelPath: String
    ): String {
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

    private fun callGemini(settings: SettingsStore, history: List<ChatMessage>, systemPrompt: String): String {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) return "API key Gemini belum diisi. Buka Settings buat masukin API key."
        val model = settings.geminiModel.ifBlank { "gemini-flash-latest" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val contents = JSONArray()
        history.forEachIndexed { index, msg ->
            val role = if (msg.role == "user") "user" else "model"
            val parts = JSONArray()
            val isLastMessage = index == history.lastIndex

            var textForTurn = msg.text
            if (msg.attachmentName != null) {
                textForTurn = if (textForTurn.isBlank()) "[Lampiran file: ${msg.attachmentName}]"
                else "$textForTurn\n[Lampiran file: ${msg.attachmentName}]"
            }

            if (msg.imageBase64 != null) {
                if (isLastMessage) {
                    parts.put(JSONObject().apply {
                        put("inline_data", JSONObject().apply {
                            put("mime_type", msg.imageMimeType ?: "image/jpeg")
                            put("data", msg.imageBase64)
                        })
                    })
                    if (textForTurn.isNotBlank()) parts.put(JSONObject().apply { put("text", textForTurn) })
                } else {
                    val placeholder = if (textForTurn.isNotBlank()) textForTurn else "[gambar terlampir]"
                    parts.put(JSONObject().apply { put("text", placeholder) })
                }
            } else if (textForTurn.isNotBlank()) {
                parts.put(JSONObject().apply { put("text", textForTurn) })
            }

            if (parts.length() > 0) {
                contents.put(JSONObject().apply {
                    put("role", role)
                    put("parts", parts)
                })
            }
        }

        val bodyJson = JSONObject().apply {
            put("contents", contents)
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

    private fun callOpenAiCompatibleRaw(
        baseUrl: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        systemPrompt: String
    ): String {
        val trimmedBase = baseUrl.trimEnd('/')
        if (trimmedBase.isBlank()) return "Base URL AI belum diisi. Buka Settings buat masukin URL-nya."
        val url = URL("$trimmedBase/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply { put("role", "system"); put("content", systemPrompt) })
        }
        history.forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            var content = msg.text
            if (msg.attachmentName != null) {
                content = if (content.isBlank()) "[Lampiran file: ${msg.attachmentName}]"
                else "$content\n[Lampiran file: ${msg.attachmentName}]"
            }
            if (msg.imageBase64 != null && content.isBlank()) {
                content = "[gambar terlampir - provider ini belum mendukung analisis gambar]"
            }
            messages.put(JSONObject().apply { put("role", role); put("content", content) })
        }

        val bodyJson = JSONObject().apply {
            put("model", model.ifBlank { "default" })
            put("messages", messages)
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
        val text = json.optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")

        AppLog.add("CHAT", "AI merespons (${text?.length ?: 0} karakter)")
        return text ?: "AI gak ngasih jawaban (response kosong)."
    }

    private fun callOpenAiCompatible(settings: SettingsStore, history: List<ChatMessage>, systemPrompt: String): String =
        callOpenAiCompatibleRaw(settings.customBaseUrl, settings.customApiKey, settings.customModel, history, systemPrompt)
}
