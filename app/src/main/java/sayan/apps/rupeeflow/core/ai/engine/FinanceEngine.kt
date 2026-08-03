package sayan.apps.rupeeflow.core.ai.engine

import sayan.apps.rupeeflow.core.ai.intent.FinanceIntent
import sayan.apps.rupeeflow.core.ai.metrics.AiMetricsCollector
import sayan.apps.rupeeflow.core.ai.tools.ToolResult

class FinanceEngine(
    private val analyticsEngine: AnalyticsEngine,
    private val categoryAnalyzer: CategoryAnalyzer,
    private val merchantAnalyzer: MerchantAnalyzer,
    private val budgetAnalyzer: BudgetAnalyzer,
    private val subscriptionAnalyzer: SubscriptionAnalyzer,
    private val metricsCollector: AiMetricsCollector
) {
    private val cache = mutableMapOf<String, ToolResult>()

    suspend fun execute(intent: FinanceIntent): ToolResult {
        val cacheKey = intent.toString()
        cache[cacheKey]?.let { 
            metricsCollector.recordCacheHit()
            return it 
        }

        metricsCollector.recordCacheMiss()
        val result = when (intent) {
            is FinanceIntent.GetMonthlySummary -> analyticsEngine.monthlySummary()
            is FinanceIntent.GetCategoryExpense -> categoryAnalyzer.categoryExpense(intent.category, intent.period)
            is FinanceIntent.GetTopMerchants -> merchantAnalyzer.topMerchants(intent.period, intent.limit)
            is FinanceIntent.ComparePeriods -> analyticsEngine.comparePeriods(intent.left, intent.right)
            is FinanceIntent.DetectRecurringPayments -> subscriptionAnalyzer.detectRecurring(intent.period)
            is FinanceIntent.DetectAnomalies -> analyticsEngine.detectAnomalies(intent.period)
            is FinanceIntent.GetBudgetStatus -> budgetAnalyzer.budgetStatus()
            is FinanceIntent.CategorizeTransaction -> categoryAnalyzer.suggestCategory(intent.merchant, intent.amount)
            is FinanceIntent.SearchTransactions -> analyticsEngine.searchTransactions(intent.query)
            FinanceIntent.Unknown -> ToolResult.Error("Unknown intent")
        }

        cache[cacheKey] = result
        return result
    }

    fun clearCache() {
        cache.clear()
    }
}
