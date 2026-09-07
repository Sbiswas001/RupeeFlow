package sayan.apps.rupeeflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.FinancialHealthCalculator
import sayan.apps.rupeeflow.core.financial.FinancialHealthResult
import sayan.apps.rupeeflow.core.financial.SafeToSpendResult
import sayan.apps.rupeeflow.core.financial.SpendingForecastCalculator
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

sealed class AttentionItem {
    data class ReconcileAccount(val account: Account) : AttentionItem()
    data class OverduePayment(val occurrence: RecurringOccurrence) : AttentionItem()
    data class MissingAutoPayAccount(val itemId: Long, val itemName: String) : AttentionItem()
}

data class DashboardState(
    val isLoading: Boolean = true,
    val netWorth: Double = 0.0,
    val netWorthTrend: Double = 0.0,
    val netWorthTrendLabel: String = "",
    val netWorthHistory: List<Pair<Long, Double>> = emptyList(),
    val monthlyIncome: Double = 0.0,
    val monthlySpending: Double = 0.0,
    val monthlySavings: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val lastTransaction: Transaction? = null,
    val nextUpcomingOccurrence: RecurringOccurrence? = null,
    val attentionItems: List<AttentionItem> = emptyList(),
    val safeToSpendResult: SafeToSpendResult = SafeToSpendResult(0.0, 0.0, 0.0, 0, 0.0, false, "Set a monthly budget", ""),
    val healthResult: FinancialHealthResult = FinancialHealthResult(null, "Not enough data yet", emptyList(), false, emptyList())
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository,
    private val planningRepository: PlanningRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardState> = combine(
        accountRepository.getAccounts(),
        transactionRepository.getTransactions(),
        accountRepository.getNetWorthHistory(30),
        recurringRepository.getUpcomingOccurrences(10),
        combine(
            planningRepository.getBudgetsWithProgress(DateUtils.getStartOfMonth(System.currentTimeMillis()), DateUtils.getEndOfMonth(System.currentTimeMillis())),
            recurringRepository.getRecurringItems()
        ) { budgets, recurring -> Pair(budgets, recurring) }
    ) { accounts, transactions, netWorthHistory, allPendingOccurrences, (budgets, recurringItems) ->
        
        val currentTime = System.currentTimeMillis()
        val startOfMonth = DateUtils.getStartOfMonth(currentTime)
        val endOfMonth = DateUtils.getEndOfMonth(currentTime)
        val startOfToday = DateUtils.getStartOfToday()

        // 1. Net Worth
        val currentNetWorth = netWorthHistory.lastOrNull()?.second ?: 0.0
        
        // 2. Trend (Current vs Start of Month)
        val startOfMonthPoint = netWorthHistory.find { it.first >= startOfMonth }
        val startOfMonthNetWorth = startOfMonthPoint?.second 
            ?: netWorthHistory.firstOrNull()?.second 
            ?: 0.0
        val trend = currentNetWorth - startOfMonthNetWorth
        
        val isFirstDayOfMonth = currentTime < startOfMonth + (24 * 60 * 60 * 1000)
        val trendLabel = when {
            isFirstDayOfMonth && trend == 0.0 -> "Month started today"
            startOfMonthNetWorth == 0.0 -> "Started at zero this month"
            else -> {
                val percentage = (trend / abs(startOfMonthNetWorth)) * 100
                "(${String.format(Locale.ROOT, "%.1f", percentage)}%) this month"
            }
        }

        // 3. Monthly Stats (Exclude Transfers and Adjustments)
        val monthlyTransactions = transactions.filter { it.timestamp in startOfMonth..endOfMonth }
        val income = monthlyTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val spending = monthlyTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val savings = income - spending

        // 4. Attention Items
        val attention = mutableListOf<AttentionItem>()
        
        val accountMap = accounts.associateBy { it.id }
        
        // Enrich occurrences with parent account info
        val enrichedOccurrences = allPendingOccurrences.map { occ ->
            val parentAccount = occ.parentAccountId?.let { accountMap[it] }
            occ.copy(parentAccountName = parentAccount?.name)
        }

        // Overdue payments (Pending and scheduled before today) - Only most overdue per item
        val overdue = enrichedOccurrences
            .filter { it.scheduledDate < startOfToday }
            .groupBy { it.recurringItemId }
            .map { it.value.minBy { occ -> occ.scheduledDate } }
        
        overdue.forEach { attention.add(AttentionItem.OverduePayment(it)) }

        // Reconciliation needed (> 7 days)
        val sevenDaysAgo = DateUtils.getTimestampDaysAgo(7)
        accounts.filter { !it.isClosed && (it.lastReconciledAt ?: 0L) < sevenDaysAgo }.forEach {
            attention.add(AttentionItem.ReconcileAccount(it))
        }

        // Missing payment accounts for upcoming payments (Check both occurrence and parent template)
        enrichedOccurrences
            .filter { it.accountId == null && it.parentAccountId == null }
            .distinctBy { it.recurringItemId }
            .forEach {
                attention.add(
                    AttentionItem.MissingAutoPayAccount(
                        itemId = it.recurringItemId ?: 0L,
                        itemName = it.recurringItemNameSnapshot ?: "Subscription"
                    )
                )
            }

        // 5. Future Upcoming Occurrences (Next one for each unique recurring item)
        val futureOccurrences = enrichedOccurrences
            .filter { it.scheduledDate >= startOfToday }
            .groupBy { it.recurringItemId }
            .map { it.value.minBy { occ -> occ.scheduledDate } }
            .sortedBy { it.scheduledDate }

        // 6. Safe to Spend Today
        val now = Calendar.getInstance()
        val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daysLeft = (daysInMonth - now.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        val upcomingRecurringActive = recurringItems.filter { it.status == "ACTIVE" }
        val upcomingBills = upcomingRecurringActive.flatMap { item ->
            item.occurrences.asSequence()
                .filter { occurrence ->
                    occurrence.status == "PENDING" && occurrence.scheduledDate in currentTime..endOfMonth
                }
                .map { item to it }
                .toList()
        }
        val recurringTotalForSafeToSpend = upcomingBills.sumOf { (item, occurrence) -> occurrence.amount ?: item.amount }

        val safeToSpend = SpendingForecastCalculator.calculateSafeToSpend(
            budgets = budgets,
            upcomingBills = recurringTotalForSafeToSpend,
            plannedSavings = 0.0,
            daysRemaining = daysLeft,
            upcomingBillsAlreadyInBudget = false,
            totalDaysInMonth = daysInMonth
        )

        // 7. Financial Health Score
        val nonTransferTxs = FinancialCalculations.filterNonTransferTransactions(transactions)
        val currentTrans = nonTransferTxs.filter { it.timestamp in startOfMonth..endOfMonth }
        val currentNet = FinancialCalculations.calculateNetCashFlow(currentTrans)
        val pendingRecurringTotal = recurringItems.filter { it.status == "PENDING" }.sumOf { it.amount }

        val health = FinancialHealthCalculator.calculateFinancialHealth(
            income = currentNet.income,
            expense = currentNet.spending,
            budgets = budgets,
            recurringTotal = pendingRecurringTotal,
            transactionCount = currentTrans.size,
            allTransactions = nonTransferTxs
        )

        DashboardState(
            isLoading = false,
            netWorth = currentNetWorth,
            netWorthTrend = trend,
            netWorthTrendLabel = trendLabel,
            netWorthHistory = netWorthHistory,
            monthlyIncome = income,
            monthlySpending = spending,
            monthlySavings = savings,
            accounts = accounts.filter { !it.isClosed },
            lastTransaction = transactions.maxByOrNull { it.timestamp },
            nextUpcomingOccurrence = futureOccurrences.firstOrNull(),
            attentionItems = attention.distinctBy { 
                when(it) {
                    is AttentionItem.OverduePayment -> "overdue_${it.occurrence.id}"
                    is AttentionItem.ReconcileAccount -> "reconcile_${it.account.id}"
                    is AttentionItem.MissingAutoPayAccount -> "missing_${it.itemName}"
                }
            },
            safeToSpendResult = safeToSpend,
            healthResult = health
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState(isLoading = true)
    )
}
