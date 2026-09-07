package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.CategoryDao
import sayan.apps.rupeeflow.core.database.dao.PlanningDao
import sayan.apps.rupeeflow.core.database.entity.BudgetEntity
import sayan.apps.rupeeflow.core.database.entity.BudgetPeriod
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val categoryDao: CategoryDao,
    private val planningDao: PlanningDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomainModel()
    }

    override suspend fun addCategory(category: Category): Long {
        val id = categoryDao.insertCategory(category.toEntity())
        syncBudget(id, category.budget)
        return id
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
        syncBudget(category.id, category.budget)
    }

    private suspend fun syncBudget(categoryId: Long, budgetAmount: Double?) {
        val category = categoryDao.getCategoryById(categoryId)
        if (category?.type == TransactionType.EXPENSE && budgetAmount != null && budgetAmount > 0) {
            planningDao.insertBudget(
                BudgetEntity(
                    categoryId = categoryId,
                    limitAmount = budgetAmount,
                    period = BudgetPeriod.MONTHLY,
                    startDate = System.currentTimeMillis()
                )
            )
        } else {
            planningDao.deleteBudgetByCategoryId(categoryId)
        }
    }

    override suspend fun deleteCategory(category: Category, targetCategoryId: Long?) {
        database.withTransaction {
            if (targetCategoryId != null) {
                // 1. Move transactions to the new category
                categoryDao.reassignTransactions(category.id, targetCategoryId)
                // 2. Hard delete the old category since it's now empty
                categoryDao.deleteCategory(category.toEntity())
            } else if (categoryDao.isCategoryInUse(category.id)) {
                // 1. Keep transactions, but soft delete the category so it's hidden but history is preserved
                categoryDao.softDeleteCategory(category.id)
            } else {
                // 1. No transactions, safe to hard delete
                categoryDao.deleteCategory(category.toEntity())
            }
            
            // Cleanup budgets regardless of delete type
            planningDao.deleteBudgetByCategoryId(category.id)
        }
    }

    override suspend fun isCategoryInUse(categoryId: Long): Boolean {
        return categoryDao.isCategoryInUse(categoryId)
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query).map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }
}
