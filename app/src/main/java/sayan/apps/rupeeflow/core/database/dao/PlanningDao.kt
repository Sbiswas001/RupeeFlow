package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.BudgetEntity
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
}
