package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.RecurringItem

interface RecurringRepository {
    fun getRecurringItems(): Flow<List<RecurringItem>>
    suspend fun getRecurringItemById(id: Long): RecurringItem?
    suspend fun addRecurringItem(item: RecurringItem)
    suspend fun updateRecurringItem(item: RecurringItem)
    suspend fun deleteRecurringItem(item: RecurringItem)
    
    suspend fun markAsPaid(item: RecurringItem, accountId: Long)
    fun getActiveRecurringItems(): Flow<List<RecurringItem>>
}
