package sayan.apps.rupeeflow.domain.model

data class Budget(
    val id: Long,
    val categoryId: Long,
    val categoryName: String,
    val categoryIcon: String = "💰",
    val limitAmount: Double,
    val spentAmount: Double = 0.0,
    val period: String, // WEEKLY, MONTHLY
    val startDate: Long
)

data class BudgetWithProgress(
    val budget: Budget,
    val progress: Float, // 0.0 to 1.0
    val remaining: Double
)
