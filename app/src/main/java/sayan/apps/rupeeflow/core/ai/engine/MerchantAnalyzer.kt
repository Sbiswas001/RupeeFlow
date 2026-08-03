package sayan.apps.rupeeflow.core.ai.engine

import sayan.apps.rupeeflow.core.ai.intent.PeriodSpec
import sayan.apps.rupeeflow.core.ai.tools.ToolResult
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.core.util.DateUtils
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class MerchantAnalyzer @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend fun topMerchants(period: PeriodSpec, limit: Int): ToolResult {
        val now = System.currentTimeMillis()
        val start = DateUtils.getStartOfMonth(now)
        val end = DateUtils.getEndOfMonth(now)
        
        val top = transactionRepository.getTopMerchants(start, end, limit).first()
        if (top.isEmpty()) return ToolResult.Success("No merchant data found for this period.")
        
        val summary = top.joinToString("\n") { "${it.categoryName}: ₹${it.amount}" }
        
        return ToolResult.Success(
            summary = "Your top merchants are:\n$summary",
            structuredData = mapOf("merchants" to top)
        )
    }
}
