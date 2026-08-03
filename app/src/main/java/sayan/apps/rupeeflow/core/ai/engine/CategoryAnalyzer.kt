package sayan.apps.rupeeflow.core.ai.engine

import sayan.apps.rupeeflow.core.ai.intent.PeriodSpec
import sayan.apps.rupeeflow.core.ai.tools.ToolResult
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class CategoryAnalyzer @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    suspend fun categoryExpense(categoryName: String, period: PeriodSpec): ToolResult {
        val now = System.currentTimeMillis()
        val start = DateUtils.getStartOfMonth(now)
        val end = DateUtils.getEndOfMonth(now)
        
        // Find category by name
        val categories = categoryRepository.getCategories().first()
        val category = categories.find { it.name.equals(categoryName, ignoreCase = true) }
            ?: return ToolResult.Error("Category '$categoryName' not found.")
            
        val total = transactionRepository.getTotalAmountForCategory(category.id.toLong()).first() ?: 0.0
        
        return ToolResult.Success(
            summary = "You spent total ₹$total on $categoryName.",
            structuredData = mapOf("category" to categoryName, "total" to total)
        )
    }

    suspend fun suggestCategory(merchant: String, amount: Double?): ToolResult {
        return ToolResult.Success("Suggestion for $merchant not implemented yet.")
    }
}
