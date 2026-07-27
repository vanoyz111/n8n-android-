package com.vano.n8nmobile.engine

import com.vano.n8nmobile.model.WorkflowNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class HttpRequestExecutor : NodeExecutor {
    override suspend fun execute(node: WorkflowNode, input: List<Map<String, Any?>>): List<Map<String, Any?>> {
        val url = node.config["url"] ?: throw IllegalStateException("Node httpRequest butuh config 'url'")
        val method = node.config["method"]?.uppercase() ?: "GET"
        val items = if (input.isEmpty()) listOf(emptyMap()) else input

        return items.map { item ->
            val result = withContext(Dispatchers.IO) { performRequest(url, method) }
            item + result
        }
    }

    private fun performRequest(urlString: String, method: String): Map<String, Any?> {
        return try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            connection.disconnect()
            mapOf("httpStatus" to code, "httpBody" to body.take(300))
        } catch (e: Exception) {
            mapOf("httpStatus" to -1, "httpError" to (e.message ?: "unknown error"))
        }
    }
}
