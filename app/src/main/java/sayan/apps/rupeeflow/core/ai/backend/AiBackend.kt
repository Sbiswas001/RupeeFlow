package sayan.apps.rupeeflow.core.ai.backend

import sayan.apps.rupeeflow.core.ai.session.ChatMessage

interface AiBackend {
    suspend fun initialize()
    suspend fun generate(
        messages: List<ChatMessage>,
        onToken: ((String) -> Unit)? = null
    ): String
    fun isInitialized(): Boolean
    suspend fun release()
}
