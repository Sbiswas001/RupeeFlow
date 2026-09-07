package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.CategoryEntity
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.model.CategoryTotal
import sayan.apps.rupeeflow.core.database.model.DailyTrend
import sayan.apps.rupeeflow.domain.model.TransactionType

data class TransactionWithCategory(
    @Embedded val transaction: TransactionEntity,
    @Relation(
        parentColumn = "categoryId",
        entityColumn = "id"
    )
    val category: CategoryEntity?
)

@Dao
interface TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>>

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME'")
    fun getTotalIncome(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE'")
    fun getTotalExpense(): Flow<Double?>

    @Query("""
        SELECT t.categoryId as categoryId, c.name as categoryName, c.colorHex as colorHex, SUM(t.amount) as totalAmount
        FROM transactions t
        LEFT JOIN categories c ON t.categoryId = c.id
        WHERE t.type = 'EXPENSE' AND t.timestamp BETWEEN :start AND :end
        GROUP BY t.categoryId
    """)
    fun getCategoryTotalsInRange(start: Long, end: Long): Flow<List<CategoryTotal>>

    @Query("""
        SELECT timestamp as timestamp, amount as totalAmount
        FROM transactions
        WHERE type = 'EXPENSE' AND timestamp BETWEEN :start AND :end
        ORDER BY timestamp ASC
    """)
    fun getDailySpendingTrendInRange(start: Long, end: Long): Flow<List<DailyTrend>>

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    fun getTransactionCountForCategory(categoryId: Long): Flow<Int>

    @Query("SELECT SUM(amount) FROM transactions WHERE categoryId = :categoryId")
    fun getTotalAmountForCategory(categoryId: Long): Flow<Double?>

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE categoryId = :categoryId 
          AND type = 'EXPENSE' 
          AND (transferId IS NULL OR transferId = '')
          AND timestamp BETWEEN :start AND :end
    """)
    fun getCategorySpendingInRangeFlow(categoryId: Long, start: Long, end: Long): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getTransactionsForCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND (transferId IS NULL OR transferId = '') AND timestamp BETWEEN :start AND :end")
    suspend fun getMonthlySpending(start: Long, end: Long): Double?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND (transferId IS NULL OR transferId = '') AND timestamp BETWEEN :start AND :end")
    suspend fun getTotalIncomeInRange(start: Long, end: Long): Double?

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' 
          AND categoryId = :categoryId 
          AND (transferId IS NULL OR transferId = '')
          AND timestamp BETWEEN :start AND :end
    """)
    suspend fun getCategorySpendingInRange(categoryId: Long, start: Long, end: Long): Double?

    @Query("SELECT * FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :start AND :end ORDER BY amount DESC LIMIT 1")
    suspend fun getBiggestExpenseInRange(start: Long, end: Long): TransactionEntity?

    @Query("SELECT COUNT(*) FROM transactions WHERE timestamp BETWEEN :start AND :end")
    suspend fun getTransactionCountInRange(start: Long, end: Long): Int

    @Query("SELECT * FROM transactions WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp ASC")
    fun getTransactionsInRangeSync(start: Long, end: Long): List<TransactionEntity>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND type = 'BALANCE_ADJUSTMENT' AND id != :excludeId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastReconciliationForAccount(accountId: Long, excludeId: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE transferId = :transferId")
    suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity>

    @Transaction
    @Query("""
        SELECT * FROM transactions 
        WHERE (title LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%')
        AND (:minAmount IS NULL OR amount >= :minAmount)
        AND (:maxAmount IS NULL OR amount <= :maxAmount)
        AND (:startDate IS NULL OR timestamp >= :startDate)
        AND (:endDate IS NULL OR timestamp <= :endDate)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:accountId IS NULL OR accountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:isRecurring IS NULL OR isRecurring = :isRecurring)
        ORDER BY timestamp DESC
    """)
    fun searchTransactions(
        query: String,
        minAmount: Double? = null,
        maxAmount: Double? = null,
        startDate: Long? = null,
        endDate: Long? = null,
        categoryId: Long? = null,
        accountId: Long? = null,
        type: TransactionType? = null,
        isRecurring: Boolean? = null
    ): Flow<List<TransactionWithCategory>>
}
