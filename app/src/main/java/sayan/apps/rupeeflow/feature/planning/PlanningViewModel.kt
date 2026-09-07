package sayan.apps.rupeeflow.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.financial.BudgetUsageResult
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.ForecastResult
import sayan.apps.rupeeflow.core.financial.SafeToSpendResult
import sayan.apps.rupeeflow.core.financial.SpendingForecastCalculator
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Goal
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import java.util.*
import javax.inject.Inject

data class PlanningState(
    val safeToSpendResult: SafeToSpendResult = SafeToSpendResult(0.0, 0.0, 0.0, 0, 0.0, false, "Set a monthly budget to calculate your safe spending limit.", ""),
    val budgetUsageResult: BudgetUsageResult = BudgetUsageResult(0.0, 0.0, 0.0, 0),
    val forecastResult: ForecastResult = ForecastResult(0.0, 0.0, 0.0, 0.0, 0.0, false),
    val budgets: List<BudgetWithProgress> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recurringItems: List<RecurringItem> = emptyList(),
    val insights: List<PlanningInsight> = emptyList(),
    val selectedMonth: Calendar = Calendar.getInstance(),
    val isLoading: Boolean = true
)

data class PlanningInsight(
    val message: String,
    val type: InsightType
)

enum class InsightType {
    INFO, SUCCESS, WARNING, ALERT
}

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val repository: PlanningRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow<Calendar>(Calendar.getInstance())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanningState> = _selectedMonth.flatMapLatest { month ->
        val range = getMonthRange(month)
        combine(
            repository.getBudgetsWithProgress(range.first, range.second),
            repository.getGoals(),
            categoryRepository.getCategories(),
            recurringRepository.getRecurringItems(),
            combine(accountRepository.getAccounts(), transactionRepository.getTransactions()) { accounts, transactions ->
                Pair(accounts, transactions)
            }
        ) { budgets, goals, categories, recurring, (accounts, transactions) ->
            val now = Calendar.getInstance()
            val isCurrentMonth = month.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                    month.get(Calendar.MONTH) == now.get(Calendar.MONTH)
            val isPastMonth = (month.get(Calendar.YEAR) < now.get(Calendar.YEAR)) ||
                    (month.get(Calendar.YEAR) == now.get(Calendar.YEAR) && month.get(Calendar.MONTH) < now.get(Calendar.MONTH))

            val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
            val daysPassed = if (isCurrentMonth) now.get(Calendar.DAY_OF_MONTH) else if (isPastMonth) daysInMonth else 1
            val daysLeft = when {
                isPastMonth -> 0
                isCurrentMonth -> (daysInMonth - now.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
                else -> daysInMonth
            }

            // A recurring item's status describes its template (ACTIVE/PAUSED); a
            // payment's status lives on its occurrence. Only unpaid occurrences in
            // the selected month belong in the forecast.
            val forecastStart = maxOf(System.currentTimeMillis(), range.first)
            val upcomingRecurring = recurring.filter { it.status == "ACTIVE" }
            val upcomingBills = upcomingRecurring.flatMap { item ->
                item.occurrences.asSequence()
                    .filter { occurrence ->
                        occurrence.status == "PENDING" && occurrence.scheduledDate in forecastStart..range.second
                    }
                    .map { item to it }
                    .toList()
            }
            val recurringTotal = upcomingBills.sumOf { (item, occurrence) -> occurrence.amount ?: item.amount }

            val safeToSpend = SpendingForecastCalculator.calculateSafeToSpend(
                budgets = budgets,
                upcomingBills = recurringTotal,
                plannedSavings = 0.0,
                daysRemaining = daysLeft,
                upcomingBillsAlreadyInBudget = false,
                totalDaysInMonth = daysInMonth
            )

            val budgetUsage = SpendingForecastCalculator.calculateBudgetUsage(budgets)

            val nonTransferTxs = FinancialCalculations.filterNonTransferTransactions(transactions)
            val monthTransactions = nonTransferTxs.filter { it.timestamp in range.first..range.second }
            val currentSpending = monthTransactions.filter { !it.isIncome && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val budgetedCurrentSpending = budgets.sumOf { it.budget.spentAmount }

            val currentBalance = accounts.sumOf { it.balance }

            val forecast = SpendingForecastCalculator.calculateSpendingForecast(
                currentBalance = currentBalance,
                // Recorded income has already changed account balances. Recurring
                // income is not modelled yet, so do not count recorded income again.
                expectedIncome = 0.0,
                upcomingBills = recurringTotal,
                currentMonthSpending = currentSpending,
                daysPassed = daysPassed,
                totalDaysInMonth = daysInMonth,
                budgetLimit = budgetUsage.totalBudgetedLimit,
                budgetedCurrentSpending = budgetedCurrentSpending
            )

            PlanningState(
                safeToSpendResult = safeToSpend,
                budgetUsageResult = budgetUsage,
                forecastResult = forecast,
                budgets = budgets,
                goals = goals,
                categories = categories.filter { it.type == TransactionType.EXPENSE && !it.isDeleted },
                recurringItems = upcomingBills.map { (item, occurrence) -> item.copy(dueDate = occurrence.scheduledDate) }
                    .sortedBy { it.dueDate }.take(5),
                insights = generateInsights(budgets, upcomingBills.map { it.first }, forecast),
                selectedMonth = month,
                isLoading = false
            )
        }.flowOn(Dispatchers.Default)
    }.flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanningState()
    )

    private fun generateInsights(
        budgets: List<BudgetWithProgress>,
        recurring: List<RecurringItem>,
        forecast: ForecastResult
    ): List<PlanningInsight> {
        val insights = mutableListOf<PlanningInsight>()

        if (forecast.isPacingOverBudget) {
            insights.add(PlanningInsight(forecast.statusMessage, InsightType.ALERT))
        }

        if (forecast.isProjectedDeficit) {
            insights.add(PlanningInsight("Your projected month-end balance is negative.", InsightType.ALERT))
        }

        budgets.forEach {
            if (it.progress > 0.9f) {
                insights.add(PlanningInsight("${it.budget.categoryName} budget is almost exhausted.", InsightType.WARNING))
            }
        }

        val nextBill = recurring.minByOrNull { it.dueDate }
        nextBill?.let {
            val daysToBill = ((it.dueDate - System.currentTimeMillis()) / 86400000).toInt()
            if (daysToBill in 0..3) {
                insights.add(PlanningInsight("${it.title} bill due in $daysToBill days.", InsightType.ALERT))
            }
        }

        return insights
    }

    fun onMonthSelected(month: Calendar) {
        _selectedMonth.value = month
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

    fun addBudget(categoryId: Long, amount: Double, period: String) {
        viewModelScope.launch {
            repository.addBudget(
                Budget(
                    id = 0,
                    categoryId = categoryId,
                    categoryName = "",
                    limitAmount = amount,
                    period = period,
                    startDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun updateBudget(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget)
        }
    }

    fun resetBudgetProgress(budget: Budget) {
        viewModelScope.launch {
            repository.updateBudget(budget.copy(startDate = System.currentTimeMillis()))
        }
    }

    fun addGoal(title: String, targetAmount: Double, startingAmount: Double = 0.0, targetDate: Long? = null) {
        viewModelScope.launch {
            repository.addGoal(
                Goal(
                    id = 0,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = startingAmount,
                    targetDate = targetDate
                )
            )
        }
    }

    fun addGoalContribution(goal: Goal, contributionAmount: Double, note: String? = null) {
        viewModelScope.launch {
            val cappedAmount = if (goal.remainingAmount > 0) contributionAmount.coerceAtMost(goal.remainingAmount) else contributionAmount
            repository.addGoalContribution(goal.id, cappedAmount, note)
        }
    }

    fun deleteGoalContribution(goalId: Long, contributionId: Long, contributionAmount: Double) {
        viewModelScope.launch {
            repository.deleteGoalContribution(goalId, contributionId, contributionAmount)
        }
    }

    fun getGoalContributions(goalId: Long) = repository.getGoalContributions(goalId)

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }
}
