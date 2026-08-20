package com.agent.app.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progressPercent: Int, val downloadSpeed: String, val bytesDownloaded: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val file: File) : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloader(private val context: Context) {

    companion object {
        const val MODEL_URL = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf"
        const val MODEL_FILENAME = "qwen2.5-3b-instruct-q4_k_m.gguf"
    }

    fun getModelFile(): File {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()
        return File(modelsDir, MODEL_FILENAME)
    }

    fun isModelDownloaded(): Boolean {
        val file = getModelFile()
        return file.exists() && file.length() > 100_000_000L // >100MB
    }

    fun deleteModel(): Boolean {
        val file = getModelFile()
        return if (file.exists()) file.delete() else false
    }

    fun downloadModel(): Flow<DownloadState> = flow {
        val destinationFile = getModelFile()
        val client = OkHttpClient.Builder().build()

        val request = Request.Builder().url(MODEL_URL).build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(DownloadState.Error("HTTP Download failed: ${response.code}"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(DownloadState.Error("Empty response body from HuggingFace."))
                return@flow
            }

            val totalBytes = body.contentLength()
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(destinationFile)

            val buffer = ByteArray(8192)
            var bytesDownloaded: Long = 0
            var bytesRead: Int
            var startTime = System.currentTimeMillis()
            var bytesSinceLastCheck: Long = 0

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesDownloaded += bytesRead
                bytesSinceLastCheck += bytesRead

                val currentTime = System.currentTimeMillis()
                val timeDiff = (currentTime - startTime) / 1000.0

                if (timeDiff >= 0.5) {
                    val speedMb = (bytesSinceLastCheck / (1024.0 * 1024.0)) / timeDiff
                    val speedStr = String.format("%.2f MB/s", speedMb)
                    val progress = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0

                    emit(DownloadState.Downloading(progress, speedStr, bytesDownloaded, totalBytes))

                    startTime = currentTime
                    bytesSinceLastCheck = 0
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            emit(DownloadState.Completed(destinationFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Download exception occurred."))
        }
    }.flowOn(Dispatchers.IO)
}
