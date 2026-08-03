package sayan.apps.rupeeflow.core.ai.backend

import kotlinx.coroutines.delay
import sayan.apps.rupeeflow.core.ai.session.ChatMessage
import sayan.apps.rupeeflow.core.ai.session.Role
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockAiBackend @Inject constructor() : AiBackend {
    private var initialized = false

    override suspend fun initialize() {
        delay(500)
        initialized = true
    }

    override suspend fun generate(
        messages: List<ChatMessage>,
        onToken: ((String) -> Unit)?
    ): String {
        delay(1000) // Simulate "thinking"
        
        val lastUserMessage = messages.lastOrNull { it.role == Role.USER }?.content ?: ""
        val toolMessage = messages.lastOrNull { it.role == Role.TOOL }?.content ?: ""
        
        val response = if (toolMessage.isNotBlank()) {
            "Based on your data: $toolMessage"
        } else {
            "I'm in Demo Mode. I can't process complex queries without the local model, but I can see your basic transaction summaries!"
        }

        if (onToken != null) {
            response.split(" ").forEach { token ->
                onToken("$token ")
                delay(50)
            }
        }
        
        return response
    }

    override fun isInitialized(): Boolean = initialized

    override suspend fun release() {
        initialized = false
    }
}
