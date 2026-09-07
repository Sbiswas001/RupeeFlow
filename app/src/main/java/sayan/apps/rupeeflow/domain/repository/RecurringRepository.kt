package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence

interface RecurringRepository {
    fun getRecurringItems(): Flow<List<RecurringItem>>
    suspend fun getRecurringItemById(id: Long): RecurringItem?
    suspend fun addRecurringItem(item: RecurringItem)
    suspend fun updateRecurringItem(item: RecurringItem)
    suspend fun deleteRecurringItem(item: RecurringItem)
    
    suspend fun processDueRecurringTransactions(): Int
    suspend fun markAsPaid(occurrenceId: Long, accountId: Long)
    suspend fun skipOccurrence(occurrenceId: Long)
    suspend fun undoPayment(occurrenceId: Long)
    suspend fun toggleRecurringItemStatus(itemId: Long)
    
    fun getActiveRecurringItems(): Flow<List<RecurringItem>>
    fun getOccurrences(itemId: Long): Flow<List<RecurringOccurrence>>
    fun getUpcomingOccurrences(limit: Int): Flow<List<RecurringOccurrence>>
}
