package sayan.apps.rupeeflow.core.ai.engine

import sayan.apps.rupeeflow.core.ai.intent.PeriodSpec
import sayan.apps.rupeeflow.core.ai.tools.ToolResult
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class SubscriptionAnalyzer @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend fun detectRecurring(period: PeriodSpec?): ToolResult {
        val recurring = recurringRepository.getActiveRecurringItems().first()
        if (recurring.isEmpty()) return ToolResult.Success("I couldn't find any active subscriptions or recurring payments.")
        
        val summary = recurring.joinToString("\n") { 
            "${it.title}: ₹${it.amount} (${it.frequency})" 
        }
        
        return ToolResult.Success(
            summary = "Here are your detected recurring payments:\n$summary",
            structuredData = mapOf("recurring" to recurring)
        )
    }
}
