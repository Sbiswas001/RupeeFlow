package sayan.apps.rupeeflow.core.ai.backend

import sayan.apps.rupeeflow.core.ai.intent.IntentExtractor
import sayan.apps.rupeeflow.core.ai.session.AiSession
import sayan.apps.rupeeflow.core.ai.session.ChatMessage
import sayan.apps.rupeeflow.core.ai.session.Role
import sayan.apps.rupeeflow.core.ai.tools.ToolResult
import sayan.apps.rupeeflow.core.ai.tools.ToolRouter
import javax.inject.Inject

class AiManager @Inject constructor(
    private val backend: AiBackend,
    private val intentExtractor: IntentExtractor,
    private val toolRouter: ToolRouter,
    private val session: AiSession
) {
    suspend fun initialize() {
        if (!backend.isInitialized()) {
            backend.initialize()
        }
    }

    suspend fun chat(userText: String, onToken: ((String) -> Unit)? = null): String {
        session.add(ChatMessage(role = Role.USER, content = userText))

        val parseResult = intentExtractor.extract(userText, session.snapshot().map { it.content })
        val toolResult = toolRouter.route(parseResult.intent)

        val toolSummary = when (toolResult) {
            is ToolResult.Success -> toolResult.summary
            is ToolResult.Error -> "Error retrieving data: ${toolResult.message}"
        }

        val response = backend.generate(
            messages = session.snapshot() + ChatMessage(
                role = Role.TOOL,
                content = toolSummary
            ),
            onToken = onToken
        )

        session.add(ChatMessage(role = Role.ASSISTANT, content = response))

        return response
    }

    suspend fun release() {
        backend.release()
    }
}
