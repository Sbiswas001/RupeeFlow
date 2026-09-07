package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
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
            entities.map { it.toDomainModel() }.deduplicateTransfers()
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomainModel()
    }

    override suspend fun addTransaction(transaction: Transaction, accountId: Long?, categoryId: Long?): Long {
        return database.withTransaction {
            val account = if (accountId != null) accountDao.getAccountByIdSync(accountId) else null
            
            val entity = transaction.toEntity(accountId, categoryId).copy(
                accountNameSnapshot = account?.name,
                accountCategorySnapshot = account?.category
            )
            val id = transactionDao.insertTransaction(entity)
            
            // Update Account Balance
            if (account != null) {
                val isLiability = account.category == AccountCategory.LIABILITIES
                val newBalance = when (entity.type) {
                    TransactionType.INCOME -> {
                        if (isLiability) account.balance - entity.amount else account.balance + entity.amount
                    }
                    TransactionType.EXPENSE -> {
                        if (isLiability) account.balance + entity.amount else account.balance - entity.amount
                    }
                    TransactionType.BALANCE_ADJUSTMENT -> account.balance + entity.amount
                    TransactionType.TRANSFER -> {
                        if (entity.isIncoming) {
                            if (isLiability) account.balance - entity.amount else account.balance + entity.amount
                        } else {
                            if (isLiability) account.balance + entity.amount else account.balance - entity.amount
                        }
                    }
                }
                accountDao.updateAccount(account.copy(balance = newBalance))
            }
            id
        }
    }

    override suspend fun updateTransaction(transaction: Transaction, accountId: Long?, categoryId: Long?) {
        database.withTransaction {
            val oldTransaction = transactionDao.getTransactionById(transaction.id.toLong()) ?: return@withTransaction
            
            // Revert old balance
            val oldAccountId = oldTransaction.accountId
            if (oldAccountId != null) {
                val oldAccount = accountDao.getAccountByIdSync(oldAccountId)
                if (oldAccount != null) {
                    val isOldLiability = oldAccount.category == AccountCategory.LIABILITIES
                    val revertedBalance = when (oldTransaction.type) {
                        TransactionType.INCOME -> {
                            if (isOldLiability) oldAccount.balance + oldTransaction.amount else oldAccount.balance - oldTransaction.amount
                        }
                        TransactionType.EXPENSE -> {
                            if (isOldLiability) oldAccount.balance - oldTransaction.amount else oldAccount.balance + oldTransaction.amount
                        }
                        TransactionType.BALANCE_ADJUSTMENT -> oldAccount.balance - oldTransaction.amount
                        TransactionType.TRANSFER -> {
                            if (oldTransaction.isIncoming) {
                                if (isOldLiability) oldAccount.balance + oldTransaction.amount else oldAccount.balance - oldTransaction.amount
                            } else {
                                if (isOldLiability) oldAccount.balance - oldTransaction.amount else oldAccount.balance + oldTransaction.amount
                            }
                        }
                    }
                    accountDao.updateAccount(oldAccount.copy(balance = revertedBalance))
                }
            }

            // Apply new transaction
            val account = if (accountId != null) accountDao.getAccountByIdSync(accountId) else null
            val entity = transaction.toEntity(accountId, categoryId).copy(
                accountNameSnapshot = account?.name,
                accountCategorySnapshot = account?.category
            )
            transactionDao.updateTransaction(entity)

            // Apply new balance
            if (account != null) {
                val isNewLiability = account.category == AccountCategory.LIABILITIES
                val newBalance = when (entity.type) {
                    TransactionType.INCOME -> {
                        if (isNewLiability) account.balance - entity.amount else account.balance + entity.amount
                    }
                    TransactionType.EXPENSE -> {
                        if (isNewLiability) account.balance + entity.amount else account.balance - entity.amount
                    }
                    TransactionType.BALANCE_ADJUSTMENT -> account.balance + entity.amount
                    TransactionType.TRANSFER -> {
                        if (entity.isIncoming) {
                            if (isNewLiability) account.balance - entity.amount else account.balance + entity.amount
                        } else {
                            if (isNewLiability) account.balance + entity.amount else account.balance - entity.amount
                        }
                    }
                }
                accountDao.updateAccount(account.copy(balance = newBalance))
            }
        }
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        database.withTransaction {
            val entity = transactionDao.getTransactionById(transaction.id.toLong()) ?: return@withTransaction
            
            if (entity.type == TransactionType.TRANSFER && entity.transferId != null) {
                deleteTransfer(entity.transferId)
            } else {
                deleteSingleTransactionInternal(entity)
            }
        }
    }

    override suspend fun deleteTransfer(transferId: String) {
        database.withTransaction {
            val transferSides = transactionDao.getTransactionsByTransferId(transferId)
            transferSides.forEach { side ->
                deleteSingleTransactionInternal(side)
            }
        }
    }

    private suspend fun deleteSingleTransactionInternal(entity: TransactionEntity) {
        transactionDao.deleteTransaction(entity)

        // Update Account Balance (revert)
        val accountId = entity.accountId
        if (accountId != null) {
            val account = accountDao.getAccountByIdSync(accountId)
            if (account != null) {
                val isLiability = account.category == AccountCategory.LIABILITIES
                val revertedBalance = when (entity.type) {
                    TransactionType.INCOME -> {
                        if (isLiability) account.balance + entity.amount else account.balance - entity.amount
                    }
                    TransactionType.EXPENSE -> {
                        if (isLiability) account.balance - entity.amount else account.balance + entity.amount
                    }
                    TransactionType.BALANCE_ADJUSTMENT -> account.balance - entity.amount
                    TransactionType.TRANSFER -> {
                        if (entity.isIncoming) {
                            if (isLiability) account.balance + entity.amount else account.balance - entity.amount
                        } else {
                            if (isLiability) account.balance - entity.amount else account.balance + entity.amount
                        }
                    }
                }
                
                var lastReconciledAt = account.lastReconciledAt
                var lastReconciledBalance = account.lastReconciledBalance

                // If we deleted the latest reconciliation, find the previous one
                if (entity.type == TransactionType.BALANCE_ADJUSTMENT && entity.timestamp == account.lastReconciledAt) {
                    val prevRec = transactionDao.getLastReconciliationForAccount(entity.accountId ?: 0L, entity.id)
                    lastReconciledAt = prevRec?.timestamp
                    lastReconciledBalance = prevRec?.actualBalance
                }

                accountDao.updateAccount(
                    account.copy(
                        balance = revertedBalance,
                        lastReconciledAt = lastReconciledAt,
                        lastReconciledBalance = lastReconciledBalance
                    )
                )
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
                    amount = it.totalAmount,
                    categoryId = it.categoryId
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    private fun toStartOfDay(millis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    override fun getSpendingTrend(start: Long?, end: Long?): Flow<List<TrendPoint>> {
        val flow = if (start != null && end != null) {
            transactionDao.getDailySpendingTrendInRange(start, end)
        } else {
            transactionDao.getDailySpendingTrendInRange(0L, Long.MAX_VALUE)
        }

        return flow.map { list ->
            list.groupBy { toStartOfDay(it.timestamp) }
                .map { (startOfDay, items) ->
                    TrendPoint(
                        timestamp = startOfDay,
                        amount = items.sumOf { it.totalAmount }
                    )
                }
                .sortedBy { it.timestamp }
        }.flowOn(Dispatchers.Default)
    }

    override fun getAttachments(transactionId: Long): Flow<List<Attachment>> {
        return utilityDao.getAttachmentsForTransaction(transactionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun addAttachment(attachment: Attachment) {
        utilityDao.insertAttachment(attachment.toEntity())
    }

    override suspend fun deleteAttachmentsForTransaction(transactionId: Long) {
        utilityDao.deleteAttachmentsForTransaction(transactionId)
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

    override suspend fun getTotalIncomeInRange(start: Long, end: Long): Double? {
        return transactionDao.getTotalIncomeInRange(start, end)
    }

    override suspend fun getCategorySpendingInRange(categoryId: Long, start: Long, end: Long): Double? {
        return transactionDao.getCategorySpendingInRange(categoryId, start, end)
    }

    override suspend fun getBiggestExpenseInRange(start: Long, end: Long): Transaction? {
        return transactionDao.getBiggestExpenseInRange(start, end)?.toDomainModel()
    }

    override suspend fun getTransactionCountInRange(start: Long, end: Long): Int {
        return transactionDao.getTransactionCountInRange(start, end)
    }

    override suspend fun getTransactionsInRangeSync(start: Long, end: Long): List<Transaction> {
        return transactionDao.getTransactionsInRangeSync(start, end)
            .map { it.toDomainModel() }
            .deduplicateTransfers()
    }

    private fun List<Transaction>.deduplicateTransfers(): List<Transaction> {
        val result = mutableListOf<Transaction>()
        val seenTransferIds = mutableSetOf<String>()
        
        val transferGroups = this.filter { it.transferId != null }.groupBy { it.transferId!! }
        
        val canonicalTransfers = transferGroups.mapValues { (_, list) ->
            list.minWithOrNull(
                compareBy<Transaction> { if (it.isIncoming) 1 else 0 }
                    .thenBy { it.id.toLongOrNull() ?: 0L }
            ) ?: list.first()
        }
        
        for (transaction in this) {
            val transferId = transaction.transferId
            if (transferId != null) {
                if (transferId !in seenTransferIds) {
                    seenTransferIds.add(transferId)
                    val canonical = canonicalTransfers[transferId]
                    if (canonical != null) {
                        result.add(canonical)
                    }
                }
            } else {
                result.add(transaction)
            }
        }
        return result
    }
}
