package sayan.apps.rupeeflow.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.repository.TrendPoint
import javax.inject.Inject

data class AnalyticsState(
    val categorySpending: List<CategorySpending> = emptyList(),
    val spendingTrend: List<TrendPoint> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netSavings: Double = 0.0,
    val transactionCount: Int = 0,
    val spentTrendPercent: Double = 0.0,
    val incomeTrendPercent: Double = 0.0,
    val savingsTrendPercent: Double = 0.0,
    val transTrendPercent: Double = 0.0,
    val highestExpense: sayan.apps.rupeeflow.domain.model.Transaction? = null,
    val topMerchantName: String = "N/A",
    val topMerchantTrans: Int = 0,
    val topMerchantAmount: Double = 0.0,
    val dailyAverage: Double = 0.0,
    val isFinancialYear: Boolean = false,
    val fyLabel: String = "",
    val isLoading: Boolean = true,
    val selectedFilter: String = "30D"
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _isFinancialYear = MutableStateFlow(false)
    private val _selectedFilter = MutableStateFlow("30D")

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsState> = combine(_isFinancialYear, _selectedFilter) { isFY, filter ->
        Pair(isFY, filter)
    }.flatMapLatest { (isFY, filter) ->
        val range = getRangeForFilter(filter, isFY)
        combine(
            repository.getCategorySpending(range.first, range.second),
            repository.getSpendingTrend(range.first, range.second),
            repository.getTransactions()
        ) { categorySpending, spendingTrend, transactions ->
            val filteredTransactions = transactions.filter { it.timestamp in range.first..range.second }
            val expenses = filteredTransactions.filter { !it.isIncome }
            val incomes = filteredTransactions.filter { it.isIncome }
            val spent = expenses.sumOf { it.amount }
            val income = incomes.sumOf { it.amount }
            
            // Previous period for comparison
            val periodLength = range.second - range.first
            val prevRange = Pair(range.first - periodLength, range.first)
            val prevTransactions = transactions.filter { it.timestamp in prevRange.first..prevRange.second }
            val prevSpent = prevTransactions.filter { !it.isIncome }.sumOf { it.amount }
            val prevIncome = prevTransactions.filter { it.isIncome }.sumOf { it.amount }

            val highest = expenses.maxByOrNull { it.amount }
            val merchants = expenses.groupBy { it.title }
            val topMerchant = merchants.maxByOrNull { it.value.sumOf { t -> t.amount } }

            val fullTrend = fillMissingDays(range.first, range.second, spendingTrend)
            val daysCount = ((range.second - range.first) / 86400000).coerceAtLeast(1).toDouble()

            AnalyticsState(
                categorySpending = categorySpending,
                spendingTrend = fullTrend,
                totalSpent = spent,
                totalIncome = income,
                netSavings = income - spent,
                transactionCount = filteredTransactions.size,
                highestExpense = highest,
                topMerchantName = topMerchant?.key ?: "N/A",
                topMerchantTrans = topMerchant?.value?.size ?: 0,
                topMerchantAmount = topMerchant?.value?.sumOf { it.amount } ?: 0.0,
                dailyAverage = spent / daysCount,
                spentTrendPercent = calculatePercentChange(prevSpent, spent),
                incomeTrendPercent = calculatePercentChange(prevIncome, income),
                savingsTrendPercent = calculatePercentChange(prevIncome - prevSpent, income - spent),
                transTrendPercent = calculatePercentChange(prevTransactions.size.toDouble(), filteredTransactions.size.toDouble()),
                isFinancialYear = isFY,
                fyLabel = if (isFY) DateUtils.getFinancialYearLabel() else "Calendar Year",
                selectedFilter = filter,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )

    private fun calculatePercentChange(old: Double, new: Double): Double {
        if (old == 0.0) return if (new > 0.0) 100.0 else 0.0
        return ((new - old) / old) * 100.0
    }

    private fun fillMissingDays(start: Long, end: Long, trend: List<TrendPoint>): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        var current = start
        val trendMap = trend.associateBy { (it.timestamp / 86400000) * 86400000 }
        
        while (current <= end) {
            val dayStart = (current / 86400000) * 86400000
            result.add(TrendPoint(dayStart, trendMap[dayStart]?.amount ?: 0.0))
            current += 86400000
        }
        return result
    }

    fun toggleFinancialYear() {
        _isFinancialYear.value = !_isFinancialYear.value
    }

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    private fun getRangeForFilter(filter: String, isFY: Boolean): Pair<Long, Long> {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        return when (filter) {
            "7D" -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
                Pair(calendar.timeInMillis, now)
            }
            "30D" -> {
                calendar.add(java.util.Calendar.DAY_OF_YEAR, -30)
                Pair(calendar.timeInMillis, now)
            }
            "3M" -> {
                calendar.add(java.util.Calendar.MONTH, -3)
                Pair(calendar.timeInMillis, now)
            }
            "6M" -> {
                calendar.add(java.util.Calendar.MONTH, -6)
                Pair(calendar.timeInMillis, now)
            }
            "1Y" -> {
                calendar.add(java.util.Calendar.YEAR, -1)
                Pair(calendar.timeInMillis, now)
            }
            "YTD" -> {
                calendar.set(java.util.Calendar.DAY_OF_YEAR, 1)
                Pair(calendar.timeInMillis, now)
            }
            // Add more as needed
            else -> {
                if (isFY) DateUtils.getCurrentFinancialYearRange()
                else Pair(0L, now)
            }
        }
    }
}
