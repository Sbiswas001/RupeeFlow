package sayan.apps.rupeeflow.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.FinancialHealthCalculator
import sayan.apps.rupeeflow.core.financial.FinancialHealthResult
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

enum class ReportType {
    OVERVIEW, SPENDING, INCOME, BUDGET, ACCOUNTS, CATEGORIES
}

data class ReportSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val netCashFlow: Double = 0.0,
    val transactionCount: Int = 0
)

data class MonthlyComparison(
    val incomeChange: Double? = null,
    val prevIncome: Double = 0.0,
    val currentIncome: Double = 0.0,
    val expenseChange: Double? = null,
    val prevExpense: Double = 0.0,
    val currentExpense: Double = 0.0,
    val netCashFlowChange: Double? = null,
    val prevNetCashFlow: Double = 0.0,
    val currentNetCashFlow: Double = 0.0
)

data class SpendingEntityBreakdown(
    val title: String,
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
    val formattedMonthLabel: String = "",
    val summary: ReportSummary = ReportSummary(),
    val comparison: MonthlyComparison = MonthlyComparison(),
    val categorySpending: List<CategorySpending> = emptyList(),
    val incomeSpending: List<CategorySpending> = emptyList(),
    val spendingBreakdown: List<SpendingEntityBreakdown> = emptyList(),
    val dailySpending: List<DaySpending> = emptyList(),
    val highlights: ReportHighlights = ReportHighlights(),
    val monthlyStory: List<String> = emptyList(),
    val timeline: List<Transaction> = emptyList(),
    val healthResult: FinancialHealthResult = FinancialHealthResult(null, "Not enough data yet", emptyList(), false, emptyList()),
    val budgets: List<BudgetWithProgress> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val exportSummaryText: String = "",
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
            planningRepository.getBudgetsWithProgress(range.first, range.second),
            transactionRepository.getCategorySpending(range.first, range.second),
            accountRepository.getAccounts(),
            recurringRepository.getRecurringItems()
        ) { transactions, budgets, categorySpending, accounts, recurring ->
            val nonTransferTxs = FinancialCalculations.filterNonTransferTransactions(transactions)

            val currentTrans = nonTransferTxs.filter { it.timestamp in range.first..range.second }
            val prevTrans = nonTransferTxs.filter { it.timestamp in prevRange.first..prevRange.second }

            val currentNet = FinancialCalculations.calculateNetCashFlow(currentTrans)
            val prevNet = FinancialCalculations.calculateNetCashFlow(prevTrans)

            val incomeTrans = currentTrans.filter { it.isIncome || it.type == TransactionType.INCOME }
            val incomeSpending = incomeTrans.groupBy { it.category }.map { (name, list) ->
                CategorySpending(
                    categoryName = name,
                    colorHex = "#10B981",
                    amount = list.sumOf { it.amount }
                )
            }.sortedByDescending { it.amount }

            val expenses = currentTrans.filter { !it.isIncome && it.type == TransactionType.EXPENSE }

            val comparison = MonthlyComparison(
                incomeChange = FinancialCalculations.calculatePercentChange(prevNet.income, currentNet.income),
                prevIncome = prevNet.income,
                currentIncome = currentNet.income,
                expenseChange = FinancialCalculations.calculatePercentChange(prevNet.spending, currentNet.spending),
                prevExpense = prevNet.spending,
                currentExpense = currentNet.spending,
                netCashFlowChange = FinancialCalculations.calculatePercentChange(prevNet.netCashFlow, currentNet.netCashFlow),
                prevNetCashFlow = prevNet.netCashFlow,
                currentNetCashFlow = currentNet.netCashFlow
            )

            val highestExpense = expenses.maxByOrNull { it.amount }
            val highestIncome = incomeTrans.maxByOrNull { it.amount }
            val mostUsedCategory = expenses.groupBy { it.category }.maxByOrNull { it.value.size }?.key ?: "N/A"

            val titles = expenses.groupBy { it.title }
            val spendingBreakdown = titles.map { (name, trans) ->
                SpendingEntityBreakdown(
                    title = name,
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

            val recurringTotal = recurring.filter { it.status == "PENDING" }.sumOf { it.amount }
            val healthResult = FinancialHealthCalculator.calculateFinancialHealth(
                income = currentNet.income,
                expense = currentNet.spending,
                budgets = budgets,
                recurringTotal = recurringTotal,
                transactionCount = currentTrans.size,
                allTransactions = nonTransferTxs
            )

            val monthName = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)

            // Data-driven Natural Monthly Review Story
            val story = mutableListOf<String>()
            story.add("$monthName in review: You earned ₹${formatAmountSimple(currentNet.income)} and spent ₹${formatAmountSimple(currentNet.spending)}.")
            if (currentNet.netCashFlow < 0) {
                story.add("Your net cash flow was -₹${formatAmountSimple(abs(currentNet.netCashFlow))}.")
            } else if (currentNet.netCashFlow > 0) {
                story.add("Your net cash flow was +₹${formatAmountSimple(currentNet.netCashFlow)}.")
            }

            if (expenses.isNotEmpty() && currentNet.spending > 0) {
                val topCat = expenses.groupBy { it.category }.maxByOrNull { it.value.sumOf { t -> t.amount } }
                if (topCat != null) {
                    val catPercent = ((topCat.value.sumOf { it.amount } / currentNet.spending) * 100).toInt()
                    story.add("${topCat.key} accounted for $catPercent% of your expenses.")
                }
            }

            if (highestExpense != null) {
                story.add("Your largest expense was ${highestExpense.title} at ₹${formatAmountSimple(highestExpense.amount)}.")
            }

            val exportText = buildString {
                appendLine("RupeeFlow Report - $monthName")
                appendLine("==========================================")
                appendLine("Total Income: ₹${formatAmountSimple(currentNet.income)}")
                appendLine("Total Expense: ₹${formatAmountSimple(currentNet.spending)}")
                appendLine("Net Cash Flow: ₹${formatAmountSimple(currentNet.netCashFlow)}")
                appendLine("Transactions: ${currentTrans.size}")
                if (highestExpense != null) {
                    appendLine("Largest Expense: ${highestExpense.title} (₹${formatAmountSimple(highestExpense.amount)})")
                }
                appendLine("==========================================")
            }

            ReportsState(
                selectedReportType = type,
                selectedMonth = month,
                formattedMonthLabel = monthName,
                summary = ReportSummary(currentNet.income, currentNet.spending, currentNet.netCashFlow, currentTrans.size),
                comparison = comparison,
                categorySpending = categorySpending.sortedByDescending { it.amount },
                incomeSpending = incomeSpending,
                spendingBreakdown = spendingBreakdown,
                dailySpending = dailySpending,
                highlights = ReportHighlights(
                    largestExpense = highestExpense,
                    highestIncome = highestIncome,
                    mostUsedCategory = mostUsedCategory,
                    mostUsedAccount = accounts.maxByOrNull { it.balance }?.name ?: "N/A"
                ),
                monthlyStory = story,
                timeline = currentTrans.sortedByDescending { it.timestamp },
                healthResult = healthResult,
                budgets = budgets,
                accounts = accounts,
                exportSummaryText = exportText,
                isLoading = false
            )
        }.flowOn(Dispatchers.Default)
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsState()
    )

    private fun formatAmountSimple(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
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

    fun onReportTypeSelected(type: ReportType) {
        _selectedReportType.value = type
    }

    fun onMonthSelected(month: Calendar) {
        _selectedMonth.value = month
    }

    fun onPreviousMonth() {
        val prev = _selectedMonth.value.clone() as Calendar
        prev.add(Calendar.MONTH, -1)
        _selectedMonth.value = prev
    }

    fun onNextMonth() {
        val next = _selectedMonth.value.clone() as Calendar
        next.add(Calendar.MONTH, 1)
        _selectedMonth.value = next
    }
}
