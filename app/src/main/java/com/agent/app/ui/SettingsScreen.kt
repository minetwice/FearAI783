package com.agent.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.app.model.DownloadState
import com.agent.app.model.ModelDownloader

@Composable
fun SettingsScreen(
    currentPat: String,
    currentOwner: String,
    currentRepo: String,
    currentBaseBranch: String,
    isModelDownloaded: Boolean,
    downloadState: DownloadState,
    onSaveGitHubConfig: (pat: String, owner: String, repo: String, baseBranch: String) -> Unit,
    onStartModelDownload: () -> Unit,
    onDeleteModel: () -> Unit
) {
    var pat by remember { mutableStateOf(currentPat) }
    var owner by remember { mutableStateOf(currentOwner) }
    var repo by remember { mutableStateOf(currentRepo) }
    var baseBranch by remember { mutableStateOf(currentBaseBranch) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D14))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("⚙️ FearAI Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        // GitHub Configuration Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🔑 GitHub Configuration (Actions Backend)", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pat, onValueChange = { pat = it },
                    label = { Text("GitHub Personal Access Token (PAT)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = owner, onValueChange = { owner = it },
                    label = { Text("Repository Owner (Username / Org)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = repo, onValueChange = { repo = it },
                    label = { Text("Repository Name") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = baseBranch, onValueChange = { baseBranch = it },
                    label = { Text("Base Branch (Default: main)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onSaveGitHubConfig(pat, owner, repo, baseBranch) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save GitHub Configuration")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local Model Management Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121824)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("🧠 Local Qwen 2.5 3B GGUF Model Status", fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isModelDownloaded) "Status: Model File Downloaded (Offline Ready)" else "Status: Model File Not Found (~2.0 GB)",
                    color = if (isModelDownloaded) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                when (downloadState) {
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(
                            progress = { downloadState.progressPercent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF7C3AED)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Downloading: ${downloadState.progressPercent}% (${downloadState.downloadSpeed})", color = Color.White, fontSize = 12.sp)
                    }
                    is DownloadState.Completed -> {
                        Text("✅ Model Download Complete!", color = Color(0xFF10B981), fontSize = 13.sp)
                    }
                    is DownloadState.Error -> {
                        Text("❌ Error: ${downloadState.message}", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onStartModelDownload,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isModelDownloaded) "Re-download" else "Download Model")
                    }

                    if (isModelDownloaded) {
                        Button(
                            onClick = onDeleteModel,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Delete File")
                        }
                    }
                }
            }
        }
    }
}
