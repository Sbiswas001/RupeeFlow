package sayan.apps.rupeeflow.feature.recurring

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Undo
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
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDetailScreen(
    itemId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val items by viewModel.recurringItems.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val item = items.find { it.id == itemId }
    val allOccurrences by viewModel.getOccurrences(itemId).collectAsState(initial = emptyList())
    
    val occurrences = remember(allOccurrences) {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = now.timeInMillis
        
        val history = allOccurrences.filter { it.status != "PENDING" }
        val pending = allOccurrences.filter { it.status == "PENDING" }.sortedBy { it.scheduledDate }
        
        val overdue = pending.filter { it.scheduledDate < startOfToday }
        val nextFuture = pending.filter { it.scheduledDate >= startOfToday }.take(1)
        
        (history + overdue + nextFuture).sortedBy { it.scheduledDate }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { item?.let { onNavigateToEdit(it.id) } }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { innerPadding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Item not found", color = Color.White)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            color = Color(0xFF7C3AED).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF7C3AED)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(item.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                        Text(
                            CurrencyFormatter.format(item.amount, preferences, overrideHideBalances = true),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }

                item { HorizontalDivider(color = Color(0xFF1F2937)) }

                // Info Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DetailRow("Category", item.category)
                        val frequencyLabel = if (item.frequency == "CUSTOM") {
                            "Every ${item.frequencyInterval} ${item.frequencyUnit.lowercase().replaceFirstChar { it.uppercase() }}"
                        } else {
                            item.frequency.lowercase().replaceFirstChar { it.uppercase() }
                        }
                        DetailRow("Frequency", frequencyLabel)
                        
                        val account = accounts.find { it.id == item.accountId }
                        DetailRow("Default Account", account?.name ?: item.accountNameSnapshot ?: "Not set")
                        DetailRow("AutoPay", if (item.isAutoPay) "Enabled" else "Disabled")
                    }
                }

                // Occurrence History
                item {
                    Text(
                        "Payment History",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }

                if (occurrences.isEmpty()) {
                    item {
                        Text("No occurrences generated yet", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    items(occurrences.reversed(), key = { it.id }) { occurrence ->
                        OccurrenceItem(
                            occurrence = occurrence,
                            accountName = accounts.find { it.id == occurrence.accountId }?.name,
                            onUndo = { viewModel.undoPayment(occurrence.id) },
                            onSkip = { viewModel.skipOccurrence(occurrence.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isPaused = item.status == "PAUSED"
                        OutlinedButton(
                            onClick = { viewModel.toggleRecurringItemStatus(item.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF1F2937))
                        ) {
                            Text(if (isPaused) "Resume" else "Pause", color = Color.White)
                        }
                        Button(
                            onClick = { 
                                viewModel.deleteRecurringItem(item)
                                onNavigateBack()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OccurrenceItem(
    occurrence: RecurringOccurrence,
    accountName: String?,
    onUndo: () -> Unit,
    onSkip: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val isPaid = occurrence.status == "PAID"
    val isSkipped = occurrence.status == "SKIPPED"
    val isPending = occurrence.status == "PENDING"
    
    Surface(
        color = Color(0xFF1F2937).copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateFormat.format(Date(occurrence.scheduledDate)),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = when {
                        isPaid -> "Paid on ${dateFormat.format(Date(occurrence.paymentDate ?: 0))} via ${accountName ?: occurrence.accountNameSnapshot?.let { "$it (deleted)" } ?: "Unknown"}"
                        isSkipped -> "Skipped"
                        else -> "Upcoming"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (isPaid) {
                IconButton(onClick = onUndo) {
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = "Undo", tint = Color.Gray)
                }
            } else if (isPending) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = Color(0xFF9CA3AF))
                }
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = when {
                            isPaid -> Color(0xFF10B981)
                            isSkipped -> Color.Gray
                            else -> Color(0xFFF59E0B)
                        },
                        shape = CircleShape
                    )
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold, color = Color.White))
    }
}
