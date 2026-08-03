package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.AttachmentEntity
import sayan.apps.rupeeflow.core.database.entity.MerchantEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity

@Dao
interface UtilityDao {
    // Merchants
    @Query("SELECT * FROM merchants")
    fun getAllMerchants(): Flow<List<MerchantEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMerchant(merchant: MerchantEntity): Long

    @Query("SELECT * FROM merchants WHERE name LIKE '%' || :query || '%'")
    fun searchMerchants(query: String): Flow<List<MerchantEntity>>

    // Recurring Items
    @Query("SELECT * FROM recurring_items")
    fun getAllRecurringItems(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_items WHERE status = 'PENDING' AND dueDate <= :currentTime")
    suspend fun getDueRecurringItemsSync(currentTime: Long): List<RecurringEntity>

    @Query("SELECT * FROM recurring_items WHERE id = :id")
    suspend fun getRecurringItemById(id: Long): RecurringEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringItem(item: RecurringEntity): Long

    @Update
    suspend fun updateRecurringItem(item: RecurringEntity)

    @Delete
    suspend fun deleteRecurringItem(item: RecurringEntity)

    @Query("SELECT * FROM recurring_items WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%'")
    fun searchRecurringItems(query: String): Flow<List<RecurringEntity>>

    @Query("""
        SELECT * FROM recurring_items 
        WHERE status != 'CANCELLED'
    """)
    fun getActiveRecurringItems(): Flow<List<RecurringEntity>>

    // Attachments
    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId")
    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long
}
