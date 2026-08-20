package com.agent.app.github

import android.util.Base64
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Random

class GitHubClient(
    private var token: String,
    private val owner: String,
    private val repo: String
) {
    private val api: GitHubApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(GitHubApi::class.java)
    }

    private fun getAuthHeader() = "Bearer $token"

    suspend fun validateToken(): Boolean {
        return try {
            val res = api.getUser(getAuthHeader())
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createNewBranch(baseBranch: String = "main"): String? {
        return try {
            val refRes = api.getBranchRef(getAuthHeader(), owner, repo, baseBranch)
            if (!refRes.isSuccessful) return null

            val map = refRes.body() ?: return null
            val obj = map["object"] as? Map<*, *> ?: return null
            val sha = obj["sha"] as? String ?: return null

            val randomSuffix = (1..6).map { ('a'..'z').random() }.joinToString("")
            val newBranchName = "agent-${System.currentTimeMillis()}-$randomSuffix"

            val createRes = api.createBranch(
                getAuthHeader(), owner, repo,
                CreateRefRequest(ref = "refs/heads/$newBranchName", sha = sha)
            )

            if (createRes.isSuccessful) newBranchName else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createFile(branch: String, path: String, contentText: String): Boolean {
        return try {
            val base64Content = Base64.encodeToString(contentText.toByteArray(), Base64.NO_WRAP)
            val payload = ContentPayload(
                message = "Add $path via FearAI Agent",
                content = base64Content,
                branch = branch
            )
            val res = api.createFile(getAuthHeader(), owner, repo, path, payload)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun triggerWorkflow(branch: String, workflowFile: String = "build.yml"): Boolean {
        return try {
            val payload = DispatchPayload(ref = branch)
            val res = api.triggerWorkflow(getAuthHeader(), owner, repo, workflowFile, payload)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getWorkflowRunStatus(branch: String): WorkflowRun? {
        return try {
            val res = api.getWorkflowRuns(getAuthHeader(), owner, repo, branch)
            if (res.isSuccessful) {
                res.body()?.workflowRuns?.firstOrNull()
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadArtifactResult(runId: Long): String? {
        return try {
            val res = api.getArtifacts(getAuthHeader(), owner, repo, runId)
            if (res.isSuccessful) {
                val artifact = res.body()?.artifacts?.firstOrNull() ?: return null
                return "Artifact Output downloaded successfully (${artifact.name}, ${artifact.sizeInBytes} bytes)"
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteBranch(branch: String): Boolean {
        return try {
            val res = api.deleteBranch(getAuthHeader(), owner, repo, branch)
            res.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
