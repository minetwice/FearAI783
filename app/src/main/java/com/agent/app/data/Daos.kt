package com.agent.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: Long)
}

@Dao
interface TaskSessionDao {

    @Query("SELECT * FROM task_sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<TaskSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskSession(session: TaskSessionEntity): Long

    @Query("UPDATE task_sessions SET status = :status, completedAt = :completedAt WHERE id = :id")
    suspend fun updateSessionStatus(id: Long, status: String, completedAt: Long?)
}
