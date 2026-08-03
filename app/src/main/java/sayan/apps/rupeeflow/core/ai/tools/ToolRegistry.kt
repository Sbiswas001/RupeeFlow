package sayan.apps.rupeeflow.core.ai.tools

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToolRegistry @Inject constructor(
    private val tools: List<@JvmSuppressWildcards FinanceTool>
) {
    private val toolMap: Map<ToolId, FinanceTool> = tools.associateBy { it.supportsTool() }

    fun getTool(toolId: ToolId): FinanceTool? = toolMap[toolId]
}
