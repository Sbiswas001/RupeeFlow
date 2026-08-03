package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : AccountRepository {
    override fun getAccounts(): Flow<List<Account>> {
        return accountDao.getAllAccounts().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomainModel()
    }

    override suspend fun addAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?) {
        database.withTransaction {
            val fromAccount = accountDao.getAccountByIdSync(fromAccountId) ?: return@withTransaction
            val toAccount = accountDao.getAccountByIdSync(toAccountId) ?: return@withTransaction

            // Update Balances
            accountDao.updateAccount(fromAccount.copy(balance = fromAccount.balance - amount))
            accountDao.updateAccount(toAccount.copy(balance = toAccount.balance + amount))

            // Create two transactions for tracking
            val timestamp = System.currentTimeMillis()
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer Out",
                    amount = amount,
                    type = TransactionType.EXPENSE,
                    categoryId = null, // Maybe a specific "Transfer" category later
                    accountId = fromAccountId,
                    merchantId = null,
                    timestamp = timestamp,
                    note = "Transfer to ${toAccount.name}${if (note != null) ": $note" else ""}"
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Transfer In",
                    amount = amount,
                    type = TransactionType.INCOME,
                    categoryId = null,
                    accountId = toAccountId,
                    merchantId = null,
                    timestamp = timestamp,
                    note = "Transfer from ${fromAccount.name}${if (note != null) ": $note" else ""}"
                )
            )
        }
    }
}
