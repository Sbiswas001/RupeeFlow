package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Attachment
import sayan.apps.rupeeflow.domain.model.Transaction

data class CategorySpending(
    val categoryName: String,
    val colorHex: String,
    val amount: Double
)

data class TrendPoint(
    val timestamp: Long,
    val amount: Double
)

interface TransactionRepository {
    fun getTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun addTransaction(transaction: Transaction, accountId: Long, categoryId: Long?): Long
    suspend fun updateTransaction(transaction: Transaction, accountId: Long, categoryId: Long?)
    suspend fun deleteTransaction(transaction: Transaction)
    
    fun getCategorySpending(start: Long? = null, end: Long? = null): Flow<List<CategorySpending>>
    fun getSpendingTrend(start: Long? = null, end: Long? = null): Flow<List<TrendPoint>>

    fun getAttachments(transactionId: Long): Flow<List<Attachment>>
    suspend fun addAttachment(attachment: Attachment)

    fun getTransactionCountForCategory(categoryId: Long): Flow<Int>
    fun getTotalAmountForCategory(categoryId: Long): Flow<Double?>
    fun getTransactionsForCategory(categoryId: Long): Flow<List<Transaction>>

    suspend fun getMonthlySpending(start: Long, end: Long): Double?
    fun getTopMerchants(start: Long, end: Long, limit: Int): Flow<List<CategorySpending>>
    suspend fun getTransactionsInRangeSync(start: Long, end: Long): List<Transaction>
}
