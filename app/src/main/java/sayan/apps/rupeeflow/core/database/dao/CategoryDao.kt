package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteCategory(id: Long)

    @Query("SELECT * FROM categories WHERE isDeleted = 0 AND name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM transactions WHERE categoryId = :categoryId)")
    suspend fun isCategoryInUse(categoryId: Long): Boolean

    @Query("UPDATE transactions SET categoryId = :targetCategoryId WHERE categoryId = :sourceCategoryId")
    suspend fun reassignTransactions(sourceCategoryId: Long, targetCategoryId: Long?)

    @Query("DELETE FROM categories")
    suspend fun clearAllCategories()
}
