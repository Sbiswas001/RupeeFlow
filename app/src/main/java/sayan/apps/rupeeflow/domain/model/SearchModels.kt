package sayan.apps.rupeeflow.domain.model

data class SearchFilters(
    val query: String = "",
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val type: TransactionType? = null,
    val isRecurring: Boolean? = null
)

sealed interface SearchResult {
    data class TransactionResult(val transaction: Transaction) : SearchResult
    data class AccountResult(val account: Account) : SearchResult
    data class CategoryResult(val category: Category) : SearchResult
    data class RecurringResult(val item: RecurringItem) : SearchResult
    data class BudgetResult(val budget: Budget, val categoryName: String) : SearchResult
    data class GoalResult(val goal: Goal) : SearchResult
}
