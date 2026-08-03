package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Account

interface AccountRepository {
    fun getAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?)
}
