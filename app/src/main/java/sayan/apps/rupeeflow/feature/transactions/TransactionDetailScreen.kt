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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
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
    val transaction = transactions.find { it.id.toLong() == transactionId }
    val attachments by viewModel.getAttachments(transactionId).collectAsState(initial = emptyList())

    val dateFormat = SimpleDateFormat("dd MMMM, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Scaffold(
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
                // Icon & Amount
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = if (transaction.isIncome) EmeraldGreen.copy(alpha = 0.1f) else VibrantRed.copy(alpha = 0.1f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.isIncome) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                            contentDescription = null,
                            tint = if (transaction.isIncome) EmeraldGreen else VibrantRed,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${if (transaction.isIncome) "+" else "-"} ${CurrencyFormatter.format(transaction.amount, preferences)}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.isIncome) EmeraldGreen else VibrantRed
                        )
                    )
                    Text(
                        text = transaction.title,
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
                        DetailItem(label = "Category", value = transaction.category, icon = Icons.Rounded.Category)
                        DetailItem(label = "Date", value = dateFormat.format(Date(transaction.timestamp)), icon = Icons.Rounded.Event)
                        DetailItem(label = "Time", value = timeFormat.format(Date(transaction.timestamp)), icon = Icons.Rounded.Schedule)
                        
                        if (transaction.upiMetadata != null) {
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
                    Text("Delete Transaction")
                }
            }
        }
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
