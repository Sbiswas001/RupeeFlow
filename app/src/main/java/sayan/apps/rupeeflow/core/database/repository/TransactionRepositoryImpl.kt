package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.Attachment
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.repository.TrendPoint
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val utilityDao: UtilityDao
) : TransactionRepository {

    override fun getTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransactionsWithCategory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomainModel()
    }

    override suspend fun addTransaction(transaction: Transaction, accountId: Long, categoryId: Long?): Long {
        return database.withTransaction {
            val entity = transaction.toEntity(accountId, categoryId)
            val id = transactionDao.insertTransaction(entity)
            
            // Update Account Balance
            val account = accountDao.getAccountByIdSync(accountId)
            if (account != null) {
                val newBalance = if (transaction.isIncome) {
                    account.balance + transaction.amount
                } else {
                    account.balance - transaction.amount
                }
                accountDao.updateAccount(account.copy(balance = newBalance))
            }
            id
        }
    }

    override suspend fun updateTransaction(transaction: Transaction, accountId: Long, categoryId: Long?) {
        database.withTransaction {
            val oldTransaction = transactionDao.getTransactionById(transaction.id.toLong()) ?: return@withTransaction
            
            // Revert old balance
            val oldAccount = accountDao.getAccountByIdSync(oldTransaction.accountId)
            if (oldAccount != null) {
                val revertedBalance = if (oldTransaction.type == TransactionType.INCOME) {
                    oldAccount.balance - oldTransaction.amount
                } else {
                    oldAccount.balance + oldTransaction.amount
                }
                accountDao.updateAccount(oldAccount.copy(balance = revertedBalance))
            }

            // Apply new transaction
            val entity = transaction.toEntity(accountId, categoryId)
            transactionDao.updateTransaction(entity)

            // Apply new balance
            val newAccount = accountDao.getAccountByIdSync(accountId)
            if (newAccount != null) {
                val newBalance = if (transaction.isIncome) {
                    newAccount.balance + transaction.amount
                } else {
                    newAccount.balance - transaction.amount
                }
                accountDao.updateAccount(newAccount.copy(balance = newBalance))
            }
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        database.withTransaction {
            val entity = transactionDao.getTransactionById(transaction.id.toLong()) ?: return@withTransaction
            transactionDao.deleteTransaction(entity)

            // Update Account Balance (revert)
            val account = accountDao.getAccountByIdSync(entity.accountId)
            if (account != null) {
                val newBalance = if (transaction.isIncome) {
                    account.balance - transaction.amount
                } else {
                    account.balance + transaction.amount
                }
                accountDao.updateAccount(account.copy(balance = newBalance))
            }
        }
    }

    override fun getCategorySpending(start: Long?, end: Long?): Flow<List<CategorySpending>> {
        val flow = if (start != null && end != null) {
            transactionDao.getCategoryTotalsInRange(start, end)
        } else {
            transactionDao.getCategoryTotalsInRange(0L, Long.MAX_VALUE)
        }
        
        return flow.map { list ->
            list.map {
                CategorySpending(
                    categoryName = it.categoryName ?: "Uncategorized",
                    colorHex = it.colorHex ?: "#808080",
                    amount = it.totalAmount
                )
            }
        }
    }

    override fun getSpendingTrend(start: Long?, end: Long?): Flow<List<TrendPoint>> {
        val flow = if (start != null && end != null) {
            transactionDao.getDailySpendingTrendInRange(start, end)
        } else {
            transactionDao.getDailySpendingTrendInRange(0L, Long.MAX_VALUE)
        }

        return flow.map { list ->
            list.map {
                TrendPoint(
                    timestamp = it.timestamp,
                    amount = it.totalAmount
                )
            }
        }
    }

    override fun getAttachments(transactionId: Long): Flow<List<Attachment>> {
        return utilityDao.getAttachmentsForTransaction(transactionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addAttachment(attachment: Attachment) {
        utilityDao.insertAttachment(attachment.toEntity())
    }

    override fun getTransactionCountForCategory(categoryId: Long): Flow<Int> {
        return transactionDao.getTransactionCountForCategory(categoryId)
    }

    override fun getTotalAmountForCategory(categoryId: Long): Flow<Double?> {
        return transactionDao.getTotalAmountForCategory(categoryId)
    }

    override fun getTransactionsForCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsForCategory(categoryId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getMonthlySpending(start: Long, end: Long): Double? {
        return transactionDao.getMonthlySpending(start, end)
    }

    override fun getTopMerchants(start: Long, end: Long, limit: Int): Flow<List<CategorySpending>> {
        return transactionDao.getTopMerchants(start, end, limit).map { list ->
            list.map {
                CategorySpending(
                    categoryName = it.categoryName ?: "Unknown Merchant",
                    colorHex = it.colorHex ?: "#808080",
                    amount = it.totalAmount
                )
            }
        }
    }

    override suspend fun getTransactionsInRangeSync(start: Long, end: Long): List<Transaction> {
        return transactionDao.getTransactionsInRangeSync(start, end).map { it.toDomainModel() }
    }
}
