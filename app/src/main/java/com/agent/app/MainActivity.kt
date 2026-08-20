package com.agent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import com.agent.app.data.ChatDatabase
import com.agent.app.github.GitHubClient
import com.agent.app.model.AgentLoop
import com.agent.app.model.AgentState
import com.agent.app.model.DownloadState
import com.agent.app.model.ModelDownloader
import com.agent.app.model.ModelManager
import com.agent.app.security.TokenManager
import com.agent.app.ui.ChatScreen
import com.agent.app.ui.ChatUiMessage
import com.agent.app.ui.FileViewerScreen
import com.agent.app.ui.SettingsScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var modelDownloader: ModelDownloader
    private lateinit var modelManager: ModelManager
    private lateinit var database: ChatDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        modelDownloader = ModelDownloader(this)
        modelManager = ModelManager(this)
        database = ChatDatabase.getDatabase(this)

        setContent {
            var selectedTab by remember { mutableStateOf(0) }
            var currentStatus by remember { mutableStateOf<String?>(null) }
            var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
            var isModelDownloaded by remember { mutableStateOf(modelDownloader.isModelDownloaded()) }

            val messages = remember { mutableStateListOf<ChatUiMessage>() }

            // Welcome Message
            LaunchedEffect(Unit) {
                messages.add(
                    ChatUiMessage(
                        sender = "agent",
                        text = "👋 Welcome to FearAI Agent! I am your local Qwen 2.5 3B AI Agent running on-device. Enter a task below to create, compile, and run code via GitHub Actions!"
                    )
                )
            }

            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF121824)) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Text("💬", fontSize = 18.sp) },
                            label = { Text("Chat", color = Color.White) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Text("📁", fontSize = 18.sp) },
                            label = { Text("Files", color = Color.White) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Text("⚙️", fontSize = 18.sp) },
                            label = { Text("Settings", color = Color.White) }
                        )
                    }
                }
            ) { innerPadding ->
                Surface(modifier = Modifier.padding(innerPadding)) {
                    when (selectedTab) {
                        0 -> ChatScreen(
                            currentStatus = currentStatus,
                            messages = messages,
                            onSendMessage = { userTask ->
                                messages.add(ChatUiMessage(sender = "user", text = userTask))
                                lifecycleScope.launch {
                                    val githubClient = GitHubClient(
                                        token = tokenManager.getToken(),
                                        owner = tokenManager.getRepoOwner(),
                                        repo = tokenManager.getRepoName()
                                    )
                                    val agentLoop = AgentLoop(modelManager, githubClient)

                                    var lastThought: String? = null
                                    agentLoop.runAgentTask(userTask).collectLatest { state ->
                                        when (state) {
                                            is AgentState.StatusUpdate -> currentStatus = state.statusText
                                            is AgentState.AgentThought -> lastThought = state.thought
                                            is AgentState.Completed -> {
                                                currentStatus = null
                                                messages.add(ChatUiMessage(sender = "agent", text = state.resultMessage, thought = lastThought))
                                            }
                                            is AgentState.Failed -> {
                                                currentStatus = null
                                                messages.add(ChatUiMessage(sender = "agent", text = "⚠️ Task Failed: ${state.errorMessage}", thought = lastThought))
                                            }
                                        }
                                    }
                                }
                            }
                        )
                        1 -> FileViewerScreen(files = emptyList())
                        2 -> SettingsScreen(
                            currentPat = tokenManager.getToken(),
                            currentOwner = tokenManager.getRepoOwner(),
                            currentRepo = tokenManager.getRepoName(),
                            currentBaseBranch = tokenManager.getBaseBranch(),
                            isModelDownloaded = isModelDownloaded,
                            downloadState = downloadState,
                            onSaveGitHubConfig = { pat, owner, repo, baseBranch ->
                                tokenManager.saveToken(pat)
                                tokenManager.saveRepoOwner(owner)
                                tokenManager.saveRepoName(repo)
                                tokenManager.saveBaseBranch(baseBranch)
                            },
                            onStartModelDownload = {
                                lifecycleScope.launch {
                                    modelDownloader.downloadModel().collectLatest { state ->
                                        downloadState = state
                                        if (state is DownloadState.Completed) {
                                            isModelDownloaded = true
                                        }
                                    }
                                }
                            },
                            onDeleteModel = {
                                modelDownloader.deleteModel()
                                isModelDownloaded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
