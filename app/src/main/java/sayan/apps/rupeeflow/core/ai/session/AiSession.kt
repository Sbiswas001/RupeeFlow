package sayan.apps.rupeeflow.core.ai.session

class AiSession(
    private val maxMessages: Int = 12
) {
    private val history = mutableListOf<ChatMessage>()

    fun add(message: ChatMessage) {
        history += message
        trim()
    }

    fun clear() {
        history.clear()
    }

    fun snapshot(): List<ChatMessage> = history.toList()

    private fun trim() {
        while (history.size > maxMessages) {
            history.removeAt(0)
        }
    }
}

data class ChatMessage(
    val role: Role,
    val content: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

enum class Role {
    USER,
    ASSISTANT,
    TOOL
}
