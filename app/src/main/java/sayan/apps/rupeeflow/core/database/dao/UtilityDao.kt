package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.AttachmentEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringOccurrenceEntity

@Dao
interface UtilityDao {
    // Recurring Items
    @Query("SELECT * FROM recurring_items")
    fun getAllRecurringItems(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring_items WHERE status = 'ACTIVE'")
    suspend fun getActiveRecurringItemsSync(): List<RecurringEntity>

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

    // Recurring Occurrences
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrences(occurrences: List<RecurringOccurrenceEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long

    @Update
    suspend fun updateOccurrence(occurrence: RecurringOccurrenceEntity)

    @Query("SELECT * FROM recurring_occurrences WHERE recurringItemId = :itemId ORDER BY scheduledDate ASC")
    fun getOccurrencesByItemId(itemId: Long): Flow<List<RecurringOccurrenceEntity>>

    @Query("SELECT * FROM recurring_occurrences ORDER BY scheduledDate ASC")
    fun getAllOccurrences(): Flow<List<RecurringOccurrenceEntity>>

    @Query("SELECT * FROM recurring_occurrences WHERE recurringItemId = :itemId ORDER BY scheduledDate ASC")
    suspend fun getOccurrencesByItemIdSync(itemId: Long): List<RecurringOccurrenceEntity>

    @Query("SELECT * FROM recurring_occurrences WHERE recurringItemId = :itemId AND status = 'PENDING' ORDER BY scheduledDate ASC LIMIT 1")
    suspend fun getNextPendingOccurrence(itemId: Long): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrences WHERE id = :id")
    suspend fun getOccurrenceById(id: Long): RecurringOccurrenceEntity?

    @Query("SELECT * FROM recurring_occurrences WHERE status = 'PENDING' AND scheduledDate <= :currentTime")
    suspend fun getDueOccurrencesSync(currentTime: Long): List<RecurringOccurrenceEntity>

    @Query("DELETE FROM recurring_occurrences WHERE recurringItemId = :itemId AND status = 'PENDING' AND scheduledDate > :currentTime")
    suspend fun deleteFuturePendingOccurrences(itemId: Long, currentTime: Long)

    @Query("DELETE FROM recurring_occurrences WHERE recurringItemId = :itemId AND status = 'PENDING'")
    suspend fun deleteAllPendingOccurrences(itemId: Long)

    @Query("DELETE FROM recurring_items")
    suspend fun clearAllRecurringItems()

    @Query("DELETE FROM recurring_occurrences")
    suspend fun clearAllOccurrences()

    data class OccurrenceWithItem(
        @Embedded val occurrence: RecurringOccurrenceEntity,
        @Relation(
            parentColumn = "recurringItemId",
            entityColumn = "id"
        )
        val item: RecurringEntity?
    )

    @Transaction
    @Query("""
        SELECT * FROM recurring_occurrences 
        WHERE status = 'PENDING' 
        ORDER BY scheduledDate ASC 
        LIMIT :limit
    """)
    fun getUpcomingPendingOccurrences(limit: Int): Flow<List<OccurrenceWithItem>>

    // Attachments
    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId")
    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Query("DELETE FROM attachments WHERE transactionId = :transactionId")
    suspend fun deleteAttachmentsForTransaction(transactionId: Long)
}
