package sayan.apps.rupeeflow.feature.recurring

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringDetailScreen(
    itemId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val items by viewModel.recurringItems.collectAsState()
    val item = items.find { it.id == itemId }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        if (item == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Item not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Category, // Should be category specific
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(item.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                    Text(
                        CurrencyFormatter.format(item.amount, preferences),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider()

                // Info Grid
                DetailRow("Category", item.category)
                DetailRow("Frequency", item.frequency)
                DetailRow("Payment Method", item.paymentMethod ?: "Not set")
                DetailRow("AutoPay", if (item.isAutoPay) "Enabled" else "Disabled")
                DetailRow("Reminder", "${item.reminderDaysBefore} Day Before")
                
                val dateFormat = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
                DetailRow("Next Payment", dateFormat.format(Date(item.dueDate)))
                item.lastPaidDate?.let { 
                    DetailRow("Last Paid", dateFormat.format(Date(it)))
                }
                
                if (!item.notes.isNullOrBlank()) {
                    Column {
                        Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.notes, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* Pause */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pause")
                    }
                    Button(
                        onClick = { 
                            viewModel.deleteRecurringItem(item)
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}
