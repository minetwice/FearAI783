package com.agent.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.agent.app.data.ChatDatabase
import com.agent.app.github.GitHubClient
import com.agent.app.huggingface.HuggingFaceApi
import com.agent.app.huggingface.HuggingFaceModel
import com.agent.app.model.*
import com.agent.app.security.TokenManager
import com.agent.app.ui.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var modelDownloadManager: ModelDownloadManager
    private lateinit var modelManager: ModelManager
    private lateinit var database: ChatDatabase
    private lateinit var huggingFaceApi: HuggingFaceApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        modelDownloadManager = ModelDownloadManager(this)
        modelManager = ModelManager(this)
        database = ChatDatabase.getDatabase(this)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://huggingface.co/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        huggingFaceApi = retrofit.create(HuggingFaceApi::class.java)

        setContent {
            var selectedTab by remember { mutableStateOf(0) }
            var currentStatus by remember { mutableStateOf<String?>(null) }

            val downloadState by modelDownloadManager.downloadState.collectAsState()
            var isModelDownloaded by remember { mutableStateOf(modelManager.listDownloadedModels().isNotEmpty()) }

            var modelsList by remember { mutableStateOf<List<HuggingFaceModel>>(emptyList()) }
            var isLoadingHfModels by remember { mutableStateOf(false) }
            var selectedHfModel by remember { mutableStateOf<HuggingFaceModel?>(null) }

            val messages = remember { mutableStateListOf<ChatUiMessage>() }

            // Fetch default Hugging Face models on load
            LaunchedEffect(Unit) {
                messages.add(
                    ChatUiMessage(
                        sender = "agent",
                        text = "👋 Welcome to FearAI Agent! Active Model: ${modelManager.getActiveModelName()}. Enter a task to build & compile on-device and cloud!"
                    )
                )

                isLoadingHfModels = true
                try {
                    val res = huggingFaceApi.searchModels()
                    if (res.isSuccessful) {
                        modelsList = res.body() ?: emptyList()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoadingHfModels = false
                }
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
                            icon = { Text("🤗", fontSize = 18.sp) },
                            label = { Text("Models", color = Color.White) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Text("📁", fontSize = 18.sp) },
                            label = { Text("Files", color = Color.White) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            icon = { Text("⚙️", fontSize = 18.sp) },
                            label = { Text("Settings", color = Color.White) }
                        )
                    }
                }
            ) { innerPadding ->
                Column(modifier = Modifier.padding(innerPadding)) {
                    // Active Download Progress Widget across all tabs
                    DownloadProgressCard(
                        downloadState = downloadState,
                        onPause = { modelDownloadManager.pauseDownload() },
                        onResume = { modelDownloadManager.resumeDownload(downloadState.modelId, downloadState.filename) },
                        onCancel = { modelDownloadManager.cancelDownload() }
                    )

                    Surface(modifier = Modifier.weight(1f)) {
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
                            1 -> {
                                BrowseModelsScreen(
                                    modelsList = modelsList,
                                    isLoading = isLoadingHfModels,
                                    onSearch = { query ->
                                        lifecycleScope.launch {
                                            isLoadingHfModels = true
                                            try {
                                                val res = huggingFaceApi.searchModels(search = query)
                                                if (res.isSuccessful) {
                                                    modelsList = res.body() ?: emptyList()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            } finally {
                                                isLoadingHfModels = false
                                            }
                                        }
                                    },
                                    onSelectModel = { model ->
                                        selectedHfModel = model
                                    }
                                )

                                selectedHfModel?.let { model ->
                                    ModelDetailBottomSheet(
                                        model = model,
                                        onDismiss = { selectedHfModel = null },
                                        onStartDownload = { modelId, filename ->
                                            lifecycleScope.launch {
                                                modelDownloadManager.startDownload(modelId, filename)
                                                isModelDownloaded = true
                                            }
                                        }
                                    )
                                }
                            }
                            2 -> FileViewerScreen(files = emptyList())
                            3 -> SettingsScreen(
                                currentPat = tokenManager.getToken(),
                                currentOwner = tokenManager.getRepoOwner(),
                                currentRepo = tokenManager.getRepoName(),
                                currentBaseBranch = tokenManager.getBaseBranch(),
                                isModelDownloaded = isModelDownloaded,
                                downloadState = DownloadState.Idle,
                                onSaveGitHubConfig = { pat, owner, repo, baseBranch ->
                                    tokenManager.saveToken(pat)
                                    tokenManager.saveRepoOwner(owner)
                                    tokenManager.saveRepoName(repo)
                                    tokenManager.saveBaseBranch(baseBranch)
                                },
                                onStartModelDownload = {
                                    lifecycleScope.launch {
                                        modelDownloadManager.startDownload("Qwen/Qwen2.5-3B-Instruct-GGUF", "qwen2.5-3b-instruct-q4_k_m.gguf")
                                        isModelDownloaded = true
                                    }
                                },
                                onDeleteModel = {
                                    modelManager.deleteModel("qwen2.5-3b-instruct-q4_k_m.gguf")
                                    isModelDownloaded = modelManager.listDownloadedModels().isNotEmpty()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
