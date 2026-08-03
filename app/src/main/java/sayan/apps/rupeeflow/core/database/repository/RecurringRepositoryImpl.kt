package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import javax.inject.Inject

class RecurringRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val utilityDao: UtilityDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao
) : RecurringRepository {

    override fun getRecurringItems(): Flow<List<RecurringItem>> {
        return utilityDao.getAllRecurringItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getRecurringItemById(id: Long): RecurringItem? {
        return utilityDao.getRecurringItemById(id)?.toDomainModel()
    }

    override suspend fun addRecurringItem(item: RecurringItem) {
        utilityDao.insertRecurringItem(item.toEntity())
    }

    override suspend fun updateRecurringItem(item: RecurringItem) {
        utilityDao.updateRecurringItem(item.toEntity())
    }

    override suspend fun deleteRecurringItem(item: RecurringItem) {
        utilityDao.deleteRecurringItem(item.toEntity())
    }

    override suspend fun markAsPaid(item: RecurringItem, accountId: Long) {
        database.withTransaction {
            // Update Recurring Item Status
            val updatedItem = item.copy(
                status = RecurringStatus.PAID.name,
                lastPaidDate = System.currentTimeMillis()
            )
            utilityDao.updateRecurringItem(updatedItem.toEntity())

            // Create Transaction
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = item.title,
                    amount = item.amount,
                    type = TransactionType.EXPENSE,
                    categoryId = null, // TODO: Map to actual category ID
                    accountId = accountId,
                    merchantId = null,
                    timestamp = System.currentTimeMillis(),
                    note = "Recurring payment for ${item.category}"
                )
            )

            // Update Account Balance
            val account = accountDao.getAccountByIdSync(accountId)
            if (account != null) {
                accountDao.updateAccount(account.copy(balance = account.balance - item.amount))
            }
        }
    }

    override fun getActiveRecurringItems(): Flow<List<RecurringItem>> {
        return utilityDao.getActiveRecurringItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
