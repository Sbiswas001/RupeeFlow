package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.ChatMessageEntity
import sayan.apps.rupeeflow.core.database.entity.ChatSessionEntity

@Dao
interface AiDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastOpenedAt DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Insert
    suspend fun insertMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearMessages(sessionId: String)
}
