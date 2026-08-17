package com.vano.n8nmobile.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vano.n8nmobile.chat.AiClient
import com.vano.n8nmobile.chat.ChatMessage
import com.vano.n8nmobile.logging.AppLog
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class LocalServerService : Service() {

    private var serverSocket: ServerSocket? = null
    private var serverThread: Thread? = null
    @Volatile private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        val port = LocalServerStore.getPort(applicationContext)
        running = true

        serverThread = Thread {
            try {
                val socket = ServerSocket(port)
                serverSocket = socket
                LocalServerStore.setRunning(applicationContext, true)
                AppLog.add("SERVER", "Server lokal jalan di port $port")

                while (running) {
                    try {
                        val client = socket.accept()
                        Thread { handleClient(client) }.start()
                    } catch (e: Exception) {
                        if (running) AppLog.add("SERVER_ERROR", "Accept error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                AppLog.add("SERVER_ERROR", "Gagal start server: ${e.message}")
                LocalServerStore.setRunning(applicationContext, false)
            }
        }
        serverThread?.start()
        return START_STICKY
    }

    private fun handleClient(client: Socket) {
        try {
            client.use { sock ->
                val input = sock.getInputStream()
                val requestLine = readHttpLine(input)
                if (requestLine.isBlank()) return
                val parts = requestLine.split(" ")
                if (parts.size < 2) return
                val method = parts[0]
                val path = parts[1].substringBefore("?")

                val headers = mutableMapOf<String, String>()
                while (true) {
                    val line = readHttpLine(input)
                    if (line.isEmpty()) break
                    val idx = line.indexOf(":")
                    if (idx > 0) {
                        headers[line.substring(0, idx).trim().lowercase()] = line.substring(idx + 1).trim()
                    }
                }

                val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
                val bodyBytes = ByteArray(contentLength)
                var readTotal = 0
                while (readTotal < contentLength) {
                    val readCount = input.read(bodyBytes, readTotal, contentLength - readTotal)
                    if (readCount == -1) break
                    readTotal += readCount
                }
                val body = String(bodyBytes, Charsets.UTF_8)

                val output = sock.getOutputStream()
                when {
                    method == "POST" && path.startsWith("/v1/chat/completions") -> {
                        handleChatCompletions(headers, body, output)
                    }
                    method == "GET" && path.startsWith("/v1/models") -> {
                        val responseBody = JSONObject().apply {
                            put("object", "list")
                            put("data", JSONArray().put(JSONObject().apply {
                                put("id", "aiwa")
                                put("object", "model")
                            }))
                        }.toString()
                        writeHttpResponse(output, 200, responseBody)
                    }
                    else -> {
                        writeHttpResponse(output, 404, JSONObject().put("error", "Not found").toString())
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.add("SERVER_ERROR", "Client error: ${e.message}")
        }
    }

    private fun handleChatCompletions(headers: Map<String, String>, body: String, output: OutputStream) {
        val expectedKey = LocalServerStore.getApiKey(applicationContext)
        if (expectedKey.isNotBlank()) {
            val authHeader = headers["authorization"] ?: ""
            val providedKey = authHeader.removePrefix("Bearer ").trim()
            if (providedKey != expectedKey) {
                writeHttpResponse(output, 401, JSONObject().put("error", "Unauthorized").toString())
                return
            }
        }

        val history = try {
            val json = JSONObject(body)
            val messagesArray = json.getJSONArray("messages")
            (0 until messagesArray.length()).mapNotNull { i ->
                val m = messagesArray.getJSONObject(i)
                val role = m.optString("role", "user")
                val content = m.optString("content", "")
                if (role == "system") null else ChatMessage(role = if (role == "user") "user" else "ai", text = content)
            }
        } catch (e: Exception) {
            writeHttpResponse(output, 400, JSONObject().put("error", "Invalid request body").toString())
            return
        }

        AppLog.add("SERVER", "Request masuk dari client lokal (${history.size} pesan)")
        val reply = runBlocking { AiClient.sendMessage(applicationContext, history) }

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

        writeHttpResponse(output, 200, responseJson)
    }

    private fun readHttpLine(input: InputStream): String {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (b == '\n'.code) {
                if (sb.isNotEmpty() && sb.last() == '\r') sb.deleteCharAt(sb.length - 1)
                break
            }
            sb.append(b.toChar())
        }
        return sb.toString()
    }

    private fun writeHttpResponse(output: OutputStream, code: Int, body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        val statusText = when (code) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val header = "HTTP/1.1 $code $statusText\r\n" +
            "Content-Type: application/json\r\n" +
            "Content-Length: ${bytes.size}\r\n" +
            "Connection: close\r\n\r\n"
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(bytes)
        output.flush()
    }

    override fun onDestroy() {
        running = false
        try { serverSocket?.close() } catch (e: Exception) { }
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
