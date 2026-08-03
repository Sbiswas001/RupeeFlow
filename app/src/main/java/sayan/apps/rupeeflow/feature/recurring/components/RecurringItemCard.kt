package sayan.apps.rupeeflow.feature.recurring.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecurringItemCard(
    item: RecurringItem,
    onClick: () -> Unit,
    onPay: () -> Unit
) {
    val preferences = LocalUserPreferences.current
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Placeholder for Category Icon
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getIconForCategory(item.category),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (item.status == "PAID") "Last Paid: ${dateFormat.format(Date(item.lastPaidDate ?: item.dueDate))}" 
                                   else "Due: ${dateFormat.format(Date(item.dueDate))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = CurrencyFormatter.format(item.amount, preferences),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.frequency != "NONE") {
                        Text(
                            text = "/ ${item.frequency.lowercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            if (item.category == "EMIs" && item.recurrenceCount != null && item.totalRecurrence != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { item.recurrenceCount.toFloat() / item.totalRecurrence.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Text(
                    text = "${item.recurrenceCount} / ${item.totalRecurrence} Paid",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            if (item.category == "Credit Cards" && item.outstandingAmount != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Outstanding: ${CurrencyFormatter.format(item.outstandingAmount, preferences)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (item.isAutoPay) {
                        Icon(
                            Icons.Rounded.FlashOn,
                            contentDescription = "AutoPay",
                            tint = Color(0xFFFFB74D),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AutoPay",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFFB74D)
                        )
                    } else if (item.reminderDaysBefore > 0) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = "Reminder",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reminder On",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
                
                if (item.status == "PENDING") {
                    Button(
                        onClick = onPay,
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                        modifier = Modifier.height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Pay Now", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PAID",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

fun getIconForCategory(category: String) = when (category) {
    "Bills" -> Icons.Rounded.Receipt
    "Subscriptions" -> Icons.Rounded.Subscriptions
    "EMIs" -> Icons.Rounded.CreditCard
    "Credit Cards" -> Icons.Rounded.Payment
    "Insurance" -> Icons.Rounded.Security
    "SIP" -> Icons.Rounded.TrendingUp
    else -> Icons.Rounded.Category
}
