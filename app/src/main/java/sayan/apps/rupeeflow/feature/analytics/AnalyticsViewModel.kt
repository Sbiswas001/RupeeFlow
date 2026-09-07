package sayan.apps.rupeeflow.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.financial.CategoryTrend
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.FormattedCategoryShare
import sayan.apps.rupeeflow.core.financial.NetCashFlow
import sayan.apps.rupeeflow.core.financial.SpendingAnomaly
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.repository.TrendPoint
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AnalyticsState(
    val netCashFlow: NetCashFlow = NetCashFlow(0.0, 0.0, 0.0, "No transactions for this period."),
    val categorySpending: List<CategorySpending> = emptyList(),
    val formattedCategoryShares: List<FormattedCategoryShare> = emptyList(),
    val categoryTrends: List<CategoryTrend> = emptyList(),
    val anomalies: List<SpendingAnomaly> = emptyList(),
    val interactiveTrendPoints: List<InteractiveTrendPoint> = emptyList(),
    val spendingTrend: List<TrendPoint> = emptyList(),
    val totalSpent: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netSavings: Double = 0.0,
    val transactionCount: Int = 0,
    val spentTrendPercent: Double? = null,
    val incomeTrendPercent: Double? = null,
    val savingsTrendPercent: Double? = null,
    val transTrendPercent: Double? = null,
    val highestExpense: Transaction? = null,
    val topMerchant: MerchantInfo? = null,
    val averageLabel: String = "Avg/day",
    val averageAmount: Double = 0.0,
    val isLoading: Boolean = true,
    val selectedFilter: String = "1M",
    val selectedMonth: Calendar? = null,
    val customRange: Pair<Long, Long>? = null,
    val formattedDateRange: String = "",
    val aggregation: Aggregation = Aggregation.DAY,
    val budgets: List<BudgetWithProgress> = emptyList(),
    val insights: List<Insight> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val planningRepository: PlanningRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow("1M")
    private val _selectedMonth = MutableStateFlow<Calendar?>(null)
    private val _customRange = MutableStateFlow<Pair<Long, Long>?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsState> = combine(
        _selectedFilter,
        _selectedMonth,
        _customRange
    ) { filter, month, custom ->
        DataQuery(filter, month, custom)
    }.flatMapLatest { query ->
        val range = when {
            query.custom != null -> query.custom
            query.month != null -> {
                val cal = query.month.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis

                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            else -> getRangeForFilter(query.filter)
        }

        val aggregation = when (query.filter) {
            "1D", "1W", "1M" -> Aggregation.DAY
            "3M", "6M" -> Aggregation.WEEK
            "1Y" -> Aggregation.MONTH
            else -> Aggregation.DAY
        }

        val periodLength = range.second - range.first
        val prevRange = Pair(range.first - periodLength, range.first)

        combine(
            repository.getCategorySpending(range.first, range.second),
            repository.getCategorySpending(prevRange.first, prevRange.second),
            repository.getSpendingTrend(range.first, range.second),
            repository.getTransactions(),
            planningRepository.getBudgetsWithProgress(range.first, range.second)
        ) { categorySpending, prevCategorySpending, spendingTrend, transactions, budgets ->
            val nonTransferTxs = FinancialCalculations.filterNonTransferTransactions(transactions)

            val filteredCurrent = nonTransferTxs.filter { it.timestamp in range.first..range.second }
            val filteredPrev = nonTransferTxs.filter { it.timestamp in prevRange.first..prevRange.second }

            val netCashFlow = FinancialCalculations.calculateNetCashFlow(filteredCurrent)
            val prevNetCashFlow = FinancialCalculations.calculateNetCashFlow(filteredPrev)

            val expenses = filteredCurrent.filter { !it.isIncome && it.type == TransactionType.EXPENSE }
            val prevExpenses = filteredPrev.filter { !it.isIncome && it.type == TransactionType.EXPENSE }

            val spent = netCashFlow.spending
            val income = netCashFlow.income
            val prevSpent = prevNetCashFlow.spending
            val prevIncome = prevNetCashFlow.income

            val highest = expenses.maxByOrNull { it.amount }
            val merchants = expenses.groupBy { it.title }
            val topMerch = merchants.maxByOrNull { it.value.sumOf { t -> t.amount } }?.let { (name, trans) ->
                MerchantInfo(name, trans.sumOf { it.amount }, trans.size)
            }

            val formattedCategoryShares = FinancialCalculations.calculateCategoryShare(categorySpending, spent)
            val categoryTrends = FinancialCalculations.calculateCategoryTrends(categorySpending, prevCategorySpending)
            val anomalies = FinancialCalculations.detectSpendingAnomalies(filteredCurrent, nonTransferTxs)

            val aggregatedTrend = aggregateTrend(range.first, range.second, spendingTrend, aggregation)
            val interactivePoints = aggregatedTrend.map { tp ->
                val dateStr = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(tp.timestamp))
                val dayTxs = expenses.filter { isSameDay(it.timestamp, tp.timestamp) }
                val topCat = dayTxs.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }?.key
                val topTx = dayTxs.maxByOrNull { it.amount }?.title
                InteractiveTrendPoint(
                    timestamp = tp.timestamp,
                    dateLabel = dateStr,
                    amount = tp.amount,
                    topCategory = topCat,
                    topTransactionTitle = topTx
                )
            }

            val daysCount = ((range.second - range.first) / 86400000.0).coerceAtLeast(1.0)
            val (avgLabel, avgAmount) = when (aggregation) {
                Aggregation.DAY -> Pair("Avg/day", spent / daysCount)
                Aggregation.WEEK -> Pair("Avg/week", spent / (daysCount / 7.0).coerceAtLeast(1.0))
                Aggregation.MONTH -> Pair("Avg/month", spent / (daysCount / 30.0).coerceAtLeast(1.0))
                Aggregation.YEAR -> Pair("Avg/year", spent / (daysCount / 365.0).coerceAtLeast(1.0))
            }

            val formattedRangeText = formatDateRangeText(range.first, range.second)

            AnalyticsState(
                netCashFlow = netCashFlow,
                categorySpending = categorySpending,
                formattedCategoryShares = formattedCategoryShares,
                categoryTrends = categoryTrends,
                anomalies = anomalies,
                interactiveTrendPoints = interactivePoints,
                spendingTrend = aggregatedTrend,
                totalSpent = spent,
                totalIncome = income,
                netSavings = netCashFlow.netCashFlow,
                transactionCount = filteredCurrent.size,
                highestExpense = highest,
                topMerchant = topMerch,
                averageLabel = avgLabel,
                averageAmount = avgAmount,
                spentTrendPercent = FinancialCalculations.calculatePercentChange(prevSpent, spent),
                incomeTrendPercent = FinancialCalculations.calculatePercentChange(prevIncome, income),
                savingsTrendPercent = FinancialCalculations.calculatePercentChange(prevNetCashFlow.netCashFlow, netCashFlow.netCashFlow),
                transTrendPercent = FinancialCalculations.calculatePercentChange(prevExpenses.size.toDouble(), expenses.size.toDouble()),
                selectedFilter = query.filter,
                selectedMonth = query.month,
                customRange = query.custom,
                formattedDateRange = formattedRangeText,
                aggregation = aggregation,
                budgets = budgets,
                insights = generateInsights(spent, income, prevSpent, categorySpending, highest),
                isLoading = false
            )
        }.flowOn(Dispatchers.Default)
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsState()
    )

    private data class DataQuery(
        val filter: String,
        val month: Calendar?,
        val custom: Pair<Long, Long>?
    )

    private fun isSameDay(t1: Long, t2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    private fun formatDateRangeText(startMillis: Long, endMillis: Long): String {
        val startCal = Calendar.getInstance().apply { timeInMillis = startMillis }
        val endCal = Calendar.getInstance().apply { timeInMillis = endMillis }

        val sameYear = startCal.get(Calendar.YEAR) == endCal.get(Calendar.YEAR)
        val sameMonth = sameYear && startCal.get(Calendar.MONTH) == endCal.get(Calendar.MONTH)

        return if (sameMonth && startCal.get(Calendar.DAY_OF_MONTH) == 1 && endCal.get(Calendar.DAY_OF_MONTH) == endCal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
            SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(startMillis))
        } else {
            val f1 = SimpleDateFormat("MMM d", Locale.getDefault())
            val f2 = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            "${f1.format(Date(startMillis))} – ${f2.format(Date(endMillis))}"
        }
    }

    private fun aggregateTrend(
        start: Long,
        end: Long,
        trend: List<TrendPoint>,
        agg: Aggregation
    ): List<TrendPoint> {
        val fullTrend = fillMissingDays(start, end, trend)
        return when (agg) {
            Aggregation.DAY -> fullTrend
            Aggregation.WEEK -> {
                fullTrend.groupBy {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.timestamp
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.timeInMillis
                }.map { (time, points) -> TrendPoint(time, points.sumOf { it.amount }) }.sortedBy { it.timestamp }
            }
            Aggregation.MONTH -> {
                fullTrend.groupBy {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.timestamp
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.timeInMillis
                }.map { (time, points) -> TrendPoint(time, points.sumOf { it.amount }) }.sortedBy { it.timestamp }
            }
            Aggregation.YEAR -> {
                fullTrend.groupBy {
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = it.timestamp
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.timeInMillis
                }.map { (time, points) -> TrendPoint(time, points.sumOf { it.amount }) }.sortedBy { it.timestamp }
            }
        }
    }

    private fun generateInsights(
        spent: Double,
        income: Double,
        prevSpent: Double,
        categorySpending: List<CategorySpending>,
        highestExpense: Transaction?
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // Priority 1: Spending Increase Spike
        if (spent > prevSpent && prevSpent > 0) {
            val increase = ((spent - prevSpent) / prevSpent * 100).toInt()
            if (increase > 10) {
                insights.add(
                    Insight(
                        type = InsightType.SPENDING_SPIKE,
                        title = "Spending Increase",
                        description = "Your spending increased by $increase% compared to the previous period.",
                        amount = spent - prevSpent,
                        icon = "📈"
                    )
                )
            }
        }

        // Priority 2: Category Dominance
        var dominantCategory: String? = null
        if (categorySpending.isNotEmpty() && spent > 0) {
            val topCat = categorySpending.maxByOrNull { it.amount }
            if (topCat != null) {
                val pct = ((topCat.amount / spent) * 100).toInt()
                if (pct >= 30) {
                    dominantCategory = topCat.categoryName
                    insights.add(
                        Insight(
                            type = InsightType.CATEGORY_DOMINANCE,
                            title = "${topCat.categoryName} Dominance",
                            description = "${topCat.categoryName} accounts for $pct% of your spending this period.",
                            amount = topCat.amount,
                            icon = "📊",
                            targetCategory = topCat.categoryName
                        )
                    )
                }
            }
        }

        // Priority 3: Largest Single Expense (only if category is not already covered by dominant category)
        if (highestExpense != null && highestExpense.amount > 0 && highestExpense.category != dominantCategory) {
            insights.add(
                Insight(
                    type = InsightType.SPENDING_SPIKE,
                    title = "Largest Expense",
                    description = "Your largest single purchase was ${highestExpense.title} at ₹${String.format(Locale.US, "%,.0f", highestExpense.amount)}.",
                    amount = highestExpense.amount,
                    icon = "💳"
                )
            )
        }

        return insights
    }

    private fun toStartOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun fillMissingDays(start: Long, end: Long, trend: List<TrendPoint>): List<TrendPoint> {
        val result = mutableListOf<TrendPoint>()
        val trendMap = trend.associateBy { toStartOfDay(it.timestamp) }

        val cal = Calendar.getInstance()
        cal.timeInMillis = toStartOfDay(start)
        val endDay = toStartOfDay(end)

        while (cal.timeInMillis <= endDay) {
            val dayStart = cal.timeInMillis
            result.add(TrendPoint(dayStart, trendMap[dayStart]?.amount ?: 0.0))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }

    fun onFilterSelected(filter: String) {
        _selectedMonth.value = null
        _customRange.value = null
        _selectedFilter.value = filter
    }

    fun onCustomRangeSelected(start: Long, end: Long) {
        _selectedMonth.value = null
        _selectedFilter.value = "Custom"
        _customRange.value = Pair(start, end)
    }

    fun onMonthSelected(calendar: Calendar) {
        _customRange.value = null
        _selectedMonth.value = calendar
    }

    fun onClearMonth() {
        _selectedMonth.value = null
        _customRange.value = null
    }

    private fun getRangeForFilter(filter: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val end = calendar.timeInMillis

        return when (filter) {
            "1D" -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            "1W" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            "1M" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            "3M" -> {
                calendar.add(Calendar.MONTH, -3)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            "6M" -> {
                calendar.add(Calendar.MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            "1Y" -> {
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
            else -> {
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                Pair(calendar.timeInMillis, end)
            }
        }
    }
}
