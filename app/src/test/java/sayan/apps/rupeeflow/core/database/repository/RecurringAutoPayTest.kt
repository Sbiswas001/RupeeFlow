package sayan.apps.rupeeflow.core.database.repository

import android.content.Context
import android.content.ContextWrapper
import androidx.room.DatabaseConfiguration
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.dao.AccountDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.dao.TransactionWithCategory
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.AccountEntity
import sayan.apps.rupeeflow.core.database.entity.AccountSubType
import sayan.apps.rupeeflow.core.database.entity.AttachmentEntity
import sayan.apps.rupeeflow.core.database.entity.OccurrenceStatus
import sayan.apps.rupeeflow.core.database.entity.RecurrenceFrequency
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringOccurrenceEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.model.CategoryTotal
import sayan.apps.rupeeflow.core.database.model.DailyTrend
import sayan.apps.rupeeflow.domain.model.LockTimeout
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository

import sayan.apps.rupeeflow.core.database.dao.CategoryDao
import sayan.apps.rupeeflow.core.database.dao.DebitCardDao
import sayan.apps.rupeeflow.core.database.dao.AccountUpiAppDao
import sayan.apps.rupeeflow.core.database.dao.PlanningDao
import sayan.apps.rupeeflow.core.database.dao.RecentSearchDao
import java.util.concurrent.Executors

class FakeRupeeFlowDatabase : RupeeFlowDatabase() {
    init {
        val executor = Executors.newSingleThreadExecutor()
        try {
            val field = RoomDatabase::class.java.getDeclaredField("internalTransactionExecutor")
            field.isAccessible = true
            field.set(this, executor)
        } catch (_: Exception) {}
    }

    @Suppress("DEPRECATION")
    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper = error("Stub")
    override fun createInvalidationTracker(): InvalidationTracker = error("Stub")
    override fun clearAllTables() {}
    override fun runInTransaction(body: Runnable) { body.run() }
    override fun beginTransaction() {}
    override fun endTransaction() {}
    override fun setTransactionSuccessful() {}
    override fun inTransaction(): Boolean = false
    override fun transactionDao(): TransactionDao = error("Stub")
    override fun accountDao(): AccountDao = error("Stub")
    override fun categoryDao(): CategoryDao = error("Stub")
    override fun planningDao(): PlanningDao = error("Stub")
    override fun utilityDao(): UtilityDao = error("Stub")
    override fun recentSearchDao(): RecentSearchDao = error("Stub")
    override fun debitCardDao(): DebitCardDao = error("Stub")
    override fun accountUpiAppDao(): AccountUpiAppDao = error("Stub")
}

class FakeUtilityDao : UtilityDao {
    val items = mutableMapOf<Long, RecurringEntity>()
    val occurrences = mutableMapOf<Long, RecurringOccurrenceEntity>()
    private var nextItemId = 1L
    private var nextOccId = 1L

    override fun getAllRecurringItems(): Flow<List<RecurringEntity>> = MutableStateFlow(items.values.toList())
    override suspend fun getActiveRecurringItemsSync(): List<RecurringEntity> = items.values.filter { it.status == RecurringStatus.ACTIVE }
    override suspend fun getRecurringItemById(id: Long): RecurringEntity? = items[id]
    override suspend fun insertRecurringItem(item: RecurringEntity): Long {
        val id = if (item.id == 0L) nextItemId++ else item.id
        items[id] = item.copy(id = id)
        return id
    }
    override suspend fun updateRecurringItem(item: RecurringEntity) { items[item.id] = item }
    override suspend fun deleteRecurringItem(item: RecurringEntity) { items.remove(item.id) }
    override fun searchRecurringItems(query: String): Flow<List<RecurringEntity>> = MutableStateFlow(emptyList())
    override fun getActiveRecurringItems(): Flow<List<RecurringEntity>> = MutableStateFlow(items.values.toList())

    override suspend fun insertOccurrences(occurrencesList: List<RecurringOccurrenceEntity>) {
        occurrencesList.forEach { insertOccurrence(it) }
    }
    override suspend fun insertOccurrence(occurrence: RecurringOccurrenceEntity): Long {
        val id = if (occurrence.id == 0L) nextOccId++ else occurrence.id
        occurrences[id] = occurrence.copy(id = id)
        return id
    }
    override suspend fun updateOccurrence(occurrence: RecurringOccurrenceEntity) {
        occurrences[occurrence.id] = occurrence
    }
    override fun getOccurrencesByItemId(itemId: Long): Flow<List<RecurringOccurrenceEntity>> =
        MutableStateFlow(occurrences.values.filter { it.recurringItemId == itemId })
    override fun getAllOccurrences(): Flow<List<RecurringOccurrenceEntity>> =
        MutableStateFlow(occurrences.values.toList())
    override suspend fun getOccurrencesByItemIdSync(itemId: Long): List<RecurringOccurrenceEntity> =
        occurrences.values.filter { it.recurringItemId == itemId }
    override suspend fun getNextPendingOccurrence(itemId: Long): RecurringOccurrenceEntity? =
        occurrences.values.firstOrNull { it.recurringItemId == itemId && it.status == OccurrenceStatus.PENDING }
    override suspend fun getOccurrenceById(id: Long): RecurringOccurrenceEntity? = occurrences[id]
    override suspend fun getDueOccurrencesSync(currentTime: Long): List<RecurringOccurrenceEntity> =
        occurrences.values.filter { it.status == OccurrenceStatus.PENDING && it.scheduledDate <= currentTime }
    override suspend fun deleteFuturePendingOccurrences(itemId: Long, currentTime: Long) {
        occurrences.entries.removeIf { it.value.recurringItemId == itemId && it.value.status == OccurrenceStatus.PENDING && it.value.scheduledDate > currentTime }
    }
    override suspend fun deleteAllPendingOccurrences(itemId: Long) {
        occurrences.entries.removeIf { it.value.recurringItemId == itemId && it.value.status == OccurrenceStatus.PENDING }
    }
    override suspend fun clearAllRecurringItems() { items.clear() }
    override suspend fun clearAllOccurrences() { occurrences.clear() }
    override fun getUpcomingPendingOccurrences(limit: Int): Flow<List<UtilityDao.OccurrenceWithItem>> = MutableStateFlow(emptyList())
    override fun getAttachmentsForTransaction(transactionId: Long): Flow<List<AttachmentEntity>> = MutableStateFlow(emptyList())
    override suspend fun insertAttachment(attachment: AttachmentEntity): Long = 1L
    override suspend fun deleteAttachmentsForTransaction(transactionId: Long) {}
}

class FakeAccountDao : AccountDao {
    val accounts = mutableMapOf<Long, AccountEntity>()
    private var nextId = 1L

    override fun getAllAccounts(): Flow<List<AccountEntity>> = MutableStateFlow(accounts.values.toList())
    override fun getAllAccountsIncludingClosed(): Flow<List<AccountEntity>> = MutableStateFlow(accounts.values.toList())
    override suspend fun getAccountById(id: Long): AccountEntity? = accounts[id]
    override fun getAccountByIdSync(id: Long): AccountEntity? = accounts[id]
    override suspend fun insertAccount(account: AccountEntity): Long {
        val id = if (account.id == 0L) nextId++ else account.id
        accounts[id] = account.copy(id = id)
        return id
    }
    override suspend fun updateAccount(account: AccountEntity) { accounts[account.id] = account }
    override suspend fun deleteAccount(account: AccountEntity) { accounts.remove(account.id) }
    override fun getTotalBalance(): Flow<Double?> = MutableStateFlow(accounts.values.sumOf { it.balance })
    override fun searchAccounts(query: String): Flow<List<AccountEntity>> = MutableStateFlow(emptyList())
    override suspend fun clearAllAccounts() { accounts.clear() }
}

class FakeTransactionDao : TransactionDao {
    val transactions = mutableMapOf<Long, TransactionEntity>()
    private var nextId = 1L

    override fun getAllTransactionsWithCategory(): Flow<List<TransactionWithCategory>> = MutableStateFlow(emptyList())
    override fun getAllTransactions(): Flow<List<TransactionEntity>> = MutableStateFlow(transactions.values.toList())
    override suspend fun getTransactionById(id: Long): TransactionEntity? = transactions[id]
    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = if (transaction.id == 0L) nextId++ else transaction.id
        transactions[id] = transaction.copy(id = id)
        return id
    }
    override suspend fun updateTransaction(transaction: TransactionEntity) { transactions[transaction.id] = transaction }
    override suspend fun deleteTransaction(transaction: TransactionEntity) { transactions.remove(transaction.id) }
    override fun getTotalIncome(): Flow<Double?> = MutableStateFlow(null)
    override fun getTotalExpense(): Flow<Double?> = MutableStateFlow(null)
    override fun getCategoryTotalsInRange(start: Long, end: Long): Flow<List<CategoryTotal>> = MutableStateFlow(emptyList())
    override fun getDailySpendingTrendInRange(start: Long, end: Long): Flow<List<DailyTrend>> = MutableStateFlow(emptyList())
    override fun getTransactionCountForCategory(categoryId: Long): Flow<Int> = MutableStateFlow(0)
    override fun getTotalAmountForCategory(categoryId: Long): Flow<Double?> = MutableStateFlow(null)
    override fun getCategorySpendingInRangeFlow(categoryId: Long, start: Long, end: Long): Flow<Double?> = MutableStateFlow(null)
    override fun getTransactionsForCategory(categoryId: Long): Flow<List<TransactionEntity>> = MutableStateFlow(emptyList())
    override suspend fun getMonthlySpending(start: Long, end: Long): Double? = null
    override suspend fun getTotalIncomeInRange(start: Long, end: Long): Double? = null
    override suspend fun getCategorySpendingInRange(categoryId: Long, start: Long, end: Long): Double? = null
    override suspend fun getBiggestExpenseInRange(start: Long, end: Long): TransactionEntity? = null
    override suspend fun getTransactionCountInRange(start: Long, end: Long): Int = 0
    override fun getTransactionsInRangeSync(start: Long, end: Long): List<TransactionEntity> = emptyList()
    override suspend fun clearAllTransactions() { transactions.clear() }
    override suspend fun getLastReconciliationForAccount(accountId: Long, excludeId: Long): TransactionEntity? = null
    override suspend fun getTransactionsByTransferId(transferId: String): List<TransactionEntity> = emptyList()
    override fun searchTransactions(
        query: String, minAmount: Double?, maxAmount: Double?, startDate: Long?, endDate: Long?,
        categoryId: Long?, accountId: Long?, type: TransactionType?, isRecurring: Boolean?
    ): Flow<List<TransactionWithCategory>> = MutableStateFlow(emptyList())
}

class FakeUserPreferencesRepo : UserPreferencesRepository {
    override val userPreferences = flowOf(UserPreferences())
    override suspend fun updateCurrency(currency: String) {}
    override suspend fun updateFirstDayOfWeek(day: Int) {}
    override suspend fun updateUseFinancialYear(use: Boolean) {}
    override suspend fun updateIndianNumberFormat(use: Boolean) {}
    override suspend fun updateDefaultAccountId(id: Long) {}
    override suspend fun updateDefaultCategoryId(id: Long) {}
    override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {}
    override suspend fun updateConfirmBeforeDelete(confirm: Boolean) {}
    override suspend fun updateAutoSaveDrafts(autoSave: Boolean) {}
    override suspend fun updateAmoledBlack(enabled: Boolean) {}
    override suspend fun updateDynamicColor(enabled: Boolean) {}
    override suspend fun updateBillReminders(enabled: Boolean) {}
    override suspend fun updateBudgetAlerts(enabled: Boolean) {}
    override suspend fun updateGoalReminders(enabled: Boolean) {}
    override suspend fun updateAppLock(enabled: Boolean) {}
    override suspend fun updateFingerprintUnlock(enabled: Boolean) {}
    override suspend fun updateLockTimeout(timeout: LockTimeout) {}
    override suspend fun updateEncryptedPinMaterial(material: String?) {}
    override suspend fun updateFailedAttempts(attempts: Int) {}
    override suspend fun updateCooldownEndTimeMillis(timestamp: Long) {}
    override suspend fun updateHideBalances(enabled: Boolean) {}
    override suspend fun updateScreenshotProtection(enabled: Boolean) {}
    override suspend fun updateLastBackupTimestamp(timestamp: Long) {}
    override suspend fun updateFirstRun(isFirstRun: Boolean) {}
    override suspend fun updateDeveloperModeEnabled(enabled: Boolean) {}
    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {}
    override suspend fun updateAiModelVerification(verified: Boolean, modelId: String, sha256: String, sizeBytes: Long) {}
}

class DummyContext : ContextWrapper(null) {
    override fun getApplicationContext(): Context = this
    override fun getSystemService(name: String): Any? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringAutoPayTest {

    private lateinit var database: FakeRupeeFlowDatabase
    private lateinit var utilityDao: FakeUtilityDao
    private lateinit var accountDao: FakeAccountDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var repository: RecurringRepositoryImpl

    @Before
    fun setup() {
        database = FakeRupeeFlowDatabase()
        utilityDao = FakeUtilityDao()
        accountDao = FakeAccountDao()
        transactionDao = FakeTransactionDao()

        val context = DummyContext()

        repository = RecurringRepositoryImpl(
            database = database,
            utilityDao = utilityDao,
            transactionDao = transactionDao,
            accountDao = accountDao,
            context = context,
            userPreferencesRepository = FakeUserPreferencesRepo()
        )
    }

    @Test
    fun `processDueRecurringTransactions processes overdue AutoPay items and creates transaction`() = runTest {
        val accountId = accountDao.insertAccount(
            AccountEntity(name = "SBI Savings", category = AccountCategory.BANKING, subType = AccountSubType.SAVINGS, balance = 10000.0)
        )

        val now = System.currentTimeMillis()
        val scheduledPastDate = now - (24 * 60 * 60 * 1000L) // Yesterday

        val itemId = utilityDao.insertRecurringItem(
            RecurringEntity(
                title = "Jio Fiber",
                amount = 470.82,
                dueDate = scheduledPastDate,
                isAutoPay = true,
                status = RecurringStatus.ACTIVE,
                frequency = RecurrenceFrequency.MONTHLY,
                category = "Bills",
                defaultAccountId = accountId
            )
        )

        val occurrenceId = utilityDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringItemId = itemId,
                scheduledDate = scheduledPastDate,
                status = OccurrenceStatus.PENDING,
                recurringItemNameSnapshot = "Jio Fiber"
            )
        )

        val processedCount = repository.processDueRecurringTransactions()

        assertEquals(1, processedCount)

        val updatedOccurrence = utilityDao.getOccurrenceById(occurrenceId)
        assertNotNull(updatedOccurrence)
        assertEquals(OccurrenceStatus.PAID, updatedOccurrence?.status)
        assertNotNull(updatedOccurrence?.transactionId)
        assertEquals(accountId, updatedOccurrence?.accountId)

        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals("Jio Fiber", transactions.first().title)
        assertEquals(470.82, transactions.first().amount, 0.01)

        val updatedAccount = accountDao.getAccountByIdSync(accountId)
        assertEquals(9529.18, updatedAccount?.balance ?: 0.0, 0.01)
    }

    @Test
    fun `processDueRecurringTransactions is idempotent on concurrent executions`() = runTest {
        val accountId = accountDao.insertAccount(
            AccountEntity(name = "SBI Savings", category = AccountCategory.BANKING, subType = AccountSubType.SAVINGS, balance = 10000.0)
        )

        val now = System.currentTimeMillis()
        val scheduledPastDate = now - 10000L

        val itemId = utilityDao.insertRecurringItem(
            RecurringEntity(
                title = "Jio Fiber",
                amount = 470.82,
                dueDate = scheduledPastDate,
                isAutoPay = true,
                status = RecurringStatus.ACTIVE,
                frequency = RecurrenceFrequency.MONTHLY,
                category = "Bills",
                defaultAccountId = accountId
            )
        )

        utilityDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringItemId = itemId,
                scheduledDate = scheduledPastDate,
                status = OccurrenceStatus.PENDING,
                recurringItemNameSnapshot = "Jio Fiber"
            )
        )

        val job1 = async { repository.processDueRecurringTransactions() }
        val job2 = async { repository.processDueRecurringTransactions() }

        val res1 = job1.await()
        val res2 = job2.await()

        assertEquals(1, res1 + res2)

        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(1, transactions.size)
    }

    @Test
    fun `processDueRecurringTransactions skips non AutoPay item`() = runTest {
        val accountId = accountDao.insertAccount(
            AccountEntity(name = "SBI Savings", category = AccountCategory.BANKING, subType = AccountSubType.SAVINGS, balance = 10000.0)
        )

        val now = System.currentTimeMillis()
        val scheduledPastDate = now - 10000L

        val itemId = utilityDao.insertRecurringItem(
            RecurringEntity(
                title = "Manual Rent",
                amount = 12000.0,
                dueDate = scheduledPastDate,
                isAutoPay = false,
                status = RecurringStatus.ACTIVE,
                frequency = RecurrenceFrequency.MONTHLY,
                category = "Housing",
                defaultAccountId = accountId
            )
        )

        val occurrenceId = utilityDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringItemId = itemId,
                scheduledDate = scheduledPastDate,
                status = OccurrenceStatus.PENDING,
                recurringItemNameSnapshot = "Manual Rent"
            )
        )

        val processedCount = repository.processDueRecurringTransactions()

        assertEquals(0, processedCount)

        val occurrence = utilityDao.getOccurrenceById(occurrenceId)
        assertEquals(OccurrenceStatus.PENDING, occurrence?.status)
        assertNull(occurrence?.transactionId)

        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun `processDueRecurringTransactions does not process when account missing`() = runTest {
        val now = System.currentTimeMillis()
        val scheduledPastDate = now - 10000L

        val itemId = utilityDao.insertRecurringItem(
            RecurringEntity(
                title = "Unlinked Subscription",
                amount = 199.0,
                dueDate = scheduledPastDate,
                isAutoPay = true,
                status = RecurringStatus.ACTIVE,
                frequency = RecurrenceFrequency.MONTHLY,
                category = "Entertainment",
                defaultAccountId = null
            )
        )

        val occurrenceId = utilityDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringItemId = itemId,
                scheduledDate = scheduledPastDate,
                status = OccurrenceStatus.PENDING,
                recurringItemNameSnapshot = "Unlinked Subscription"
            )
        )

        val processedCount = repository.processDueRecurringTransactions()

        assertEquals(0, processedCount)

        val occurrence = utilityDao.getOccurrenceById(occurrenceId)
        assertEquals(OccurrenceStatus.PENDING, occurrence?.status)
        assertNull(occurrence?.transactionId)

        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(0, transactions.size)
    }
}
