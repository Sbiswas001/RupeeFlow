package sayan.apps.rupeeflow.core.database.repository

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.OccurrenceStatus
import sayan.apps.rupeeflow.core.database.entity.RecurrenceFrequency
import sayan.apps.rupeeflow.core.database.entity.RecurringOccurrenceEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import sayan.apps.rupeeflow.domain.model.TransactionType
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.NotificationHelper
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import java.util.Calendar
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringRepositoryImpl @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val utilityDao: UtilityDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) : RecurringRepository {

    override fun getRecurringItems(): Flow<List<RecurringItem>> {
        return combine(
            utilityDao.getAllRecurringItems(),
            utilityDao.getAllOccurrences()
        ) { items, allOccurrences ->
            val occurrenceMap = allOccurrences.groupBy { it.recurringItemId }
            items.map { entity ->
                val occurrences = occurrenceMap[entity.id] ?: emptyList()
                entity.toDomainModel(occurrences)
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getRecurringItemById(id: Long): RecurringItem? {
        return utilityDao.getRecurringItemById(id)?.toDomainModel()
    }

    override suspend fun addRecurringItem(item: RecurringItem) {
        database.withTransaction {
            val entity = item.toEntity().copy(
                status = RecurringStatus.ACTIVE,
                lastGeneratedOccurrenceDate = item.dueDate
            )
            val id = utilityDao.insertRecurringItem(entity)
            
            // Seed initial occurrence
            utilityDao.insertOccurrence(
                RecurringOccurrenceEntity(
                    recurringItemId = id,
                    scheduledDate = item.dueDate,
                    status = OccurrenceStatus.PENDING,
                    recurringItemNameSnapshot = item.title
                )
            )
            
            generateOccurrences(id)
        }
    }

    override suspend fun updateRecurringItem(item: RecurringItem) {
        database.withTransaction {
            val existing = utilityDao.getRecurringItemById(item.id) ?: return@withTransaction
            
            // Check if schedule-affecting fields changed (Compare date precisely)
            val oldCal = Calendar.getInstance().apply { timeInMillis = existing.dueDate }
            val newCal = Calendar.getInstance().apply { timeInMillis = item.dueDate }
            
            val dateChanged = oldCal.get(Calendar.YEAR) != newCal.get(Calendar.YEAR) ||
                             oldCal.get(Calendar.DAY_OF_YEAR) != newCal.get(Calendar.DAY_OF_YEAR)

            val scheduleChanged = dateChanged || 
                                 existing.frequency != RecurrenceFrequency.valueOf(item.frequency) ||
                                 existing.frequencyInterval != item.frequencyInterval ||
                                 existing.frequencyUnit != item.frequencyUnit

            // 1. Update the template
            utilityDao.updateRecurringItem(item.toEntity().copy(
                lastGeneratedOccurrenceDate = if (scheduleChanged) null else existing.lastGeneratedOccurrenceDate
            ))

            // 2. Handle pending occurrences
            if (scheduleChanged) {
                // Delete ALL pending occurrences to re-align with the new base date/frequency
                utilityDao.deleteAllPendingOccurrences(item.id)
                
                // Re-seed the first one at the new dueDate
                utilityDao.insertOccurrence(
                    RecurringOccurrenceEntity(
                        recurringItemId = item.id,
                        scheduledDate = item.dueDate,
                        status = OccurrenceStatus.PENDING,
                        recurringItemNameSnapshot = item.title
                    )
                )
                
                // Re-generate future ones
                generateOccurrences(item.id)
            } else {
                // Template changes (amount, default account) automatically reflect in the UI
                // Update snapshots for existing PENDING occurrences if needed
                val pendingOccurrences = utilityDao.getOccurrencesByItemIdSync(item.id)
                    .filter { it.status == OccurrenceStatus.PENDING }
                
                pendingOccurrences.forEach { occ ->
                    utilityDao.updateOccurrence(occ.copy(recurringItemNameSnapshot = item.title))
                }
            }
        }
    }

    override suspend fun deleteRecurringItem(item: RecurringItem) {
        database.withTransaction {
            // 1. Delete ALL pending occurrences (keep PAID/SKIPPED)
            utilityDao.deleteAllPendingOccurrences(item.id)
            
            // 2. Delete the template
            utilityDao.deleteRecurringItem(item.toEntity())
        }
    }

    override suspend fun processDueRecurringTransactions(): Int {
        val currentTime = System.currentTimeMillis()
        val dueOccurrences = utilityDao.getDueOccurrencesSync(currentTime)
        var processedCount = 0

        for (occurrenceEntity in dueOccurrences) {
            var notificationInfo: Pair<String, String>? = null

            database.withTransaction {
                val occurrence = utilityDao.getOccurrenceById(occurrenceEntity.id) ?: return@withTransaction
                if (occurrence.status != OccurrenceStatus.PENDING || occurrence.transactionId != null) {
                    return@withTransaction
                }

                val itemId = occurrence.recurringItemId ?: return@withTransaction
                val itemEntity = utilityDao.getRecurringItemById(itemId) ?: return@withTransaction
                val item = itemEntity.toDomainModel()

                if (item.isAutoPay) {
                    val accountId = item.accountId
                    if (accountId != null) {
                        val account = accountDao.getAccountByIdSync(accountId)
                        if (account != null) {
                            val timestamp = System.currentTimeMillis()
                            val isLiability = account.category == AccountCategory.LIABILITIES

                            val transactionId = transactionDao.insertTransaction(
                                TransactionEntity(
                                    title = item.title,
                                    amount = item.amount,
                                    type = TransactionType.EXPENSE,
                                    categoryId = item.categoryId,
                                    accountId = accountId,
                                    timestamp = timestamp,
                                    note = "AutoPay for ${item.category}",
                                    accountNameSnapshot = account.name,
                                    accountCategorySnapshot = account.category
                                )
                            )

                            val newBalance = if (isLiability) account.balance + item.amount else account.balance - item.amount
                            accountDao.updateAccount(account.copy(balance = newBalance))

                            utilityDao.updateOccurrence(
                                occurrence.copy(
                                    status = OccurrenceStatus.PAID,
                                    transactionId = transactionId,
                                    paymentDate = timestamp,
                                    accountId = accountId,
                                    recurringItemNameSnapshot = item.title,
                                    accountNameSnapshot = account.name
                                )
                            )

                            generateOccurrences(item.id)
                            utilityDao.updateRecurringItem(itemEntity.copy(lastPaidDate = timestamp))

                            val preferences = userPreferencesRepository.userPreferences.first()
                            val formattedAmount = CurrencyFormatter.format(item.amount, preferences)
                            notificationInfo = "AutoPay Successful" to "$formattedAmount paid for ${item.title}"
                            processedCount++
                        } else {
                            notificationInfo = "AutoPay Blocked" to "${item.title} requires a valid payment account."
                        }
                    } else {
                        notificationInfo = "AutoPay Blocked" to "${item.title} requires a payment account to process automatically."
                    }
                } else {
                    val preferences = userPreferencesRepository.userPreferences.first()
                    val formattedAmount = CurrencyFormatter.format(item.amount, preferences)
                    notificationInfo = "Payment Due" to "$formattedAmount due for ${item.title}"
                }
            }

            // Post notification after transaction successfully commits
            notificationInfo?.let { (title, msg) ->
                NotificationHelper.showPaymentNotification(context, title, msg)
            }
        }
        return processedCount
    }

    override suspend fun markAsPaid(occurrenceId: Long, accountId: Long) {
        database.withTransaction {
            val occurrence = utilityDao.getOccurrenceById(occurrenceId) ?: return@withTransaction
            if (occurrence.status != OccurrenceStatus.PENDING) return@withTransaction
            
            val itemId = occurrence.recurringItemId ?: return@withTransaction
            val item = utilityDao.getRecurringItemById(itemId) ?: return@withTransaction
            val account = accountDao.getAccountByIdSync(accountId) ?: return@withTransaction
            
            val timestamp = System.currentTimeMillis()
            
            // 1. Create Transaction
            val isLiability = account.category == AccountCategory.LIABILITIES
            val transactionId = transactionDao.insertTransaction(
                TransactionEntity(
                    title = item.title,
                    amount = item.amount,
                    type = TransactionType.EXPENSE,
                    categoryId = item.categoryId,
                    accountId = accountId,
                    timestamp = timestamp,
                    note = "Recurring payment for ${item.category}",
                    accountNameSnapshot = account.name,
                    accountCategorySnapshot = account.category
                )
            )
            
            // 2. Update Account Balance
            val newBalance = if (isLiability) account.balance + item.amount else account.balance - item.amount
            accountDao.updateAccount(account.copy(balance = newBalance))
            
            // 3. Update Occurrence
            utilityDao.updateOccurrence(
                occurrence.copy(
                    status = OccurrenceStatus.PAID,
                    transactionId = transactionId,
                    paymentDate = timestamp,
                    accountId = accountId,
                    recurringItemNameSnapshot = item.title,
                    accountNameSnapshot = account.name
                )
            )
            
            // 4. Maintenance
            generateOccurrences(item.id)
            utilityDao.updateRecurringItem(item.copy(lastPaidDate = timestamp))
        }
    }

    override suspend fun skipOccurrence(occurrenceId: Long) {
        database.withTransaction {
            val occurrence = utilityDao.getOccurrenceById(occurrenceId) ?: return@withTransaction
            if (occurrence.status != OccurrenceStatus.PENDING) return@withTransaction
            
            utilityDao.updateOccurrence(occurrence.copy(status = OccurrenceStatus.SKIPPED))
            if (occurrence.recurringItemId != null) {
                generateOccurrences(occurrence.recurringItemId)
            }
        }
    }

    override suspend fun undoPayment(occurrenceId: Long) {
        database.withTransaction {
            val occurrence = utilityDao.getOccurrenceById(occurrenceId) ?: return@withTransaction
            if (occurrence.status != OccurrenceStatus.PAID) return@withTransaction
            
            val transactionId = occurrence.transactionId ?: return@withTransaction
            val transaction = transactionDao.getTransactionById(transactionId) ?: return@withTransaction
            
            // 1. Delete Transaction
            transactionDao.deleteTransaction(transaction)
            
            // 2. Revert Balance
            val accountId = transaction.accountId
            if (accountId != null) {
                val account = accountDao.getAccountByIdSync(accountId)
                if (account != null) {
                    val isLiability = account.category == AccountCategory.LIABILITIES
                    val revertedBalance = if (isLiability) account.balance - transaction.amount else account.balance + transaction.amount
                    accountDao.updateAccount(account.copy(balance = revertedBalance))
                }
            }
            
            // 3. Revert Occurrence
            utilityDao.updateOccurrence(
                occurrence.copy(
                    status = OccurrenceStatus.PENDING,
                    transactionId = null,
                    paymentDate = null,
                    accountId = null
                )
            )
        }
    }

    override suspend fun toggleRecurringItemStatus(itemId: Long) {
        database.withTransaction {
            val item = utilityDao.getRecurringItemById(itemId) ?: return@withTransaction
            val newStatus = if (item.status == RecurringStatus.ACTIVE) RecurringStatus.PAUSED else RecurringStatus.ACTIVE
            
            utilityDao.updateRecurringItem(item.copy(status = newStatus))
            
            if (newStatus == RecurringStatus.PAUSED) {
                // Delete all future PENDING occurrences
                utilityDao.deleteFuturePendingOccurrences(itemId, System.currentTimeMillis())
            } else {
                // Re-generate occurrences
                generateOccurrences(itemId)
            }
        }
    }

    override fun getActiveRecurringItems(): Flow<List<RecurringItem>> {
        return utilityDao.getActiveRecurringItems().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getOccurrences(itemId: Long): Flow<List<RecurringOccurrence>> {
        return utilityDao.getOccurrencesByItemId(itemId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getUpcomingOccurrences(limit: Int): Flow<List<RecurringOccurrence>> {
        return utilityDao.getUpcomingPendingOccurrences(limit).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    private suspend fun generateOccurrences(itemId: Long, horizonMonths: Int = 6) {
        val item = utilityDao.getRecurringItemById(itemId) ?: return
        val horizon = System.currentTimeMillis() + (horizonMonths.toLong() * 30 * 24 * 60 * 60 * 1000L)
        
        // Anchor on the latest existing occurrence (paid, skipped, or pending)
        // Use sync query to get latest state inside transaction
        val existingOccurrences = utilityDao.getOccurrencesByItemIdSync(itemId)
        val latestScheduledDate = existingOccurrences.maxByOrNull { it.scheduledDate }?.scheduledDate ?: item.dueDate
        
        var currentDueDate = latestScheduledDate
        val occurrences = mutableListOf<RecurringOccurrenceEntity>()
        
        // Safety: don't generate more than 100 at once
        var count = 0
        while (currentDueDate < horizon && count < 100) {
            currentDueDate = DateUtils.calculateNextDueDate(
                currentDueDate, 
                item.frequencyInterval, 
                item.frequencyUnit
            )
            occurrences.add(
                RecurringOccurrenceEntity(
                    recurringItemId = itemId,
                    scheduledDate = currentDueDate,
                    status = OccurrenceStatus.PENDING,
                    recurringItemNameSnapshot = item.title
                )
            )
            count++
        }
        
        if (occurrences.isNotEmpty()) {
            utilityDao.insertOccurrences(occurrences)
            utilityDao.updateRecurringItem(item.copy(lastGeneratedOccurrenceDate = currentDueDate))
        }
    }
}
