package sayan.apps.rupeeflow.feature.dashboard

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import sayan.apps.rupeeflow.core.database.entity.GoalContributionEntity
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.model.*
import sayan.apps.rupeeflow.domain.repository.*

class DashboardViewModelTest {

    private lateinit var viewModel: DashboardViewModel
    private lateinit var transactionRepository: FakeTransactionRepository
    private lateinit var accountRepository: FakeAccountRepository
    private lateinit var recurringRepository: FakeRecurringRepository
    private lateinit var planningRepository: FakePlanningRepository

    @Before
    fun setup() {
        transactionRepository = FakeTransactionRepository()
        accountRepository = FakeAccountRepository()
        recurringRepository = FakeRecurringRepository()
        planningRepository = FakePlanningRepository()
        viewModel = DashboardViewModel(transactionRepository, accountRepository, recurringRepository, planningRepository)
    }

    @Test
    fun `uiState correctly calculates net worth and trend`() = runTest {
        val currentTime = System.currentTimeMillis()
        val startOfMonth = DateUtils.getStartOfMonth(currentTime)
        
        accountRepository.emitAccounts(listOf(
            Account(id = 1, name = "Bank", category = "BANKING", subType = "SAVINGS", balance = 50000.0)
        ))
        accountRepository.emitNetWorthHistory(listOf(
            startOfMonth to 45000.0,
            currentTime to 50000.0
        ))

        val state = viewModel.uiState.first { !it.isLoading }
        
        assertEquals(50000.0, state.netWorth, 0.1)
        assertEquals(5000.0, state.netWorthTrend, 0.1)
    }

    @Test
    fun `uiState correctly filters monthly income and spending`() = runTest {
        val currentTime = System.currentTimeMillis()
        val startOfMonth = DateUtils.getStartOfMonth(currentTime)
        
        transactionRepository.emitTransactions(listOf(
            Transaction(id = "1", title = "Salary", amount = 25000.0, timestamp = startOfMonth + 1000, category = "Salary", isIncome = true, type = TransactionType.INCOME),
            Transaction(id = "2", title = "Rent", amount = 10000.0, timestamp = startOfMonth + 2000, category = "Rent", isIncome = false, type = TransactionType.EXPENSE),
            Transaction(id = "3", title = "Transfer", amount = 5000.0, timestamp = startOfMonth + 3000, category = "Transfer", isIncome = false, type = TransactionType.TRANSFER),
            Transaction(id = "4", title = "Adjustment", amount = 2000.0, timestamp = startOfMonth + 4000, category = "Adjustment", isIncome = true, type = TransactionType.BALANCE_ADJUSTMENT)
        ))

        val state = viewModel.uiState.first { !it.isLoading }
        
        assertEquals(25000.0, state.monthlyIncome, 0.1)
        assertEquals(10000.0, state.monthlySpending, 0.1)
        assertEquals(15000.0, state.monthlySavings, 0.1)
    }

    @Test
    fun `uiState correctly identifies overdue payments and reconciliation needs`() = runTest {
        val currentTime = System.currentTimeMillis()
        val overdueTime = DateUtils.getTimestampDaysAgo(1)
        val oldReconciliationTime = DateUtils.getTimestampDaysAgo(15)
        
        accountRepository.emitAccounts(listOf(
            Account(id = 1, name = "HDFC", category = "BANKING", subType = "SAVINGS", balance = 10000.0, lastReconciledAt = oldReconciliationTime)
        ))
        recurringRepository.emitUpcomingOccurrences(listOf(
            RecurringOccurrence(id = 1, recurringItemId = 101, scheduledDate = overdueTime, status = "PENDING", recurringItemNameSnapshot = "Netflix", amount = 199.0, accountId = 1)
        ))

        val state = viewModel.uiState.first { !it.isLoading }
        
        assertEquals(2, state.attentionItems.size)
        assertTrue(state.attentionItems.any { it is AttentionItem.OverduePayment })
        assertTrue(state.attentionItems.any { it is AttentionItem.ReconcileAccount })
    }

    private fun assertTrue(condition: Boolean) {
        assert(condition)
    }
}

class FakeTransactionRepository : TransactionRepository {
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    fun emitTransactions(list: List<Transaction>) { transactionsFlow.value = list }
    override fun getTransactions(): Flow<List<Transaction>> = transactionsFlow
    override suspend fun getTransactionById(id: Long): Transaction? = null
    override suspend fun addTransaction(transaction: Transaction, accountId: Long?, categoryId: Long?): Long = 0
    override suspend fun updateTransaction(transaction: Transaction, accountId: Long?, categoryId: Long?) {}
    override suspend fun deleteTransaction(transaction: Transaction) {}
    override suspend fun deleteTransfer(transferId: String) {}
    override fun getCategorySpending(start: Long?, end: Long?): Flow<List<CategorySpending>> = MutableStateFlow(emptyList())
    override fun getSpendingTrend(start: Long?, end: Long?): Flow<List<TrendPoint>> = MutableStateFlow(emptyList())
    override fun getAttachments(transactionId: Long): Flow<List<Attachment>> = MutableStateFlow(emptyList())
    override suspend fun addAttachment(attachment: Attachment) {}
    override suspend fun deleteAttachmentsForTransaction(transactionId: Long) {}
    override fun getTransactionCountForCategory(categoryId: Long): Flow<Int> = MutableStateFlow(0)
    override fun getTotalAmountForCategory(categoryId: Long): Flow<Double?> = MutableStateFlow(0.0)
    override fun getTransactionsForCategory(categoryId: Long): Flow<List<Transaction>> = MutableStateFlow(emptyList())
    override suspend fun getMonthlySpending(start: Long, end: Long): Double? = 0.0
    override suspend fun getTotalIncomeInRange(start: Long, end: Long): Double? = 0.0
    override suspend fun getCategorySpendingInRange(categoryId: Long, start: Long, end: Long): Double? = 0.0
    override suspend fun getBiggestExpenseInRange(start: Long, end: Long): Transaction? = null
    override suspend fun getTransactionCountInRange(start: Long, end: Long): Int = 0
    override suspend fun getTransactionsInRangeSync(start: Long, end: Long): List<Transaction> = emptyList()
}

class FakeAccountRepository : AccountRepository {
    private val accountsFlow = MutableStateFlow<List<Account>>(emptyList())
    private val netWorthHistoryFlow = MutableStateFlow<List<Pair<Long, Double>>>(emptyList())
    fun emitAccounts(list: List<Account>) { accountsFlow.value = list }
    fun emitNetWorthHistory(list: List<Pair<Long, Double>>) { netWorthHistoryFlow.value = list }
    override fun getAccounts(): Flow<List<Account>> = accountsFlow
    override fun getAccountsIncludingClosed(): Flow<List<Account>> = accountsFlow
    override suspend fun getAccountById(id: Long): Account? = null
    override suspend fun addAccount(account: Account) {}
    override suspend fun updateAccount(account: Account) {}
    override suspend fun deleteAccount(account: Account) {}
    override suspend fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long) {}
    override suspend fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long) {}
    override suspend fun reconcileAccount(accountId: Long, actualBalance: Double, reason: String?, note: String?) {}
    override fun getNetWorthHistory(days: Int): Flow<List<Pair<Long, Double>>> = netWorthHistoryFlow
    override fun getDebitCardsForAccount(accountId: Long): Flow<List<DebitCard>> = MutableStateFlow(emptyList())
    override suspend fun getDebitCardById(id: Long): DebitCard? = null
    override suspend fun addDebitCard(debitCard: DebitCard): Long = 0L
    override suspend fun updateDebitCard(debitCard: DebitCard) {}
    override suspend fun deleteDebitCard(debitCard: DebitCard) {}
    override fun getUpiAppsForAccount(accountId: Long): Flow<List<SavedUpiApp>> = MutableStateFlow(emptyList())
    override suspend fun addUpiApp(upiApp: SavedUpiApp): Long = 0L
    override suspend fun updateUpiApp(upiApp: SavedUpiApp) {}
    override suspend fun deleteUpiApp(upiApp: SavedUpiApp) {}
}

class FakeRecurringRepository : RecurringRepository {
    private val upcomingOccurrencesFlow = MutableStateFlow<List<RecurringOccurrence>>(emptyList())
    fun emitUpcomingOccurrences(list: List<RecurringOccurrence>) { upcomingOccurrencesFlow.value = list }
    override fun getRecurringItems(): Flow<List<RecurringItem>> = MutableStateFlow(emptyList())
    override suspend fun getRecurringItemById(id: Long): RecurringItem? = null
    override suspend fun addRecurringItem(item: RecurringItem) {}
    override suspend fun updateRecurringItem(item: RecurringItem) {}
    override suspend fun deleteRecurringItem(item: RecurringItem) {}
    override suspend fun processDueRecurringTransactions(): Int = 0
    override suspend fun markAsPaid(occurrenceId: Long, accountId: Long) {}
    override suspend fun skipOccurrence(occurrenceId: Long) {}
    override suspend fun undoPayment(occurrenceId: Long) {}
    override suspend fun toggleRecurringItemStatus(itemId: Long) {}
    override fun getActiveRecurringItems(): Flow<List<RecurringItem>> = MutableStateFlow(emptyList())
    override fun getOccurrences(itemId: Long): Flow<List<RecurringOccurrence>> = MutableStateFlow(emptyList())
    override fun getUpcomingOccurrences(limit: Int): Flow<List<RecurringOccurrence>> = upcomingOccurrencesFlow
}

class FakePlanningRepository : PlanningRepository {
    override fun getBudgetsWithProgress(start: Long?, end: Long?): Flow<List<BudgetWithProgress>> = MutableStateFlow(emptyList())
    override fun getGoals(): Flow<List<Goal>> = MutableStateFlow(emptyList())
    override suspend fun addBudget(budget: Budget) {}
    override suspend fun updateBudget(budget: Budget) {}
    override suspend fun deleteBudget(budget: Budget) {}
    override suspend fun addGoal(goal: Goal) {}
    override suspend fun updateGoal(goal: Goal) {}
    override suspend fun deleteGoal(goal: Goal) {}
    override suspend fun addGoalContribution(goalId: Long, amount: Double, note: String?) {}
    override suspend fun deleteGoalContribution(goalId: Long, contributionId: Long, contributionAmount: Double) {}
    override fun getGoalContributions(goalId: Long): Flow<List<GoalContributionEntity>> = MutableStateFlow(emptyList())
    override fun getSafeSpendingLimit(): Flow<Double> = MutableStateFlow(0.0)
}
