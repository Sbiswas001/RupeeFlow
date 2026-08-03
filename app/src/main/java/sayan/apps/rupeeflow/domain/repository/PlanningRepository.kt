package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Goal

interface PlanningRepository {
    fun getBudgetsWithProgress(): Flow<List<BudgetWithProgress>>
    fun getGoals(): Flow<List<Goal>>
    
    suspend fun addBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    
    suspend fun addGoal(goal: Goal)
    suspend fun updateGoal(goal: Goal)
    suspend fun deleteGoal(goal: Goal)

    fun getSafeSpendingLimit(): Flow<Double>
}
