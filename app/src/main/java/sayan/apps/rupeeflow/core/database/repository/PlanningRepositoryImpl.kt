package sayan.apps.rupeeflow.core.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.database.dao.CategoryDao
import sayan.apps.rupeeflow.core.database.dao.PlanningDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.entity.BudgetPeriod
import sayan.apps.rupeeflow.core.database.entity.GoalContributionEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Goal
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import javax.inject.Inject

class PlanningRepositoryImpl @Inject constructor(
    private val planningDao: PlanningDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao
) : PlanningRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getBudgetsWithProgress(start: Long?, end: Long?): Flow<List<BudgetWithProgress>> {
        return categoryDao.getAllCategories().flatMapLatest { categories ->
            val budgetsTableFlow = planningDao.getAllBudgets()

            combine(budgetsTableFlow, flowOf(categories)) { budgetEntities, allCats ->
                val tableBudgetsMap = budgetEntities.associateBy { it.categoryId }

                // Only consider expense categories that have a budget (either in CategoryEntity or BudgetEntity)
                allCats.filter { (it.budget != null || tableBudgetsMap.containsKey(it.id)) && it.type == TransactionType.EXPENSE && !it.isDeleted }
                    .map { category ->
                        val tableBudget = tableBudgetsMap[category.id]
                        val limit = category.budget ?: tableBudget?.limitAmount ?: 0.0
                        val period = tableBudget?.period ?: BudgetPeriod.MONTHLY
                        val budgetStartDate = tableBudget?.startDate ?: 0L

                        // Effective range for this budget
                        val effectiveStart = start ?: budgetStartDate
                        val effectiveEnd = end ?: Long.MAX_VALUE

                        category.id to Quintuple(limit, period, budgetStartDate, category, effectiveStart to effectiveEnd)
                    }
            }.flatMapLatest { budgetDataList ->
                if (budgetDataList.isEmpty()) return@flatMapLatest flowOf(emptyList())

                val progressFlows = budgetDataList.map { (categoryId, data) ->
                    val (limit, period, budgetStartDate, category, range) = data
                    transactionDao.getCategorySpendingInRangeFlow(categoryId, range.first, range.second).map { spent ->
                        val actualSpent = Math.abs(spent ?: 0.0)
                        val budget = Budget(
                            id = 0,
                            categoryId = categoryId,
                            categoryName = category.name,
                            categoryIcon = category.icon,
                            limitAmount = limit,
                            spentAmount = actualSpent,
                            period = period.name,
                            startDate = budgetStartDate
                        )
                        BudgetWithProgress(
                            budget = budget,
                            progress = if (limit > 0) (actualSpent / limit).toFloat() else 0f,
                            remaining = limit - actualSpent
                        )
                    }
                }
                combine(progressFlows) { it.toList() }
            }
        }.flowOn(Dispatchers.Default)
    }

    override fun getGoals(): Flow<List<Goal>> {
        return planningDao.getAllGoals().map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun addBudget(budget: Budget) {
        planningDao.insertBudget(budget.toEntity())
        // Sync with Category
        val category = categoryDao.getCategoryById(budget.categoryId)
        if (category != null) {
            categoryDao.updateCategory(category.copy(budget = budget.limitAmount))
        }
    }

    override suspend fun updateBudget(budget: Budget) {
        planningDao.insertBudget(budget.toEntity())
        // Sync with Category
        val category = categoryDao.getCategoryById(budget.categoryId)
        if (category != null) {
            categoryDao.updateCategory(category.copy(budget = budget.limitAmount))
        }
    }

    override suspend fun deleteBudget(budget: Budget) {
        planningDao.deleteBudget(budget.toEntity())
        // Sync with Category
        val category = categoryDao.getCategoryById(budget.categoryId)
        if (category != null) {
            categoryDao.updateCategory(category.copy(budget = null))
        }
    }

    override suspend fun addGoal(goal: Goal) {
        planningDao.insertGoal(goal.toEntity())
    }

    override suspend fun updateGoal(goal: Goal) {
        planningDao.insertGoal(goal.toEntity())
    }

    override suspend fun deleteGoal(goal: Goal) {
        planningDao.deleteGoal(goal.toEntity())
    }

    override suspend fun addGoalContribution(goalId: Long, amount: Double, note: String?) {
        planningDao.insertGoalContribution(
            GoalContributionEntity(
                goalId = goalId,
                amount = amount,
                note = note
            )
        )
        val goalEntity = planningDao.getAllGoalsSync().find { it.id == goalId }
        if (goalEntity != null) {
            val updated = goalEntity.copy(currentAmount = goalEntity.currentAmount + amount)
            planningDao.updateGoal(updated)
        }
    }

    override suspend fun deleteGoalContribution(goalId: Long, contributionId: Long, contributionAmount: Double) {
        planningDao.deleteGoalContributionById(contributionId)
        val goalEntity = planningDao.getAllGoalsSync().find { it.id == goalId }
        if (goalEntity != null) {
            val updated = goalEntity.copy(
                currentAmount = (goalEntity.currentAmount - contributionAmount).coerceAtLeast(0.0)
            )
            planningDao.updateGoal(updated)
        }
    }

    override fun getGoalContributions(goalId: Long): Flow<List<GoalContributionEntity>> {
        return planningDao.getContributionsForGoal(goalId)
    }

    override fun getSafeSpendingLimit(): Flow<Double> {
        return getBudgetsWithProgress().map { budgets ->
            val totalRemaining = budgets.sumOf { it.remaining }
            val daysLeft = DateUtils.getDaysRemainingInMonth()
            if (daysLeft > 0) totalRemaining / daysLeft else 0.0
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
