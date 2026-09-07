package sayan.apps.rupeeflow.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val transactions by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val transaction = transactions.values.flatten().find { it.id == transactionId.toString() }
    val attachments by viewModel.getAttachments(transactionId).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    var showEditTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (transaction?.type == TransactionType.TRANSFER) {
                            showEditTransferDialog = true
                        } else {
                            onNavigateToEdit(transactionId)
                        }
                    }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (transaction == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Transaction not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                val isAdjustment = transaction.type == TransactionType.BALANCE_ADJUSTMENT
                val isTransfer = transaction.type == TransactionType.TRANSFER

                // Icon & Amount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = when {
                                    isAdjustment -> Color(0xFF7C3AED).copy(alpha = 0.1f)
                                    isTransfer -> Color(0xFF7C3AED).copy(alpha = 0.05f)
                                    transaction.isIncome -> EmeraldGreen.copy(alpha = 0.1f)
                                    else -> VibrantRed.copy(alpha = 0.1f)
                                },
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                isAdjustment -> Icons.Rounded.AccountBalance
                                isTransfer -> Icons.Rounded.SwapHoriz
                                transaction.isIncome -> Icons.Rounded.ArrowUpward
                                else -> Icons.Rounded.ArrowDownward
                            },
                            contentDescription = null,
                            tint = when {
                                isAdjustment || isTransfer -> Color(0xFF7C3AED)
                                transaction.isIncome -> EmeraldGreen
                                else -> VibrantRed
                            },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isAdjustment) {
                            (if (transaction.amount >= 0) "+" else "") + CurrencyFormatter.format(transaction.amount, preferences, overrideHideBalances = true)
                        } else if (isTransfer) {
                            CurrencyFormatter.format(transaction.amount, preferences, overrideHideBalances = true)
                        } else {
                            "${if (transaction.isIncome) "+" else "-"} ${CurrencyFormatter.format(transaction.amount, preferences, overrideHideBalances = true)}"
                        },
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isTransfer -> Color.White
                                isAdjustment && transaction.amount >= 0 -> EmeraldGreen
                                isAdjustment && transaction.amount < 0 -> VibrantRed
                                transaction.isIncome -> EmeraldGreen
                                else -> VibrantRed
                            }
                        )
                    )
                    Text(
                        text = when {
                            isAdjustment -> "Balance Adjustment"
                            isTransfer -> "Transfer"
                            else -> transaction.title
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val accountName = accounts.find { it.id == transaction.accountId }?.name 
                            ?: transaction.accountNameSnapshot?.let { "$it (closed)" }
                            ?: "Unknown Account"
                        DetailItem(label = "Account", value = accountName, icon = Icons.Rounded.AccountBalanceWallet)

                        if (isTransfer) {
                            val otherAccountName = accounts.find { it.id == transaction.transferAccountId }?.name
                                ?: transaction.transferAccountNameSnapshot?.let { "$it (closed)" }
                                ?: "Unknown Account"
                            DetailItem(
                                label = if (transaction.isIncoming) "From Account" else "To Account",
                                value = otherAccountName,
                                icon = Icons.Rounded.SwapHoriz
                            )
                        }

                        if (isAdjustment) {
                            DetailItem(label = "Adjustment Type", value = "Account Correction", icon = Icons.Rounded.AccountBalance)
                            transaction.reconciliationReason?.let {
                                DetailItem(label = "Reason", value = it, icon = Icons.AutoMirrored.Rounded.Comment)
                            }
                            transaction.previousBalance?.let { 
                                DetailItem(label = "Previous Balance", value = CurrencyFormatter.format(it, preferences, overrideHideBalances = true), icon = Icons.Rounded.History)
                            }
                            transaction.actualBalance?.let { 
                                DetailItem(label = "Actual Balance", value = CurrencyFormatter.format(it, preferences, overrideHideBalances = true), icon = Icons.Rounded.AccountBalanceWallet)
                            }
                        } else if (!isTransfer) {
                            DetailItem(label = "Category", value = transaction.category, icon = Icons.Rounded.Category)
                        }
                        
                        DetailItem(label = "Date", value = dateFormat.format(Date(transaction.timestamp)), icon = Icons.Rounded.Event)
                        DetailItem(label = "Time", value = timeFormat.format(Date(transaction.timestamp)), icon = Icons.Rounded.Schedule)
                        
                        if (transaction.upiMetadata != null && !isAdjustment && !isTransfer) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            DetailItem(
                                label = "UPI App", 
                                value = transaction.upiMetadata.app?.name?.replace("_", " ") ?: "Unknown",
                                icon = Icons.Rounded.Smartphone
                            )
                            transaction.upiMetadata.transactionId?.let {
                                DetailItem(label = "Transaction ID", value = it, icon = Icons.Rounded.QrCode)
                            }
                        }
                    }
                }

                if (!transaction.note.isNullOrBlank()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = transaction.note,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                if (attachments.isNotEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Attachments",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            attachments.forEach { attachment ->
                                Card(
                                    modifier = Modifier.size(120.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Rounded.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { 
                        viewModel.deleteTransaction(transaction)
                        onNavigateBack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTransfer) "Delete Transfer" else "Delete Transaction")
                }
            }
        }
    }

    if (showEditTransferDialog && transaction != null) {
        val fromId = if (transaction.isIncoming) transaction.transferAccountId ?: 0L else transaction.accountId ?: 0L
        val toId = if (transaction.isIncoming) transaction.accountId ?: 0L else transaction.transferAccountId ?: 0L
        sayan.apps.rupeeflow.feature.accounts.TransferDialog(
            accounts = accounts,
            title = "Edit Transfer",
            initialFromAccountId = fromId,
            initialToAccountId = toId,
            initialAmount = transaction.amount.toString(),
            initialNote = transaction.note ?: "",
            initialTimestamp = transaction.timestamp,
            onDismiss = { showEditTransferDialog = false },
            onTransfer = { from, to, amount, note, timestamp ->
                viewModel.updateTransfer(
                    transferId = transaction.transferId ?: "",
                    fromAccountId = from,
                    toAccountId = to,
                    amount = amount,
                    note = note.ifBlank { null },
                    timestamp = timestamp
                )
                showEditTransferDialog = false
            }
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium))
        }
    }
}
