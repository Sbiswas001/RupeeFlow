package sayan.apps.rupeeflow.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.model.Transaction
import java.util.*
import javax.inject.Inject

enum class ReportType {
    OVERVIEW, SPENDING, INCOME, BUDGET, ACCOUNTS, MERCHANTS, CATEGORIES
}

data class ReportSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val savings: Double = 0.0,
    val netCashFlow: Double = 0.0
)

data class MonthlyComparison(
    val incomeChange: Double = 0.0,
    val prevIncome: Double = 0.0,
    val currentIncome: Double = 0.0,
    val expenseChange: Double = 0.0,
    val prevExpense: Double = 0.0,
    val currentExpense: Double = 0.0,
    val savingsChange: Double = 0.0,
    val prevSavings: Double = 0.0,
    val currentSavings: Double = 0.0
)

data class MerchantSpending(
    val merchantName: String,
    val amount: Double,
    val transactionCount: Int
)

data class DaySpending(
    val day: Int,
    val amount: Double,
    val topTransactions: List<Transaction> = emptyList()
)

data class ReportHighlights(
    val largestExpense: Transaction? = null,
    val highestIncome: Transaction? = null,
    val mostUsedCategory: String = "N/A",
    val mostUsedAccount: String = "N/A"
)

data class ReportsState(
    val selectedReportType: ReportType = ReportType.OVERVIEW,
    val selectedMonth: Calendar = Calendar.getInstance(),
    val summary: ReportSummary = ReportSummary(),
    val comparison: MonthlyComparison = MonthlyComparison(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val merchantSpending: List<MerchantSpending> = emptyList(),
    val dailySpending: List<DaySpending> = emptyList(),
    val highlights: ReportHighlights = ReportHighlights(),
    val monthlyStory: List<String> = emptyList(),
    val timeline: List<Transaction> = emptyList(),
    val financialScore: Int = 0,
    val scoreReasons: List<String> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val planningRepository: PlanningRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    private val _selectedReportType = MutableStateFlow(ReportType.OVERVIEW)
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReportsState> = combine(
        _selectedReportType,
        _selectedMonth
    ) { type, month ->
        Pair(type, month)
    }.flatMapLatest { (type, month) ->
        val range = getMonthRange(month)
        val prevRange = getPrevMonthRange(month)

        combine(
            transactionRepository.getTransactions(),
            planningRepository.getBudgetsWithProgress(),
            transactionRepository.getCategorySpending(range.first, range.second)
        ) { transactions, budgets, categorySpending ->
            val currentTrans = transactions.filter { it.timestamp in range.first..range.second }
            val prevTrans = transactions.filter { it.timestamp in prevRange.first..prevRange.second }

            val expenses = currentTrans.filter { !it.isIncome }
            val income = currentTrans.filter { it.isIncome }.sumOf { it.amount }
            val expense = expenses.sumOf { it.amount }
            val savings = income - expense

            val prevIncome = prevTrans.filter { it.isIncome }.sumOf { it.amount }
            val prevExpense = prevTrans.filter { !it.isIncome }.sumOf { it.amount }
            val prevSavings = prevIncome - prevExpense

            val comparison = MonthlyComparison(
                incomeChange = calculatePercentChange(prevIncome, income),
                prevIncome = prevIncome,
                currentIncome = income,
                expenseChange = calculatePercentChange(prevExpense, expense),
                prevExpense = prevExpense,
                currentExpense = expense,
                savingsChange = calculatePercentChange(prevSavings, savings),
                prevSavings = prevSavings,
                currentSavings = savings
            )

            val highestExpense = expenses.maxByOrNull { it.amount }
            val highestIncome = currentTrans.filter { it.isIncome }.maxByOrNull { it.amount }
            val mostUsedCategory = expenses.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "N/A"
            
            val merchants = expenses.groupBy { it.title }
            val merchantSpending = merchants.map { (name, trans) ->
                MerchantSpending(
                    merchantName = name,
                    amount = trans.sumOf { it.amount },
                    transactionCount = trans.size
                )
            }.sortedByDescending { it.amount }

            val dailySpending = expenses.groupBy { 
                val cal = Calendar.getInstance()
                cal.timeInMillis = it.timestamp
                cal.get(Calendar.DAY_OF_MONTH)
            }.map { (day, trans) ->
                DaySpending(day, trans.sumOf { it.amount }, trans.sortedByDescending { it.amount }.take(3))
            }

            // Calculate Financial Score & Reasons
            val scoreReasons = mutableListOf<String>()
            val budgetAdherence = if (budgets.isNotEmpty()) budgets.count { it.progress <= 1.0f }.toDouble() / budgets.size else 1.0
            if (budgetAdherence >= 0.8) scoreReasons.add("Excellent budget adherence.")
            else if (budgetAdherence < 0.5) scoreReasons.add("Budget exceeded in multiple categories.")
            
            val savingsRate = if (income > 0) (savings / income).coerceIn(0.0, 1.0) else 0.0
            if (savingsRate >= 0.2) scoreReasons.add("Savings rate is healthy.")
            else if (income > 0) scoreReasons.add("Try to save at least 20% of your income.")

            val expenseTrend = if (comparison.expenseChange <= 0) 1.0 else (1.0 - (comparison.expenseChange / 100.0)).coerceIn(0.0, 1.0)
            if (comparison.expenseChange < 0) scoreReasons.add("Spending decreased compared to last month.")

            val score = (budgetAdherence * 40 + savingsRate * 30 + expenseTrend * 30).toInt()

            // Monthly Story
            val story = mutableListOf<String>()
            story.add("You earned ${formatCurrencySimple(income)}.")
            story.add("You spent only ${formatCurrencySimple(expense)}.")
            if (income > 0) {
                val rate = (savings / income * 100)
                story.add("You saved ${String.format(Locale.getDefault(), "%.2f", rate)}% of your income.")
            }
            if (expenses.isNotEmpty()) {
                val topCat = expenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }
                val catPercent = (topCat?.value?.sumOf { it.amount } ?: 0.0) / expense * 100
                story.add("${topCat?.key} accounted for ${catPercent.toInt()}% of your expenses.")
            }
            if (highestExpense != null) {
                story.add("Your largest purchase was ${highestExpense.title} (${formatCurrencySimple(highestExpense.amount)}).")
            }
            if (comparison.savingsChange > 0) {
                story.add("Compared to last month, your savings increased by ${comparison.savingsChange.toInt()}%.")
            }

            ReportsState(
                selectedReportType = type,
                selectedMonth = month,
                summary = ReportSummary(income, expense, savings, savings),
                comparison = comparison,
                categorySpending = categorySpending.sortedByDescending { it.amount },
                merchantSpending = merchantSpending,
                dailySpending = dailySpending,
                highlights = ReportHighlights(
                    largestExpense = highestExpense,
                    highestIncome = highestIncome,
                    mostUsedCategory = mostUsedCategory,
                    mostUsedAccount = "Primary" // Placeholder
                ),
                monthlyStory = story,
                timeline = currentTrans.sortedByDescending { it.timestamp }.take(10),
                financialScore = score,
                scoreReasons = scoreReasons,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsState()
    )

    private fun formatCurrencySimple(amount: Double): String {
        return "₹${String.format(Locale.getDefault(), "%,.0f", amount)}"
    }

    private fun getMonthRange(calendar: Calendar): Pair<Long, Long> {
        val start = calendar.clone() as Calendar
        start.set(Calendar.DAY_OF_MONTH, 1)
        start.set(Calendar.HOUR_OF_DAY, 0)
        start.set(Calendar.MINUTE, 0)
        start.set(Calendar.SECOND, 0)
        start.set(Calendar.MILLISECOND, 0)

        val end = calendar.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        end.set(Calendar.HOUR_OF_DAY, 23)
        end.set(Calendar.MINUTE, 59)
        end.set(Calendar.SECOND, 59)
        end.set(Calendar.MILLISECOND, 999)

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    private fun getPrevMonthRange(calendar: Calendar): Pair<Long, Long> {
        val prev = calendar.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        return getMonthRange(prev)
    }

    private fun calculatePercentChange(old: Double, new: Double): Double {
        if (old == 0.0) return if (new > 0.0) 100.0 else 0.0
        return ((new - old) / old) * 100.0
    }

    fun onReportTypeSelected(type: ReportType) {
        _selectedReportType.value = type
    }

    fun onMonthSelected(month: Calendar) {
        _selectedMonth.value = month
    }
}
