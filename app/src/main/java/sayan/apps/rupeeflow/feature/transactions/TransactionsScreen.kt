package sayan.apps.rupeeflow.feature.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.feature.transactions.components.TransactionItem

@Composable
fun TransactionsScreen(
    onNavigateToAdd: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()

    TransactionsListContent(
        transactions = transactions,
        onNavigateToAdd = onNavigateToAdd,
        onTransactionClick = { transaction ->
            onNavigateToDetail(transaction.id.toLong())
        },
        modifier = modifier
    )
}

@Composable
fun TransactionsListContent(
    transactions: List<Transaction>,
    onNavigateToAdd: () -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                modifier = Modifier.padding(24.dp)
            )

            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(transactions) { transaction ->
                        Surface(
                            onClick = { onTransactionClick(transaction) },
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            TransactionItem(transaction)
                        }
                    }
                }
            }
        }
    }
}
