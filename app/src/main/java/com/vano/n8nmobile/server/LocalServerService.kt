package com.vano.n8nmobile.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class LocalServerService : Service() {

    private var httpServer: HttpServer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val port = LocalServerStore.getPort(applicationContext)
        try {
            val server = HttpServer.create(InetSocketAddress("0.0.0.0", port), 0)
            server.createContext("/v1/chat/completions", ChatCompletionsHandler(applicationContext))
            server.createContext("/v1/models", ModelsHandler())
            server.executor = Executors.newFixedThreadPool(4)
            server.start()
            httpServer = server
            LocalServerStore.setRunning(applicationContext, true)
            AppLog.add("SERVER", "Server lokal jalan di port $port")
        } catch (e: Exception) {
            AppLog.add("SERVER_ERROR", "Gagal start server: ${e.message}")
            LocalServerStore.setRunning(applicationContext, false)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        httpServer?.stop(0)
        LocalServerStore.setRunning(applicationContext, false)
        AppLog.add("SERVER", "Server lokal dimatikan")
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Aiwa Local Server", NotificationManager.IMPORTANCE_LOW)
                )
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aiwa Local AI Server aktif")
            .setContentText("Bisa diakses device lain di jaringan yang sama")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "aiwa_local_server"
        private const val NOTIF_ID = 9001
    }
}

private class ModelsHandler : HttpHandler {
    override fun handle(exchange: HttpExchange) {
        val body = JSONObject().apply {
            put("object", "list")
            put("data", JSONArray().put(JSONObject().apply {
                put("id", "aiwa")
                put("object", "model")
            }))
        }.toString()
        sendJson(exchange, 200, body)
    }
}

private class ChatCompletionsHandler(private val context: Context) : HttpHandler {
    override fun handle(exchange: HttpExchange) {
        if (exchange.requestMethod != "POST") {
            sendJson(exchange, 405, JSONObject().put("error", "Method not allowed").toString())
            return
        }

        val expectedKey = LocalServerStore.getApiKey(context)
        if (expectedKey.isNotBlank()) {
            val authHeader = exchange.requestHeaders.getFirst("Authorization") ?: ""
            val providedKey = authHeader.removePrefix("Bearer ").trim()
            if (providedKey != expectedKey) {
                sendJson(exchange, 401, JSONObject().put("error", "Unauthorized").toString())
                return
            }
        }

        val requestBody = exchange.requestBody.bufferedReader().use { it.readText() }
        val history = try {
            val json = JSONObject(requestBody)
            val messagesArray = json.getJSONArray("messages")
            (0 until messagesArray.length()).mapNotNull { i ->
                val m = messagesArray.getJSONObject(i)
                val role = m.optString("role", "user")
                val content = m.optString("content", "")
                if (role == "system") null else ChatMessage(role = if (role == "user") "user" else "ai", text = content)
            }
        } catch (e: Exception) {
            sendJson(exchange, 400, JSONObject().put("error", "Invalid request body").toString())
            return
        }

        AppLog.add("SERVER", "Request masuk dari client lokal (${history.size} pesan)")

        val reply = runBlocking { AiClient.sendMessage(context, history) }

        val responseJson = JSONObject().apply {
            put("id", "aiwa-${System.currentTimeMillis()}")
            put("object", "chat.completion")
            put("choices", JSONArray().put(JSONObject().apply {
                put("index", 0)
                put("message", JSONObject().apply {
                    put("role", "assistant")
                    put("content", reply)
                })
                put("finish_reason", "stop")
            }))
        }.toString()

        sendJson(exchange, 200, responseJson)
    }
}

private fun sendJson(exchange: HttpExchange, code: Int, body: String) {
    val bytes = body.toByteArray()
    exchange.responseHeaders.add("Content-Type", "application/json")
    exchange.sendResponseHeaders(code, bytes.size.toLong())
    exchange.responseBody.use { it.write(bytes) }
}
