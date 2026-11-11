package io.github.xororz.localdream.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object LogCapture {
    private const val TAG = "LogCapture"
    private const val MAX_BUFFER_BYTES = 2_000_000
    private const val TRIM_KEEP_BYTES = 1_000_000

    private val lock = Any()
    private val buffer = StringBuilder()
    private var captureProcess: java.lang.Process? = null
    private var captureScope: CoroutineScope? = null
    private var captureJob: Job? = null

    val lastCapturedLogs = mutableStateOf<String?>(null)

    fun start() {
        synchronized(lock) {
            stopInternalLocked()
            buffer.clear()
            try {
                Runtime.getRuntime().exec(arrayOf("logcat", "-c")).waitFor()
            } catch (e: Exception) {
                Log.w(TAG, "logcat -c failed", e)
            }
            try {
                val pid = android.os.Process.myPid()
                val proc = Runtime.getRuntime().exec(
                    arrayOf("logcat", "--pid=$pid", "-v", "threadtime"),
                )
                captureProcess = proc
                val scope = CoroutineScope(Dispatchers.IO)
                captureScope = scope
                captureJob = scope.launch {
                    try {
                        BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                            var line: String? = null
                            while (isActive && reader.readLine().also { line = it } != null) {
                                val current = line ?: continue
                                synchronized(lock) {
                                    buffer.append(current).append('\n')
                                    if (buffer.length > MAX_BUFFER_BYTES) {
                                        val tail = buffer.substring(buffer.length - TRIM_KEEP_BYTES)
                                        buffer.setLength(0)
                                        buffer.append(tail)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "log read loop ended", e)
                    }
                }
                Log.i(TAG, "log capture started for pid=$pid")
            } catch (e: Exception) {
                Log.e(TAG, "failed to start logcat", e)
            }
        }
    }

    fun stopAndPublish() {
        val captured: String
        synchronized(lock) {
            stopInternalLocked()
            captured = buffer.toString()
        }
        lastCapturedLogs.value = captured
        Log.i(TAG, "log capture stopped, ${captured.length} chars")
    }

    suspend fun exportCurrentLogs(context: Context): String = withContext(Dispatchers.IO) {
        val pid = android.os.Process.myPid()
        val process = Runtime.getRuntime().exec(
            arrayOf("logcat", "-d", "--pid=$pid", "-v", "threadtime"),
        )
        val logs = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        if (exitCode != 0 && logs.isBlank()) {
            throw java.io.IOException("logcat exited with code $exitCode")
        }

        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
        val filename = "local_dream_log_$timestamp.log"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/LocalDream",
                )
            }
            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                values,
            ) ?: throw java.io.IOException("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(logs.toByteArray(Charsets.UTF_8))
            } ?: throw java.io.IOException("openOutputStream failed")
            "Downloads/LocalDream/$filename"
        } else {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "LocalDream",
            )
            if (!dir.exists() && !dir.mkdirs()) {
                throw java.io.IOException("Could not create Downloads/LocalDream")
            }
            val file = File(dir, filename)
            FileOutputStream(file).use { it.write(logs.toByteArray(Charsets.UTF_8)) }
            file.absolutePath
        }
    }

    fun consume() {
        lastCapturedLogs.value = null
    }

    private fun stopInternalLocked() {
        try {
            captureProcess?.destroy()
        } catch (_: Exception) {
        }
        captureProcess = null
        try {
            captureJob?.cancel()
        } catch (_: Exception) {
        }
        captureJob = null
        try {
            captureScope?.cancel()
        } catch (_: Exception) {
        }
        captureScope = null
    }
}
