package sayan.apps.rupeeflow.core.ai.engine

import sayan.apps.rupeeflow.core.ai.tools.ToolResult
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class BudgetAnalyzer @Inject constructor(
    private val planningRepository: PlanningRepository
) {
    suspend fun budgetStatus(): ToolResult {
        val budgets = planningRepository.getBudgetsWithProgress().first()
        if (budgets.isEmpty()) return ToolResult.Success("You haven't set any budgets yet.")
        
        val summary = budgets.joinToString("\n") { 
            "${it.budget.categoryName}: ₹${it.budget.spentAmount} spent of ₹${it.budget.limitAmount} (${String.format("%.1f", it.progress * 100)}%)" 
        }
        
        return ToolResult.Success(
            summary = "Here is your budget status:\n$summary",
            structuredData = mapOf("budgets" to budgets)
        )
    }
}
