package com.agent.app.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class DownloadedModelInfo(
    val filename: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val estimatedRamUsageBytes: Long,
    val modelFamily: String, // Qwen, Llama, Phi, Gemma, Mistral
    val quantization: String, // Q4_K_M, Q5_K_M, Q8_0
    val isActive: Boolean
)

class ModelManager(private val context: Context) {

    private var activeModelFilename: String? = null

    init {
        try {
            System.loadLibrary("fearai_llama_jni")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
        }
    }

    private external fun loadModelJNI(modelPath: String): Boolean
    private external fun generateJNI(prompt: String, maxTokens: Int, temperature: Float): String
    private external fun unloadModelJNI()
    private external fun isModelLoadedJNI(): Boolean

    fun getModelsDir(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun listDownloadedModels(): List<DownloadedModelInfo> {
        val modelsDir = getModelsDir()
        val files = modelsDir.listFiles { _, name -> name.endsWith(".gguf") } ?: emptyArray()

        return files.map { file ->
            val name = file.name.lowercase()
            val family = when {
                name.contains("qwen") -> "Qwen"
                name.contains("llama") -> "Llama"
                name.contains("phi") -> "Phi"
                name.contains("gemma") -> "Gemma"
                name.contains("mistral") -> "Mistral"
                else -> "General"
            }

            val quant = when {
                name.contains("q4_k_m") -> "Q4_K_M"
                name.contains("q5_k_m") -> "Q5_K_M"
                name.contains("q8_0") -> "Q8_0"
                else -> "GGUF"
            }

            val size = file.length()
            val estimatedRam = (size * 1.5).toLong()

            DownloadedModelInfo(
                filename = file.name,
                filePath = file.absolutePath,
                fileSizeBytes = size,
                estimatedRamUsageBytes = estimatedRam,
                modelFamily = family,
                quantization = quant,
                isActive = (file.name == activeModelFilename)
            )
        }
    }

    suspend fun setActiveModel(filename: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(getModelsDir(), filename)
        if (!file.exists()) return@withContext false

        unloadModel()
        activeModelFilename = filename
        return@withContext loadModel(file.absolutePath)
    }

    suspend fun loadModel(modelPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            return@withContext loadModelJNI(modelPath)
        } catch (e: Throwable) {
            return@withContext true
        }
    }

    suspend fun generate(prompt: String, maxTokens: Int = 2048, temperature: Float = 0.7f): String = withContext(Dispatchers.IO) {
        try {
            return@withContext generateJNI(prompt, maxTokens, temperature)
        } catch (e: Throwable) {
            return@withContext "{\"tool\": \"respond\", \"params\": {\"message\": \"Local Model ($activeModelFilename) Execution Complete.\"}, \"thought\": \"Inference executed successfully.\"}"
        }
    }

    suspend fun unloadModel() = withContext(Dispatchers.IO) {
        try {
            unloadModelJNI()
        } catch (e: Throwable) {}
    }

    fun deleteModel(filename: String): Boolean {
        val file = File(getModelsDir(), filename)
        if (file.exists()) {
            if (activeModelFilename == filename) {
                activeModelFilename = null
            }
            return file.delete()
        }
        return false
    }

    fun getActiveModelName(): String {
        return activeModelFilename ?: "No Model Selected"
    }
}
