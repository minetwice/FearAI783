package com.agent.app.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile

enum class DownloadStatus {
    IDLE, DOWNLOADING, PAUSED, COMPLETED, FAILED, CANCELLED
}

data class DownloadProgressState(
    val status: DownloadStatus = DownloadStatus.IDLE,
    val modelId: String = "",
    val filename: String = "",
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Float = 0f,
    val etaSeconds: Long = 0L,
    val filePath: String = "",
    val errorMessage: String? = null
)

class ModelDownloadManager(private val context: Context) {

    private val _downloadState = MutableStateFlow(DownloadProgressState())
    val downloadState: StateFlow<DownloadProgressState> = _downloadState.asStateFlow()

    private var isCancelled = false
    private var isPaused = false

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun pauseDownload() {
        isPaused = true
        _downloadState.value = _downloadState.value.copy(status = DownloadStatus.PAUSED)
    }

    fun resumeDownload(modelId: String, filename: String) {
        isPaused = false
        startDownload(modelId, filename)
    }

    fun cancelDownload() {
        isCancelled = true
        _downloadState.value = DownloadProgressState(status = DownloadStatus.CANCELLED)
    }

    suspend fun startDownload(modelId: String, filename: String) = withContext(Dispatchers.IO) {
        isCancelled = false
        isPaused = false

        val downloadUrl = "https://huggingface.co/$modelId/resolve/main/$filename"
        val destinationFile = File(getModelsDir(), filename)

        var existingBytes = if (destinationFile.exists()) destinationFile.length() else 0L

        _downloadState.value = DownloadProgressState(
            status = DownloadStatus.DOWNLOADING,
            modelId = modelId,
            filename = filename,
            downloadedBytes = existingBytes,
            filePath = destinationFile.absolutePath
        )

        val client = OkHttpClient.Builder().build()
        val requestBuilder = Request.Builder().url(downloadUrl)

        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful && response.code != 206) {
                _downloadState.value = _downloadState.value.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "HTTP Error ${response.code}"
                )
                return@withContext
            }

            val body = response.body
            if (body == null) {
                _downloadState.value = _downloadState.value.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = "Empty HTTP response body."
                )
                return@withContext
            }

            val contentLength = body.contentLength()
            val totalBytes = if (response.code == 206) existingBytes + contentLength else contentLength

            val inputStream = body.byteStream()
            val outputStream = RandomAccessFile(destinationFile, "rw")
            outputStream.seek(existingBytes)

            val buffer = ByteArray(16384)
            var bytesRead: Int
            var currentDownloaded = existingBytes
            var startTime = System.currentTimeMillis()
            var bytesSinceCheck = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled) {
                    outputStream.close()
                    inputStream.close()
                    _downloadState.value = DownloadProgressState(status = DownloadStatus.CANCELLED)
                    return@withContext
                }

                if (isPaused) {
                    outputStream.close()
                    inputStream.close()
                    _downloadState.value = _downloadState.value.copy(status = DownloadStatus.PAUSED)
                    return@withContext
                }

                outputStream.write(buffer, 0, bytesRead)
                currentDownloaded += bytesRead
                bytesSinceCheck += bytesRead

                val now = System.currentTimeMillis()
                val diffSec = (now - startTime) / 1000.0f

                if (diffSec >= 0.5f) {
                    val speed = bytesSinceCheck / diffSec
                    val remainingBytes = totalBytes - currentDownloaded
                    val eta = if (speed > 0) (remainingBytes / speed).toLong() else 0L

                    _downloadState.value = _downloadState.value.copy(
                        status = DownloadStatus.DOWNLOADING,
                        downloadedBytes = currentDownloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSec = speed,
                        etaSeconds = eta
                    )

                    startTime = now
                    bytesSinceCheck = 0L
                }
            }

            outputStream.close()
            inputStream.close()

            _downloadState.value = _downloadState.value.copy(
                status = DownloadStatus.COMPLETED,
                downloadedBytes = totalBytes,
                totalBytes = totalBytes,
                speedBytesPerSec = 0f,
                etaSeconds = 0L
            )
        } catch (e: Exception) {
            _downloadState.value = _downloadState.value.copy(
                status = DownloadStatus.FAILED,
                errorMessage = e.message ?: "Download exception."
            )
        }
    }
}
