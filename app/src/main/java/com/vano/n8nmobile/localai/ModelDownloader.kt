package com.vano.n8nmobile.localai

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object ModelDownloader {
    // dipakai lintas package: localai dan imagegen
    suspend fun download(
        context: Context,
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val destDir = context.getExternalFilesDir(null) ?: context.filesDir
        val destFile = File(destDir, fileName)
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.instanceFollowRedirects = true
        connection.connect()

        if (connection.responseCode !in 200..299) {
            connection.disconnect()
            throw IllegalStateException("Server balas HTTP ${connection.responseCode}")
        }

        val totalSize = connection.contentLengthLong
        var downloaded = 0L

        connection.inputStream.use { input ->
            FileOutputStream(destFile).use { output ->
                val buffer = ByteArray(65536)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    downloaded += bytesRead
                    if (totalSize > 0) {
                        onProgress(downloaded.toFloat() / totalSize.toFloat())
                    }
                }
            }
        }
        connection.disconnect()
        destFile
    }
}
