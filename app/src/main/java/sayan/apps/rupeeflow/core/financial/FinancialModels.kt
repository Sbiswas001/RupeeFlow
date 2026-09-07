package sayan.apps.rupeeflow.core.financial

import sayan.apps.rupeeflow.domain.model.Transaction

data class NetCashFlow(
    val income: Double,
    val spending: Double,
    val netCashFlow: Double,
    val textSummary: String
)

data class FormattedCategoryShare(
    val categoryId: Long? = null,
    val categoryName: String,
    val amount: Double,
    val percentage: Double,
    val formattedPercentage: String,
    val isOther: Boolean = false,
    val colorHex: String = "#3B82F6",
    val transactionCount: Int = 0
)

data class SafeToSpendResult(
    val safeToSpendToday: Double,
    val availableToSpend: Double,
    val remainingBudget: Double,
    val daysRemaining: Int,
    val dailyTarget: Double,
    val hasConfiguredBudget: Boolean,
    val statusMessage: String,
    val explanation: String
)

data class BudgetUsageResult(
    val overallUsagePercent: Double,
    val totalBudgetedLimit: Double,
    val totalBudgetedSpent: Double,
    val budgetedCategoriesCount: Int
)

data class ForecastResult(
    val currentBalance: Double,
    val expectedIncome: Double,
    val upcomingBills: Double,
    val projectedRemainingSpending: Double,
    val estimatedMonthEnd: Double,
    val isPacingOverBudget: Boolean,
    val projectedOverrunDay: Int? = null,
    val statusMessage: String = "On track",
    val isProjectedDeficit: Boolean = false
)

enum class FinancialHealthConfidence {
    INSUFFICIENT_DATA,
    PRELIMINARY,
    MODERATE,
    HIGH
}

data class MonthlyCashFlow(
    val monthLabel: String = "",
    val income: Double,
    val expense: Double,
    val netCashFlow: Double = income - expense
)

data class FinancialHealthSubScore(
    val name: String,
    val score: Int,
    val isAvailable: Boolean = true,
    val explanation: String,
    val weight: Double = 0.25,
    val supportingValueText: String = ""
)

data class FinancialHealthResult(
    val overallScore: Int?,
    val statusLabel: String,
    val subScores: List<FinancialHealthSubScore>,
    val isDataSufficient: Boolean,
    val advicePills: List<String>,
    val confidence: FinancialHealthConfidence = FinancialHealthConfidence.HIGH,
    val confidenceLabel: String = "",
    val keyInsight: String = "",
    val spendingControlScore: Int? = null,
    val cashFlowScore: Int? = null,
    val savingsScore: Int? = null,
    val commitmentLoadScore: Int? = null
)

enum class TrendDirection {
    UP, DOWN, NO_CHANGE, NEW
}

data class CategoryTrend(
    val categoryId: Long? = null,
    val categoryName: String,
    val currentAmount: Double,
    val previousAmount: Double,
    val percentChange: Double?,
    val formattedChange: String,
    val direction: TrendDirection
)

data class SpendingAnomaly(
    val title: String,
    val description: String,
    val transaction: Transaction? = null,
    val multiplier: Double
)
