package sayan.apps.rupeeflow.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Goal
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import java.util.*
import javax.inject.Inject

data class ForecastData(
    val currentBalance: Double = 0.0,
    val expectedIncome: Double = 0.0,
    val upcomingBills: Double = 0.0,
    val estimatedMonthEnd: Double = 0.0
)

data class PlanningState(
    val budgets: List<BudgetWithProgress> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recurringItems: List<RecurringItem> = emptyList(),
    val safeSpendingLimit: Double = 0.0,
    val remainingMonthlyBudget: Double = 0.0,
    val daysLeftInMonth: Int = 0,
    val dailyTarget: Double = 0.0,
    val totalBudgetUsedPercent: Int = 0,
    val totalRecurringBills: Double = 0.0,
    val forecast: ForecastData = ForecastData(),
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
    private val recurringRepository: RecurringRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow<Calendar>(Calendar.getInstance())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<PlanningState> = _selectedMonth.flatMapLatest { month ->
        combine(
            repository.getBudgetsWithProgress(),
            repository.getGoals(),
            categoryRepository.getCategories(),
            recurringRepository.getRecurringItems(),
            accountRepository.getAccounts()
        ) { budgets, goals, categories, recurring, accounts ->
            val now = Calendar.getInstance()
            val totalRemaining = budgets.sumOf { it.remaining }
            val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
            val currentDay = now.get(Calendar.DAY_OF_MONTH)
            val daysLeft = (daysInMonth - currentDay + 1).coerceAtLeast(1)
            
            val safeToday = totalRemaining / daysLeft
            
            val totalBudget = budgets.sumOf { it.budget.limitAmount }
            val totalSpent = budgets.sumOf { it.budget.spentAmount }
            val budgetUsedPercent = if (totalBudget > 0) (totalSpent / totalBudget * 100).toInt() else 0
            
            val upcomingRecurring = recurring.filter { it.status == "PENDING" }
            val recurringTotal = upcomingRecurring.sumOf { it.amount }
            
            val currentBalance = accounts.sumOf { it.balance }
            val expectedIncome = 58000.0 // Mock or from a setting
            val forecast = ForecastData(
                currentBalance = currentBalance,
                expectedIncome = expectedIncome,
                upcomingBills = recurringTotal,
                estimatedMonthEnd = currentBalance + expectedIncome - recurringTotal - totalRemaining
            )

            PlanningState(
                budgets = budgets,
                goals = goals,
                categories = categories,
                recurringItems = upcomingRecurring.sortedBy { it.dueDate }.take(3),
                safeSpendingLimit = safeToday,
                remainingMonthlyBudget = totalRemaining,
                daysLeftInMonth = daysLeft,
                dailyTarget = if (daysLeft > 0) totalRemaining / daysLeft else 0.0,
                totalBudgetUsedPercent = budgetUsedPercent,
                totalRecurringBills = recurringTotal,
                forecast = forecast,
                insights = generateInsights(budgets, upcomingRecurring),
                selectedMonth = month,
                isLoading = false
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlanningState()
    )

    private fun generateInsights(budgets: List<BudgetWithProgress>, recurring: List<RecurringItem>): List<PlanningInsight> {
        val insights = mutableListOf<PlanningInsight>()
        
        budgets.forEach { 
            if (it.progress > 0.9f) {
                insights.add(PlanningInsight("${it.budget.categoryName} budget is almost exhausted.", InsightType.WARNING))
            } else if (it.progress < 0.5f && it.budget.spentAmount > 0) {
                insights.add(PlanningInsight("You are within your ${it.budget.categoryName} budget.", InsightType.SUCCESS))
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

    fun addGoal(title: String, targetAmount: Double, currentAmount: Double) {
        viewModelScope.launch {
            repository.addGoal(
                Goal(
                    id = 0,
                    title = title,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDate = null
                )
            )
        }
    }

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
