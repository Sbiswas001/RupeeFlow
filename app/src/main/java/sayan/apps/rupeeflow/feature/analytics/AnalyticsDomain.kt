package sayan.apps.rupeeflow.feature.analytics

import sayan.apps.rupeeflow.domain.model.Transaction

enum class Aggregation {
    DAY, WEEK, MONTH, YEAR
}

enum class InsightType {
    SPENDING_SPIKE, BUDGET_ALERT, SAVINGS_TIP, POSITIVE_TREND, CATEGORY_DOMINANCE
}

data class Insight(
    val type: InsightType,
    val title: String,
    val description: String,
    val amount: Double? = null,
    val icon: String = "💡",
    val targetCategory: String? = null
)

data class MerchantInfo(
    val name: String,
    val totalAmount: Double,
    val transactionCount: Int
)

data class InteractiveTrendPoint(
    val timestamp: Long,
    val dateLabel: String,
    val amount: Double,
    val topCategory: String? = null,
    val topTransactionTitle: String? = null
)
