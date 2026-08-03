package sayan.apps.rupeeflow.core.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.dao.CategoryDao
import sayan.apps.rupeeflow.core.database.dao.PlanningDao
import sayan.apps.rupeeflow.core.database.entity.BudgetEntity
import sayan.apps.rupeeflow.core.database.entity.BudgetPeriod
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao,
    private val planningDao: PlanningDao
) : CategoryRepository {

    override fun getCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getActiveCategories(): Flow<List<Category>> {
        return categoryDao.getActiveCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getArchivedCategories(): Flow<List<Category>> {
        return categoryDao.getArchivedCategories().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getCategoryById(id: Long): Category? {
        return categoryDao.getCategoryById(id)?.toDomainModel()
    }

    override suspend fun addCategory(category: Category) {
        val id = categoryDao.insertCategory(category.toEntity())
        syncBudget(id, category.budget)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
        syncBudget(category.id, category.budget)
    }

    private suspend fun syncBudget(categoryId: Long, budgetAmount: Double?) {
        if (budgetAmount != null && budgetAmount > 0) {
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

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }

    override suspend fun setArchived(id: Long, isArchived: Boolean) {
        categoryDao.setArchived(id, isArchived)
    }

    override fun searchCategories(query: String): Flow<List<Category>> {
        return categoryDao.searchCategories(query).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
