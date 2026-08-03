package sayan.apps.rupeeflow.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.core.layout.WindowWidthSizeClass
import java.util.Locale
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.feature.transactions.components.TransactionItem

@Composable
fun DashboardScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val adaptiveInfo = currentWindowAdaptiveInfo()
    val isExpanded = adaptiveInfo.windowSizeClass.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Transaction")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isExpanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        BalanceCard(balance = uiState.totalBalance)
                    }
                    Column(modifier = Modifier.weight(1.2f)) {
                        RecentTransactionsHeader()
                        Spacer(modifier = Modifier.height(16.dp))
                        RecentTransactionsList(
                            transactions = uiState.recentTransactions,
                            onTransactionClick = onNavigateToDetail
                        )
                    }
                }
            } else {
                BalanceCard(balance = uiState.totalBalance)
                Spacer(modifier = Modifier.height(32.dp))
                RecentTransactionsHeader()
                Spacer(modifier = Modifier.height(16.dp))
                RecentTransactionsList(
                    transactions = uiState.recentTransactions,
                    onTransactionClick = onNavigateToDetail
                )
            }
        }
    }
}

@Composable
fun RecentTransactionsHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Recent Transactions",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun RecentTransactionsList(
    transactions: List<Transaction>,
    onTransactionClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(transactions) { transaction ->
            TransactionItem(
                transaction = transaction,
                onClick = { onTransactionClick(transaction.id.toLong()) }
            )
        }
    }
}

@Composable
fun BalanceCard(balance: Double) {
    val preferences = LocalUserPreferences.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmeraldGreen
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.labelLarge,
                color = Color.Black.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = CurrencyFormatter.format(balance, preferences),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                ),
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 412)
@Composable
fun DashboardPhonePreview() {
    RupeeFlowTheme {
        DashboardScreen(onNavigateToAdd = {}, onNavigateToDetail = {})
    }
}

@Preview(showBackground = true, widthDp = 900)
@Composable
fun DashboardTabletPreview() {
    RupeeFlowTheme {
        DashboardScreen(onNavigateToAdd = {}, onNavigateToDetail = {})
    }
}
