package sayan.apps.rupeeflow.core.ai.tools

import sayan.apps.rupeeflow.core.ai.intent.FinanceIntent

interface FinanceTool {
    fun supportsTool(): ToolId
    suspend fun execute(intent: FinanceIntent): ToolResult
}
