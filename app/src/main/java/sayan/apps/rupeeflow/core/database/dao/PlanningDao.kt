package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.BudgetEntity
import sayan.apps.rupeeflow.core.database.entity.GoalContributionEntity
import sayan.apps.rupeeflow.core.database.entity.GoalEntity

@Dao
interface PlanningDao {
    // Budgets
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE categoryId = :categoryId")
    suspend fun deleteBudgetByCategoryId(categoryId: Long)

    @Query("""
        SELECT b.* FROM budgets b
        INNER JOIN categories c ON b.categoryId = c.id
        WHERE c.name LIKE '%' || :query || '%'
    """)
    fun searchBudgets(query: String): Flow<List<BudgetEntity>>

    // Goals
    @Query("SELECT * FROM goals")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("SELECT * FROM goals WHERE title LIKE '%' || :query || '%'")
    fun searchGoals(query: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM budgets")
    suspend fun getAllBudgetsSync(): List<BudgetEntity>

    @Query("SELECT * FROM goals")
    suspend fun getAllGoalsSync(): List<GoalEntity>

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM budgets")
    suspend fun clearAllBudgets()

    @Query("DELETE FROM goals")
    suspend fun clearAllGoals()

    // Goal Contributions
    @Query("SELECT * FROM goal_contributions WHERE goalId = :goalId ORDER BY createdAt DESC")
    fun getContributionsForGoal(goalId: Long): Flow<List<GoalContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalContribution(contribution: GoalContributionEntity): Long

    @Query("DELETE FROM goal_contributions WHERE id = :id")
    suspend fun deleteGoalContributionById(id: Long)

    @Query("SELECT SUM(amount) FROM goal_contributions WHERE goalId = :goalId")
    fun getTotalContributionsForGoal(goalId: Long): Flow<Double?>
}
