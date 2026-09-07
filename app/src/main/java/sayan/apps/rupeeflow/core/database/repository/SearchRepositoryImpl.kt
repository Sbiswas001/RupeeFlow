package sayan.apps.rupeeflow.core.database.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.dao.*
import sayan.apps.rupeeflow.core.database.entity.RecentSearchEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.domain.model.SearchFilters
import sayan.apps.rupeeflow.domain.model.SearchResult
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.SearchRepository
import javax.inject.Inject

class SearchRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val utilityDao: UtilityDao,
    private val planningDao: PlanningDao,
    private val recentSearchDao: RecentSearchDao
) : SearchRepository {

    override fun globalSearch(filters: SearchFilters): Flow<List<SearchResult>> {
        val transactionsFlow = transactionDao.searchTransactions(
            query = filters.query,
            minAmount = filters.minAmount,
            maxAmount = filters.maxAmount,
            startDate = filters.startDate,
            endDate = filters.endDate,
            categoryId = filters.categoryId,
            accountId = filters.accountId,
            type = filters.type,
            isRecurring = filters.isRecurring
        )

        val accountsFlow = accountDao.searchAccounts(filters.query)
        val categoriesFlow = categoryDao.searchCategories(filters.query)
        val recurringFlow = utilityDao.searchRecurringItems(filters.query)
        val budgetsFlow = planningDao.searchBudgets(filters.query)
        val goalsFlow = planningDao.searchGoals(filters.query)

        return combine(
            transactionsFlow,
            accountsFlow,
            categoriesFlow,
            recurringFlow,
            budgetsFlow,
            goalsFlow
        ) { args: Array<Any?> ->
            val transactions = (args[0] as List<TransactionWithCategory>).deduplicateTransfers()
            val accounts = args[1] as List<sayan.apps.rupeeflow.core.database.entity.AccountEntity>
            val categories = args[2] as List<sayan.apps.rupeeflow.core.database.entity.CategoryEntity>
            val recurring = args[3] as List<sayan.apps.rupeeflow.core.database.entity.RecurringEntity>
            val budgets = args[4] as List<sayan.apps.rupeeflow.core.database.entity.BudgetEntity>
            val goals = args[5] as List<sayan.apps.rupeeflow.core.database.entity.GoalEntity>

            val results = mutableListOf<SearchResult>()
            
            results.addAll(transactions.map { SearchResult.TransactionResult(it.toDomainModel()) })
            results.addAll(accounts.map { SearchResult.AccountResult(it.toDomainModel()) })
            results.addAll(categories.map { SearchResult.CategoryResult(it.toDomainModel()) })
            results.addAll(recurring.map { SearchResult.RecurringResult(it.toDomainModel()) })
            results.addAll(budgets.map { SearchResult.BudgetResult(it.toDomainModel(), "Category") })
            results.addAll(goals.map { SearchResult.GoalResult(it.toDomainModel()) })
            
            results
        }.flowOn(Dispatchers.Default)
    }

    override fun getRecentSearches(): Flow<List<String>> {
        return recentSearchDao.getRecentSearches().map { entities ->
            entities.map { it.query }
        }
    }

    private fun List<TransactionWithCategory>.deduplicateTransfers(): List<TransactionWithCategory> {
        val result = mutableListOf<TransactionWithCategory>()
        val seenTransferIds = mutableSetOf<String>()
        
        val transferGroups = this.filter { it.transaction.transferId != null }.groupBy { it.transaction.transferId!! }
        
        val canonicalTransfers = transferGroups.mapValues { (_, list) ->
            list.minWithOrNull(
                compareBy<TransactionWithCategory> { if (it.transaction.isIncoming) 1 else 0 }
                    .thenBy { it.transaction.id }
            ) ?: list.first()
        }
        
        for (item in this) {
            val transferId = item.transaction.transferId
            if (transferId != null) {
                if (transferId !in seenTransferIds) {
                    seenTransferIds.add(transferId)
                    val canonical = canonicalTransfers[transferId]
                    if (canonical != null) {
                        result.add(canonical)
                    }
                }
            } else {
                result.add(item)
            }
        }
        return result
    }

    override suspend fun saveSearch(query: String) {
        if (query.isNotBlank()) {
            recentSearchDao.insertSearch(RecentSearchEntity(query))
        }
    }

    override suspend fun deleteSearch(query: String) {
        recentSearchDao.deleteSearch(query)
    }

    override suspend fun clearRecentSearches() {
        recentSearchDao.clearAll()
    }
}
