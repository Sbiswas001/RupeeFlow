package sayan.apps.rupeeflow.core.ai.tools

import sayan.apps.rupeeflow.core.ai.intent.FinanceIntent

class ToolRouter(
    private val toolRegistry: ToolRegistry
) {
    suspend fun route(intent: FinanceIntent): ToolResult {
        val tool = toolRegistry.getTool(intent.toolId)
            ?: return ToolResult.Error("No tool found for toolId: ${intent.toolId}")
        return tool.execute(intent)
    }
}
