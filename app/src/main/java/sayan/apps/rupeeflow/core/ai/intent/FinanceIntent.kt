package sayan.apps.rupeeflow.core.ai.intent

import sayan.apps.rupeeflow.core.ai.tools.ToolId

sealed interface FinanceIntent {
    val toolId: ToolId

    data object GetMonthlySummary : FinanceIntent {
        override val toolId = ToolId.GET_MONTHLY_SUMMARY
    }

    data class GetCategoryExpense(val category: String, val period: PeriodSpec) : FinanceIntent {
        override val toolId = ToolId.GET_CATEGORY_EXPENSE
    }

    data class GetTopMerchants(val period: PeriodSpec, val limit: Int = 5) : FinanceIntent {
        override val toolId = ToolId.GET_TOP_MERCHANTS
    }

    data class ComparePeriods(val left: PeriodSpec, val right: PeriodSpec) : FinanceIntent {
        override val toolId = ToolId.UNKNOWN // For now
    }

    data class DetectRecurringPayments(val period: PeriodSpec? = null) : FinanceIntent {
        override val toolId = ToolId.DETECT_RECURRING_PAYMENTS
    }

    data class DetectAnomalies(val period: PeriodSpec) : FinanceIntent {
        override val toolId = ToolId.DETECT_ANOMALIES
    }

    data object GetBudgetStatus : FinanceIntent {
        override val toolId = ToolId.GET_BUDGET_STATUS
    }

    data class CategorizeTransaction(val merchant: String, val amount: Double? = null) : FinanceIntent {
        override val toolId = ToolId.UNKNOWN // For now
    }

    data class SearchTransactions(val query: String) : FinanceIntent {
        override val toolId = ToolId.SEARCH_TRANSACTIONS
    }

    data object Unknown : FinanceIntent {
        override val toolId = ToolId.UNKNOWN
    }
}

data class PeriodSpec(
    val type: PeriodType,
    val month: Int? = null,
    val year: Int? = null,
)

enum class PeriodType {
    CURRENT_MONTH,
    PREVIOUS_MONTH,
    LAST_7_DAYS,
    LAST_30_DAYS,
    CUSTOM
}
