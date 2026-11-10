package com.example.imagegenerate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ModelDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    suspend fun download(
        url: String,
        destination: File,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        require(url.startsWith("https://")) { "Use uma URL HTTPS para baixar o modelo." }
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Falha HTTP: ${response.code}")
            val body = response.body ?: throw IOException("Resposta sem conteúdo.")
            destination.parentFile?.mkdirs()
            val temporary = File(destination.parentFile, destination.name + ".part")
            try {
                body.byteStream().use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 8)
                        var downloaded = 0L
                        var read: Int
                        while (input.read(buffer).also { read = it } >= 0) {
                            if (read == 0) continue
                            output.write(buffer, 0, read)
                            downloaded += read
                            val total = body.contentLength()
                            if (total > 0L) onProgress(downloaded.toFloat() / total)
                        }
                    }
                }
                if (!temporary.renameTo(destination)) {
                    temporary.copyTo(destination, overwrite = true)
                    temporary.delete()
                }
                onProgress(1f)
                destination
            } catch (error: Exception) {
                temporary.delete()
                throw error
            }
        }
    }
}
