package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.AccountUpiAppDao
import sayan.apps.rupeeflow.core.database.dao.DebitCardDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.SavedUpiApp
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val debitCardDao: DebitCardDao,
    private val accountUpiAppDao: AccountUpiAppDao
) : AccountRepository {
    override fun getAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override fun getAccountsIncludingClosed(): Flow<List<Account>> {
        return accountDao.getAllAccountsIncludingClosed().map { entities ->
            entities.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override fun getNetWorthHistory(days: Int): Flow<List<Pair<Long, Double>>> {
        return combine(
            accountDao.getAllAccountsIncludingClosed(),
            transactionDao.getAllTransactions()
        ) { accounts, transactions ->
            val currentNetWorth = accounts.sumOf { 
                if (it.category == AccountCategory.LIABILITIES) -it.balance else it.balance 
            }
            
            val history = mutableListOf<Pair<Long, Double>>()
            var runningNetWorth = currentNetWorth
            
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            
            val dayMillis = 24 * 60 * 60 * 1000L
            
            val accountMap = accounts.associateBy { it.id }

            // Add today
            history.add(calendar.timeInMillis to runningNetWorth)
            
            for (dayIdx in 1 until days) {
                val endOfDay = calendar.timeInMillis
                val startOfDay = endOfDay - dayMillis + 1
                
                val dayTransactions = transactions.filter { it.timestamp in startOfDay..endOfDay }
                val dayNetChange = dayTransactions.sumOf { trans ->
                    val account = accountMap[trans.accountId]
                    val isLiability = if (account != null) {
                        account.category == AccountCategory.LIABILITIES
                    } else {
                        trans.accountCategorySnapshot == AccountCategory.LIABILITIES
                    }

                    when (trans.type) {
                        TransactionType.INCOME -> trans.amount
                        TransactionType.EXPENSE -> -trans.amount
                        TransactionType.BALANCE_ADJUSTMENT -> {
                            if (isLiability) -trans.amount else trans.amount
                        }
                        TransactionType.TRANSFER -> {
                            if (trans.isIncoming) trans.amount else -trans.amount
                        }
                    }
                }
                
                runningNetWorth -= dayNetChange
                calendar.timeInMillis -= dayMillis
                history.add(calendar.timeInMillis to runningNetWorth)
            }
            
            history.sortedBy { it.first }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomainModel()
    }

    override suspend fun addAccount(account: Account) {
        val accountToInsert = if (account.lastReconciledAt == null) {
            account.copy(
                lastReconciledAt = System.currentTimeMillis(),
                lastReconciledBalance = account.balance
            )
        } else {
            account
        }
        accountDao.insertAccount(accountToInsert.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.updateAccount(account.toEntity().copy(isClosed = true))
    }

    override suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long) {
        database.withTransaction {
            val fromAccount = accountDao.getAccountByIdSync(fromAccountId) ?: return@withTransaction
            val toAccount = accountDao.getAccountByIdSync(toAccountId) ?: return@withTransaction

            if (fromAccountId == toAccountId) return@withTransaction
            if (amount <= 0) return@withTransaction

            val transferId = UUID.randomUUID().toString()

            // 1. Update Balances
            val fromIsLiability = fromAccount.category == AccountCategory.LIABILITIES
            val toIsLiability = toAccount.category == AccountCategory.LIABILITIES
            
            val newFromBalance = if (fromIsLiability) fromAccount.balance + amount else fromAccount.balance - amount
            val newToBalance = if (toIsLiability) toAccount.balance - amount else toAccount.balance + amount
            
            accountDao.updateAccount(fromAccount.copy(balance = newFromBalance))
            accountDao.updateAccount(toAccount.copy(balance = newToBalance))

            // 2. Create two transactions
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer Out",
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = fromAccountId,
                    timestamp = timestamp,
                    note = note,
                    transferId = transferId,
                    transferAccountId = toAccountId,
                    transferAccountNameSnapshot = toAccount.name,
                    accountNameSnapshot = fromAccount.name,
                    accountCategorySnapshot = fromAccount.category,
                    isIncoming = false
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer In",
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = toAccountId,
                    timestamp = timestamp,
                    note = note,
                    transferId = transferId,
                    transferAccountId = fromAccountId,
                    transferAccountNameSnapshot = fromAccount.name,
                    accountNameSnapshot = toAccount.name,
                    accountCategorySnapshot = toAccount.category,
                    isIncoming = true
                )
            )
        }
    }

    override suspend fun updateTransfer(
        transferId: String,
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String?,
        timestamp: Long
    ) {
        database.withTransaction {
            val oldSides = transactionDao.getTransactionsByTransferId(transferId)
            if (oldSides.isEmpty()) return@withTransaction

            // Revert balances for all old sides
            oldSides.forEach { entity ->
                val accountId = entity.accountId
                if (accountId != null) {
                    val account = accountDao.getAccountByIdSync(accountId)
                    if (account != null) {
                        val isLiability = account.category == AccountCategory.LIABILITIES
                        val revertedBalance = if (entity.isIncoming) {
                            if (isLiability) account.balance + entity.amount else account.balance - entity.amount
                        } else {
                            if (isLiability) account.balance - entity.amount else account.balance + entity.amount
                        }
                        accountDao.updateAccount(account.copy(balance = revertedBalance))
                    }
                }
            }

            // Delete old transfer records
            oldSides.forEach { transactionDao.deleteTransaction(it) }

            // Re-fetch accounts to get reverted balances
            val fromAccount = accountDao.getAccountByIdSync(fromAccountId) ?: return@withTransaction
            val toAccount = accountDao.getAccountByIdSync(toAccountId) ?: return@withTransaction

            if (fromAccountId == toAccountId || amount <= 0) return@withTransaction

            // Update balances for new transfer
            val fromIsLiability = fromAccount.category == AccountCategory.LIABILITIES
            val toIsLiability = toAccount.category == AccountCategory.LIABILITIES

            val newFromBalance = if (fromIsLiability) fromAccount.balance + amount else fromAccount.balance - amount
            val newToBalance = if (toIsLiability) toAccount.balance - amount else toAccount.balance + amount

            accountDao.updateAccount(fromAccount.copy(balance = newFromBalance))
            accountDao.updateAccount(toAccount.copy(balance = newToBalance))

            // Insert 2 updated transfer transactions
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer Out",
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = fromAccountId,
                    timestamp = timestamp,
                    note = note,
                    transferId = transferId,
                    transferAccountId = toAccountId,
                    transferAccountNameSnapshot = toAccount.name,
                    accountNameSnapshot = fromAccount.name,
                    accountCategorySnapshot = fromAccount.category,
                    isIncoming = false
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer In",
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = null,
                    accountId = toAccountId,
                    timestamp = timestamp,
                    note = note,
                    transferId = transferId,
                    transferAccountId = fromAccountId,
                    transferAccountNameSnapshot = fromAccount.name,
                    accountNameSnapshot = toAccount.name,
                    accountCategorySnapshot = toAccount.category,
                    isIncoming = true
                )
            )
        }
    }

    override suspend fun reconcileAccount(
        accountId: Long,
        actualBalance: Double,
        reason: String?,
        note: String?
    ) {
        database.withTransaction {
            val account = accountDao.getAccountByIdSync(accountId) ?: return@withTransaction
            val recordedBalance = account.balance
            val difference = actualBalance - recordedBalance
            val timestamp = System.currentTimeMillis()

            if (difference != 0.0) {
                // Create Balance Adjustment Transaction
                transactionDao.insertTransaction(
                    TransactionEntity(
                        title = "Balance Adjustment",
                        amount = difference,
                        type = TransactionType.BALANCE_ADJUSTMENT,
                        categoryId = null,
                        accountId = accountId,
                        timestamp = timestamp,
                        note = note,
                        reconciliationReason = reason,
                        previousBalance = recordedBalance,
                        actualBalance = actualBalance,
                        accountNameSnapshot = account.name,
                        accountCategorySnapshot = account.category
                    )
                )
            }

            // Update Account
            accountDao.updateAccount(
                account.copy(
                    balance = actualBalance,
                    lastReconciledAt = timestamp,
                    lastReconciledBalance = actualBalance,
                    lastUpdated = timestamp
                )
            )
        }
    }

    override fun getDebitCardsForAccount(accountId: Long): Flow<List<DebitCard>> {
        return debitCardDao.getDebitCardsForAccount(accountId).map { list ->
            list.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getDebitCardById(id: Long): DebitCard? {
        return debitCardDao.getDebitCardById(id)?.toDomainModel()
    }

    override suspend fun addDebitCard(debitCard: DebitCard): Long {
        return debitCardDao.insertDebitCard(debitCard.toEntity())
    }

    override suspend fun updateDebitCard(debitCard: DebitCard) {
        debitCardDao.updateDebitCard(debitCard.toEntity())
    }

    override suspend fun deleteDebitCard(debitCard: DebitCard) {
        debitCardDao.deleteDebitCard(debitCard.toEntity())
    }

    override fun getUpiAppsForAccount(accountId: Long): Flow<List<SavedUpiApp>> {
        return accountUpiAppDao.getUpiAppsForAccount(accountId).map { list ->
            list.map { it.toDomainModel() }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun addUpiApp(upiApp: SavedUpiApp): Long {
        return accountUpiAppDao.insertUpiApp(upiApp.toEntity())
    }

    override suspend fun updateUpiApp(upiApp: SavedUpiApp) {
        accountUpiAppDao.updateUpiApp(upiApp.toEntity())
    }

    override suspend fun deleteUpiApp(upiApp: SavedUpiApp) {
        accountUpiAppDao.deleteUpiApp(upiApp.toEntity())
    }
}
