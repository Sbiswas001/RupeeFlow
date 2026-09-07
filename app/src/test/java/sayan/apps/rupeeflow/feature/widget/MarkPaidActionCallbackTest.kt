package sayan.apps.rupeeflow.feature.widget

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.AccountEntity
import sayan.apps.rupeeflow.core.database.entity.AccountSubType
import sayan.apps.rupeeflow.core.database.entity.OccurrenceStatus
import sayan.apps.rupeeflow.core.database.entity.RecurrenceFrequency
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringOccurrenceEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.core.database.repository.DummyContext
import sayan.apps.rupeeflow.core.database.repository.FakeAccountDao
import sayan.apps.rupeeflow.core.database.repository.FakeRupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.repository.FakeTransactionDao
import sayan.apps.rupeeflow.core.database.repository.FakeUserPreferencesRepo
import sayan.apps.rupeeflow.core.database.repository.FakeUtilityDao
import sayan.apps.rupeeflow.core.database.repository.RecurringRepositoryImpl

@OptIn(ExperimentalCoroutinesApi::class)
class MarkPaidActionCallbackTest {

    private lateinit var db: FakeRupeeFlowDatabase
    private lateinit var utilityDao: FakeUtilityDao
    private lateinit var transactionDao: FakeTransactionDao
    private lateinit var accountDao: FakeAccountDao
    private lateinit var userPreferencesRepo: FakeUserPreferencesRepo
    private lateinit var repository: RecurringRepositoryImpl

    @Before
    fun setUp() {
        db = FakeRupeeFlowDatabase()
        utilityDao = FakeUtilityDao()
        transactionDao = FakeTransactionDao()
        accountDao = FakeAccountDao()
        userPreferencesRepo = FakeUserPreferencesRepo()

        repository = RecurringRepositoryImpl(
            database = db,
            utilityDao = utilityDao,
            transactionDao = transactionDao,
            accountDao = accountDao,
            context = DummyContext(),
            userPreferencesRepository = userPreferencesRepo
        )
    }

    @Test
    fun markAsPaid_processesOccurrenceAndUpdatesBalanceIdempotently() = runTest {
        // 1. Setup account
        val accountId = accountDao.insertAccount(
            AccountEntity(
                name = "Bank",
                category = AccountCategory.BANKING,
                subType = AccountSubType.SAVINGS,
                balance = 10000.0
            )
        )

        // 2. Setup recurring item & occurrence
        val itemId = utilityDao.insertRecurringItem(
            RecurringEntity(
                title = "Jio Fiber",
                amount = 899.0,
                dueDate = System.currentTimeMillis(),
                isAutoPay = false,
                status = RecurringStatus.ACTIVE,
                frequency = RecurrenceFrequency.MONTHLY,
                category = "Bills"
            )
        )

        val occurrenceId = utilityDao.insertOccurrence(
            RecurringOccurrenceEntity(
                recurringItemId = itemId,
                scheduledDate = System.currentTimeMillis(),
                status = OccurrenceStatus.PENDING,
                recurringItemNameSnapshot = "Jio Fiber"
            )
        )

        // 3. First execution of markAsPaid
        repository.markAsPaid(occurrenceId, accountId)

        // Verify transaction created
        assertEquals(1, transactionDao.transactions.size)
        val transaction = transactionDao.transactions.values.firstOrNull()
        assertNotNull(transaction)
        assertEquals("Jio Fiber", transaction?.title)
        assertEquals(899.0, transaction?.amount ?: 0.0, 0.01)

        // Verify account balance updated (10000 - 899 = 9101)
        val updatedAccount = accountDao.getAccountByIdSync(accountId)
        assertNotNull(updatedAccount)
        assertEquals(9101.0, updatedAccount!!.balance, 0.01)

        // Verify occurrence status is PAID
        val occurrence = utilityDao.getOccurrenceById(occurrenceId)
        assertNotNull(occurrence)
        assertEquals(OccurrenceStatus.PAID, occurrence!!.status)

        // 4. SECOND execution for same occurrence (Idempotency check)
        repository.markAsPaid(occurrenceId, accountId)

        // Verify NO DUPLICATE transaction created (still 1 transaction)
        assertEquals(1, transactionDao.transactions.size)

        // Verify NO DUPLICATE balance deduction (still 9101.0)
        val finalAccount = accountDao.getAccountByIdSync(accountId)
        assertEquals(9101.0, finalAccount!!.balance, 0.01)
    }
}
