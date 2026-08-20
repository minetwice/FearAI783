package com.agent.app.tools

import com.agent.app.github.GitHubClient
import com.agent.app.github.WorkflowTemplates
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.delay

data class ToolCall(
    val tool: String,
    val params: JsonObject,
    val thought: String
)

sealed class ToolResult {
    data class Success(val message: String) : ToolResult()
    data class Error(val error: String) : ToolResult()
    data class FinalResponse(val message: String) : ToolResult()
}

class ToolExecutor(private val githubClient: GitHubClient) {

    private var activeBranch: String? = null
    private var lastRunId: Long? = null

    suspend fun executeTool(toolCall: ToolCall): ToolResult {
        return when (toolCall.tool.lowercase()) {
            "create_file" -> {
                val path = toolCall.params.get("path")?.asString ?: return ToolResult.Error("Missing 'path' parameter.")
                val content = toolCall.params.get("content")?.asString ?: return ToolResult.Error("Missing 'content' parameter.")

                if (path.contains("..") || path.startsWith("/")) {
                    return ToolResult.Error("Path traversal not allowed.")
                }

                if (activeBranch == null) {
                    activeBranch = githubClient.createNewBranch()
                    if (activeBranch == null) return ToolResult.Error("Failed to create GitHub branch.")
                }

                val success = githubClient.createFile(activeBranch!!, path, content)
                if (success) {
                    ToolResult.Success("File $path created successfully on branch $activeBranch.")
                } else {
                    ToolResult.Error("Failed to push file $path to GitHub.")
                }
            }

            "create_workflow" -> {
                val language = toolCall.params.get("language")?.asString ?: "python"
                val filename = toolCall.params.get("filename")?.asString ?: "main.py"

                val workflowYaml = WorkflowTemplates.getTemplate(language, filename)

                if (activeBranch == null) {
                    activeBranch = githubClient.createNewBranch()
                    if (activeBranch == null) return ToolResult.Error("Failed to create GitHub branch.")
                }

                val success = githubClient.createFile(activeBranch!!, ".github/workflows/build.yml", workflowYaml)
                if (success) {
                    ToolResult.Success("Workflow template created successfully for $language.")
                } else {
                    ToolResult.Error("Failed to create workflow YAML.")
                }
            }

            "trigger_build" -> {
                val branch = toolCall.params.get("branch")?.asString ?: activeBranch ?: return ToolResult.Error("No active branch.")
                val triggered = githubClient.triggerWorkflow(branch, "build.yml")
                if (triggered) {
                    ToolResult.Success("Build triggered on branch $branch.")
                } else {
                    ToolResult.Error("Failed to trigger GitHub Actions workflow.")
                }
            }

            "check_status" -> {
                val branch = activeBranch ?: return ToolResult.Error("No active branch.")
                val run = githubClient.getWorkflowRunStatus(branch)
                if (run != null) {
                    lastRunId = run.id
                    ToolResult.Success("Workflow status: ${run.status}, conclusion: ${run.conclusion ?: "running"}")
                } else {
                    ToolResult.Error("No workflow run found yet.")
                }
            }

            "get_result" -> {
                val runId = lastRunId ?: return ToolResult.Error("No active run ID found.")
                val resultText = githubClient.downloadArtifactResult(runId)
                if (resultText != null) {
                    ToolResult.Success("Result downloaded: $resultText")
                } else {
                    ToolResult.Error("Failed to download build artifact.")
                }
            }

            "delete_branch" -> {
                val branch = toolCall.params.get("branch")?.asString ?: activeBranch ?: return ToolResult.Success("No branch to delete.")
                val deleted = githubClient.deleteBranch(branch)
                if (deleted) {
                    activeBranch = null
                    ToolResult.Success("Branch $branch deleted successfully.")
                } else {
                    ToolResult.Error("Failed to delete branch $branch.")
                }
            }

            "respond" -> {
                val message = toolCall.params.get("message")?.asString ?: "Task complete."
                // Clean up branch if active
                activeBranch?.let { githubClient.deleteBranch(it) }
                ToolResult.FinalResponse(message)
            }

            else -> ToolResult.Error("Unknown tool call: ${toolCall.tool}")
        }
    }
}
