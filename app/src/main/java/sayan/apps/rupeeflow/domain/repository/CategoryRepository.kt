package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Category

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    fun getActiveCategories(): Flow<List<Category>>
    fun getArchivedCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun addCategory(category: Category)
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun setArchived(id: Long, isArchived: Boolean)
    fun searchCategories(query: String): Flow<List<Category>>
}
