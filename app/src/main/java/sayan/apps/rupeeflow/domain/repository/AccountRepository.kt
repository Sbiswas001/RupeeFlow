package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.SavedUpiApp

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getAccountsIncludingClosed(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long = System.currentTimeMillis())
    suspend fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long = System.currentTimeMillis())
    suspend fun reconcileAccount(accountId: Long, actualBalance: Double, reason: String?, note: String?)
    fun getNetWorthHistory(days: Int = 30): Flow<List<Pair<Long, Double>>>

    fun getDebitCardsForAccount(accountId: Long): Flow<List<DebitCard>>
    suspend fun getDebitCardById(id: Long): DebitCard?
    suspend fun addDebitCard(debitCard: DebitCard): Long
    suspend fun updateDebitCard(debitCard: DebitCard)
    suspend fun deleteDebitCard(debitCard: DebitCard)

    fun getUpiAppsForAccount(accountId: Long): Flow<List<SavedUpiApp>>
    suspend fun addUpiApp(upiApp: SavedUpiApp): Long
    suspend fun updateUpiApp(upiApp: SavedUpiApp)
    suspend fun deleteUpiApp(upiApp: SavedUpiApp)
}
