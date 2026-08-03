package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val messageCount: Int = 0
)
