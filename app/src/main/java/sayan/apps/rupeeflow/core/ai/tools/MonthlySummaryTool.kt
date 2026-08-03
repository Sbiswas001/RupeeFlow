package sayan.apps.rupeeflow.core.ai.tools

import sayan.apps.rupeeflow.core.ai.engine.FinanceEngine
import sayan.apps.rupeeflow.core.ai.intent.FinanceIntent
import javax.inject.Inject

class MonthlySummaryTool @Inject constructor(
    private val financeEngine: FinanceEngine
) : FinanceTool {
    override fun supportsTool(): ToolId = ToolId.GET_MONTHLY_SUMMARY
    override suspend fun execute(intent: FinanceIntent): ToolResult = financeEngine.execute(intent)
}
