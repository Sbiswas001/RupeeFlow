package sayan.apps.rupeeflow.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowWidthSizeClass
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.feature.dashboard.components.*

@Composable
fun DashboardScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToUpcoming: () -> Unit,
    onNavigateToEditRecurring: (Long) -> Unit,
    onNavigateToAccountDetails: (Long) -> Unit,
    onNavigateToInsightsTab: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AnimatedVisibility(
                visible = uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            AnimatedVisibility(
                visible = !uiState.isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                DashboardContent(
                    uiState = uiState,
                    isExpanded = isExpanded,
                    onTransactionClick = onNavigateToDetail,
                    onViewAllUpcoming = onNavigateToUpcoming,
                    onNavigateToInsightsTab = onNavigateToInsightsTab,
                    onAccountClick = onNavigateToAccountDetails,
                    onAttentionClick = { item ->
                        when (item) {
                            is AttentionItem.ReconcileAccount -> onNavigateToAccountDetails(item.account.id)
                            is AttentionItem.OverduePayment -> onNavigateToEditRecurring(item.occurrence.recurringItemId ?: 0L)
                            is AttentionItem.MissingAutoPayAccount -> onNavigateToEditRecurring(item.itemId)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    uiState: DashboardState,
    isExpanded: Boolean,
    onTransactionClick: (Long) -> Unit,
    onViewAllUpcoming: () -> Unit,
    onNavigateToInsightsTab: (Int) -> Unit,
    onAccountClick: (Long) -> Unit,
    onAttentionClick: (AttentionItem) -> Unit
) {
    if (isExpanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 160.dp)
            ) {
                item {
                    NetWorthCard(
                        netWorth = uiState.netWorth,
                        trend = uiState.netWorthTrend,
                        trendLabel = uiState.netWorthTrendLabel,
                        history = uiState.netWorthHistory
                    )
                }
                item {
                    SafeToSpendDashboardCard(
                        safeToSpendResult = uiState.safeToSpendResult,
                        onClick = { onNavigateToInsightsTab(1) }
                    )
                }
                item {
                    FinancialHealthDashboardCard(
                        healthResult = uiState.healthResult,
                        onClick = { onNavigateToInsightsTab(2) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1.2f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 0.dp, bottom = 160.dp)
            ) {
                item {
                    CashFlowSummary(
                        income = uiState.monthlyIncome,
                        spending = uiState.monthlySpending,
                        savings = uiState.monthlySavings
                    )
                }
                item {
                    QuickActivityRow(
                        lastTransaction = uiState.lastTransaction,
                        nextUpcoming = uiState.nextUpcomingOccurrence,
                        onTransactionClick = onTransactionClick,
                        onUpcomingClick = onViewAllUpcoming
                    )
                }
                if (uiState.attentionItems.isNotEmpty()) {
                    item {
                        NeedsAttentionSection(
                            items = uiState.attentionItems,
                            onAttentionClick = onAttentionClick
                        )
                    }
                }
                item {
                    AccountsSnapshot(
                        accounts = uiState.accounts,
                        onAccountClick = { account -> onAccountClick(account.id) },
                        onViewAllClick = { onAccountClick(0L) }
                    )
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 0.dp, bottom = 160.dp)
        ) {
            // 1. Net Worth Hero
            item {
                NetWorthCard(
                    netWorth = uiState.netWorth,
                    trend = uiState.netWorthTrend,
                    trendLabel = uiState.netWorthTrendLabel,
                    history = uiState.netWorthHistory
                )
            }

            // 2. Safe to Spend Today Hero Card (Clicking redirects to Planning)
            item {
                SafeToSpendDashboardCard(
                    safeToSpendResult = uiState.safeToSpendResult,
                    onClick = { onNavigateToInsightsTab(1) }
                )
            }

            // 3. Cash Flow Summary (This Month)
            item {
                CashFlowSummary(
                    income = uiState.monthlyIncome,
                    spending = uiState.monthlySpending,
                    savings = uiState.monthlySavings
                )
            }

            // 4. Side-by-side Last Activity & Next Upcoming Bill
            item {
                QuickActivityRow(
                    lastTransaction = uiState.lastTransaction,
                    nextUpcoming = uiState.nextUpcomingOccurrence,
                    onTransactionClick = onTransactionClick,
                    onUpcomingClick = onViewAllUpcoming
                )
            }

            // 5. Needs Attention (Dynamic)
            if (uiState.attentionItems.isNotEmpty()) {
                item {
                    NeedsAttentionSection(
                        items = uiState.attentionItems,
                        onAttentionClick = onAttentionClick
                    )
                }
            }

            // 6. Accounts Snapshot
            item {
                AccountsSnapshot(
                    accounts = uiState.accounts,
                    onAccountClick = { account -> onAccountClick(account.id) },
                    onViewAllClick = { onAccountClick(0L) }
                )
            }

            // 7. Financial Health Summary Card (Clicking redirects to Reports)
            item {
                FinancialHealthDashboardCard(
                    healthResult = uiState.healthResult,
                    onClick = { onNavigateToInsightsTab(2) }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
fun DashboardPhonePreview() {
    RupeeFlowTheme(amoledBlack = false, dynamicColor = false) {
        DashboardScreen(
            onNavigateToAdd = {},
            onNavigateToDetail = {},
            onNavigateToUpcoming = {},
            onNavigateToEditRecurring = {},
            onNavigateToAccountDetails = {}
        )
    }
}
