package com.agent.app.github

import com.google.gson.annotations.SerializedName

data class GitHubUser(
    val login: String,
    val id: Long,
    @SerializedName("avatar_url") val avatarUrl: String
)

data class CreateRefRequest(
    val ref: String,
    val sha: String
)

data class CreateRefResponse(
    val ref: String,
    val objectSha: String?
)

data class ContentPayload(
    val message: String,
    val content: String, // Base64
    val branch: String
)

data class DispatchPayload(
    val ref: String
)

data class WorkflowRunList(
    @SerializedName("total_count") val totalCount: Int,
    @SerializedName("workflow_runs") val workflowRuns: List<WorkflowRun>
)

data class WorkflowRun(
    val id: Long,
    val name: String,
    val status: String, // "queued", "in_progress", "completed"
    val conclusion: String?, // "success", "failure", "cancelled"
    @SerializedName("head_branch") val headBranch: String,
    @SerializedName("html_url") val htmlUrl: String
)

data class ArtifactList(
    @SerializedName("total_count") val totalCount: Int,
    val artifacts: List<Artifact>
)

data class Artifact(
    val id: Long,
    val name: String,
    @SerializedName("size_in_bytes") val sizeInBytes: Long,
    @SerializedName("archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean
)
