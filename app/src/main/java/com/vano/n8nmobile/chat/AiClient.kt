package com.vano.n8nmobile.chat

import android.content.Context
import com.vano.n8nmobile.logging.AppLog
import com.vano.n8nmobile.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AiClient {

    suspend fun sendMessage(context: Context, history: List<ChatMessage>): String {
        val settings = SettingsStore(context)
        return withContext(Dispatchers.IO) {
            try {
                if (settings.aiProvider == "openai_compatible") {
                    callOpenAiCompatible(settings, history)
                } else {
                    callGemini(settings, history)
                }
            } catch (e: Exception) {
                AppLog.add("AI_ERROR", e.message ?: "Unknown error")
                "Terjadi error: ${e.message}"
            }
        }
    }

    private fun callGemini(settings: SettingsStore, history: List<ChatMessage>): String {
        val apiKey = settings.geminiApiKey
        if (apiKey.isBlank()) return "API key Gemini belum diisi. Buka Settings buat masukin API key."
        val model = settings.geminiModel.ifBlank { "gemini-flash-latest" }
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 30000

        val contents = JSONArray()
        history.forEach { msg ->
            val role = if (msg.role == "user") "user" else "model"
            contents.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().apply { put("text", msg.text) }))
                }
            )
        }

        val bodyJson = JSONObject().apply {
            put("contents", contents)
            if (settings.systemPrompt.isNotBlank()) {
                put(
                    "systemInstruction",
                    JSONObject().apply {
                        put("parts", JSONArray().put(JSONObject().apply { put("text", settings.systemPrompt) }))
                    }
                )
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
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text")

        AppLog.add("CHAT", "Gemini merespons (${text?.length ?: 0} karakter)")
        return text ?: "Gemini gak ngasih jawaban (response kosong)."
    }

    private fun callOpenAiCompatible(settings: SettingsStore, history: List<ChatMessage>): String {
        val baseUrl = settings.customBaseUrl.trimEnd('/')
        if (baseUrl.isBlank()) return "Base URL AI belum diisi. Buka Settings buat masukin URL-nya."
        val url = URL("$baseUrl/v1/chat/completions")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        if (settings.customApiKey.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer ${settings.customApiKey}")
        }
        connection.doOutput = true
        connection.connectTimeout = 20000
        connection.readTimeout = 60000

        val messages = JSONArray()
        if (settings.systemPrompt.isNotBlank()) {
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", settings.systemPrompt)
            })
        }
        history.forEach { msg ->
            val role = if (msg.role == "user") "user" else "assistant"
            messages.put(JSONObject().apply {
                put("role", role)
                put("content", msg.text)
            })
        }

        val bodyJson = JSONObject().apply {
            put("model", settings.customModel.ifBlank { "default" })
            put("messages", messages)
        }
        connection.outputStream.use { it.write(bodyJson.toString().toByteArray()) }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val responseText = stream?.bufferedReader()?.use { it.readText() } ?: ""
        connection.disconnect()

        if (code !in 200..299) {
            AppLog.add("AI_LOCAL_ERROR", "HTTP $code: ${responseText.take(200)}")
            return "Gagal manggil AI (HTTP $code). Cek server/URL-nya."
        }

        val json = JSONObject(responseText)
        val text = json.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content")

        AppLog.add("CHAT", "AI merespons (${text?.length ?: 0} karakter)")
        return text ?: "AI gak ngasih jawaban (response kosong)."
    }
}
