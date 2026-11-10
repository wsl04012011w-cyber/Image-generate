package com.example.imagegenerate

import android.graphics.Bitmap
import android.graphics.Color

enum class ComputeBackend(val nativeName: String) {
    CPU("cpu"),
    GPU("vulkan")
}

object NativeDiffusion {
    init { System.loadLibrary("imagegen") }
    external fun nativeIsAvailable(): Boolean
    external fun nativeGenerate(modelPath: String, prompt: String, steps: Int, backend: String): ByteArray?
}

class DiffusionEngine {
    fun isAvailable(): Boolean = NativeDiffusion.nativeIsAvailable()

    suspend fun generate(
        model: java.io.File,
        prompt: String,
        steps: Int,
        backend: ComputeBackend
    ): Bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        check(model.exists() && model.length() > 0L) { "Baixe um modelo antes de gerar." }
        check(isAvailable()) {
            "Engine nativa não incluída neste APK. Compile com -PincludeStableDiffusion=true."
        }
        val pixels = NativeDiffusion.nativeGenerate(model.absolutePath, prompt, steps, backend.nativeName)
            ?: throw IllegalStateException("A inferência falhou. Verifique o formato do modelo e a memória disponível.")
        val width = 512
        val height = 512
        check(pixels.size == width * height * 3) { "Formato de imagem retornado pelo engine inválido." }
        val argb = IntArray(width * height)
        for (i in argb.indices) {
            val offset = i * 3
            argb[i] = Color.rgb(
                pixels[offset].toInt() and 0xff,
                pixels[offset + 1].toInt() and 0xff,
                pixels[offset + 2].toInt() and 0xff
            )
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(argb, 0, width, 0, 0, width, height)
        }
    }
}
