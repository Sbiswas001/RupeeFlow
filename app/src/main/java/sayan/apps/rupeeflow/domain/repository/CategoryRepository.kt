package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Category

interface CategoryRepository {
    fun getCategories(): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun addCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category, targetCategoryId: Long? = null)
    suspend fun isCategoryInUse(categoryId: Long): Boolean
    fun searchCategories(query: String): Flow<List<Category>>
}
