package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.GoalContributionEntity
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Goal

interface PlanningRepository {
    fun getBudgetsWithProgress(start: Long? = null, end: Long? = null): Flow<List<BudgetWithProgress>>
    fun getGoals(): Flow<List<Goal>>

    suspend fun addBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)

    suspend fun addGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)

    suspend fun addGoalContribution(goalId: Long, amount: Double, note: String? = null)
    suspend fun deleteGoalContribution(goalId: Long, contributionId: Long, contributionAmount: Double)
    fun getGoalContributions(goalId: Long): Flow<List<GoalContributionEntity>>

    fun getSafeSpendingLimit(): Flow<Double>
}
