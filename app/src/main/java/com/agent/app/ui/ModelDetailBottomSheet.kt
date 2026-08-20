package com.agent.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agent.app.huggingface.HuggingFaceModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelDetailBottomSheet(
    model: HuggingFaceModel,
    onDismiss: () -> Unit,
    onStartDownload: (modelId: String, filename: String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF121824)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("🤖 ${model.id}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("📥 ${model.downloads} downloads | ♥ ${model.likes} likes", color = Color(0xFF06B6D4), fontSize = 12.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Available GGUF Files:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))

            val filenames = model.siblings?.map { it.rfilename }?.filter { it.endsWith(".gguf") }
                ?: listOf("qwen2.5-3b-instruct-q4_k_m.gguf")

            filenames.forEach { filename ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(filename, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                            Text(
                                if (filename.contains("q4_k_m")) "Recommended (Q4_K_M) - ~2.0 GB (Est. ~3GB RAM)"
                                else "GGUF Quantized Format",
                                color = if (filename.contains("q4_k_m")) Color(0xFF10B981) else Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = {
                                onStartDownload(model.id, filename)
                                onDismiss()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Download", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
