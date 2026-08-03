package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.CategoryEntity

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("UPDATE categories SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("SELECT * FROM categories WHERE name LIKE '%' || :query || '%'")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>
}
