package com.vano.n8nmobile.imagegen

import android.graphics.Bitmap
import com.llamatik.library.platform.StableDiffusionBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

object ImageGenRuntime {
    @Volatile private var loadedPath: String? = null

    suspend fun ensureLoaded(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        if (loadedPath == modelPath) return@withContext true
        val ok = StableDiffusionBridge.initModel(modelPath)
        if (ok) loadedPath = modelPath
        ok
    }

    suspend fun generate(
        prompt: String,
        negativePrompt: String,
        width: Int,
        height: Int,
        steps: Int,
        cfgScale: Float
    ): Bitmap? = withContext(Dispatchers.IO) {
        val rgba = StableDiffusionBridge.txt2img(
            prompt = prompt,
            negativePrompt = negativePrompt.ifBlank { null },
            width = width,
            height = height,
            steps = steps,
            cfgScale = cfgScale
        )
        if (rgba.isEmpty()) return@withContext null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(rgba))
        bitmap
    }
}
