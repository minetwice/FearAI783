package com.agent.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val role: String, // "user", "agent", "system"
    val content: String,
    val timestamp: Long,
    val toolCallsJson: String? = null,
    val status: String = "completed"
)

@Entity(tableName = "task_sessions")
data class TaskSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userTask: String,
    val branchName: String?,
    val status: String, // "created", "building", "completed", "failed", "cleaned"
    val workflowRunId: String?,
    val createdAt: Long,
    val completedAt: Long? = null
)
