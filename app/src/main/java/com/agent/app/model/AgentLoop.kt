package com.agent.app.model

import com.agent.app.github.GitHubClient
import com.agent.app.tools.ToolCall
import com.agent.app.tools.ToolExecutor
import com.agent.app.tools.ToolResult
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class AgentState {
    data class StatusUpdate(val statusText: String) : AgentState()
    data class AgentThought(val thought: String) : AgentState()
    data class Completed(val resultMessage: String) : AgentState()
    data class Failed(val errorMessage: String) : AgentState()
}

class AgentLoop(
    private val modelManager: ModelManager,
    private val githubClient: GitHubClient
) {
    private val gson = Gson()
    private val toolExecutor = ToolExecutor(githubClient)

    companion object {
        const val SYSTEM_PROMPT = """You are an AI coding agent running on an Android device. You help users create files, write code, and compile code using GitHub Actions.

You communicate using JSON tool calls. Every response must be a valid JSON object with this format:

{"tool": "<tool_name>", "params": {<params>}, "thought": "<your reasoning>"}

Available tools:
1. create_file
   params: {"path": "<filename>", "content": "<file content>"}
   Creates a file in the project branch on GitHub

2. create_workflow
   params: {"language": "<python|javascript|c|cpp|java>", "filename": "<main file>"}
   Creates a GitHub Actions workflow using a pre-made template for the specified language

3. trigger_build
   params: {"branch": "<branch name>"}
   Triggers the GitHub Actions workflow on the branch

4. check_status
   params: {"run_id": "<run id>"}
   Checks the status of a GitHub Actions run

5. get_result
   params: {"run_id": "<run id>"}
   Downloads the build artifact (output)

6. delete_branch
   params: {"branch": "<branch name>"}
   Deletes the branch after task is complete

7. respond
   params: {"message": "<message to user>"}
   Send a message to the user and end the task

Rules:
- Always create files before triggering build
- Always create workflow before triggering build
- Always check status after triggering build
- Always delete branch after getting result
- Maximum 10 tool calls per task
- If something fails, respond with an error message to the user
- Never output anything except the JSON tool call
- Never include markdown, explanation, or text outside the JSON"""
    }

    fun runAgentTask(userTask: String): Flow<AgentState> = flow {
        emit(AgentState.StatusUpdate("Initializing Qwen 2.5 Local Agent..."))

        var currentPrompt = "$SYSTEM_PROMPT\n\nUser task: $userTask"
        var iterations = 0
        val maxIterations = 10

        while (iterations < maxIterations) {
            iterations++
            emit(AgentState.StatusUpdate("Thinking (Iter $iterations)..."))

            val responseText = modelManager.generate(currentPrompt, maxTokens = 2048, temperature = 0.7f)

            val toolCall = parseJsonToolCall(responseText)
            if (toolCall == null) {
                currentPrompt += "\n\nError: Your response was not valid JSON. Please respond with only a JSON tool call."
                continue
            }

            emit(AgentState.AgentThought(toolCall.thought))

            emit(AgentState.StatusUpdate("Executing tool: ${toolCall.tool}..."))
            val result = toolExecutor.executeTool(toolCall)

            when (result) {
                is ToolResult.FinalResponse -> {
                    emit(AgentState.Completed(result.message))
                    return@flow
                }
                is ToolResult.Success -> {
                    currentPrompt += "\n\nTool result: ${result.message}"
                }
                is ToolResult.Error -> {
                    currentPrompt += "\n\nTool error: ${result.error}"
                }
            }
        }

        emit(AgentState.Failed("Task reached max iteration limit without completing."))
    }

    private fun parseJsonToolCall(text: String): ToolCall? {
        return try {
            val startIdx = text.indexOf('{')
            val endIdx = text.lastIndexOf('}')
            if (startIdx == -1 || endIdx == -1 || endIdx <= startIdx) return null

            val jsonSub = text.substring(startIdx, endIdx + 1)
            val jsonObj = gson.fromJson(jsonSub, JsonObject::class.java)

            val tool = jsonObj.get("tool")?.asString ?: return null
            val params = jsonObj.getAsJsonObject("params") ?: JsonObject()
            val thought = jsonObj.get("thought")?.asString ?: "Executing tool $tool"

            ToolCall(tool, params, thought)
        } catch (e: Exception) {
            null
        }
    }
}
