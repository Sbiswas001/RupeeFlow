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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RecurringItemCard(
    item: RecurringItem,
    onClick: () -> Unit,
    onPay: (RecurringOccurrence) -> Unit
) {
    val preferences = LocalUserPreferences.current
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    
    val nextOccurrence = item.occurrences
        .filter { it.status == "PENDING" }
        .minByOrNull { it.scheduledDate }

    val todayStart = getStartOfToday()
    val dueText: String
    val dueColor: Color
    val dueIcon: @Composable (() -> Unit)?

    if (nextOccurrence != null) {
        val scheduled = nextOccurrence.scheduledDate
        when {
            isToday(scheduled) -> {
                dueText = "Due today"
                dueColor = Color(0xFFFFB74D)
                dueIcon = {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            scheduled < todayStart -> {
                val days = ((todayStart - scheduled) / (1000 * 60 * 60 * 24)).toInt()
                dueText = if (days == 1) "Overdue by 1 day" else "Overdue by $days days"
                dueColor = Color(0xFFF43F5E)
                dueIcon = {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            else -> {
                dueText = "Next due: ${dateFormat.format(Date(scheduled))}"
                dueColor = Color(0xFF9CA3AF)
                dueIcon = null
            }
        }
    } else {
        dueText = "All paid"
        dueColor = Color(0xFF10B981)
        dueIcon = {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(12.dp)
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = Color(0xFF7C3AED).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = getIconForCategory(item.category),
                                contentDescription = null,
                                tint = Color(0xFF7C3AED),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color.White
                    )
                }
                
                Text(
                    text = CurrencyFormatter.format(item.amount, preferences),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(6.dp))

            // Second row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    dueIcon?.invoke()
                    Text(
                        text = dueText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = dueColor
                    )
                }
                
                if (item.frequency != "NONE") {
                    val freqText = if (item.frequency == "CUSTOM") {
                        "Every ${item.frequencyInterval} ${item.frequencyUnit.lowercase()}"
                    } else {
                        "Every ${item.frequency.lowercase()}"
                    }
                    Text(
                        text = freqText,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            
            if (item.category == "EMIs" && item.recurrenceCount != null && item.totalRecurrence != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { item.recurrenceCount.toFloat() / item.totalRecurrence.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    trackColor = Color(0xFF262626),
                    strokeCap = StrokeCap.Round,
                    color = Color(0xFF7C3AED)
                )
                Text(
                    text = "${item.recurrenceCount} / ${item.totalRecurrence} Paid",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    modifier = Modifier.padding(top = 2.dp),
                    color = Color(0xFF9CA3AF)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            // Bottom row
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
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AutoPay",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFFFFB74D)
                        )
                    } else if (item.reminderDaysBefore > 0) {
                        Icon(
                            Icons.Rounded.NotificationsActive,
                            contentDescription = "Reminder",
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reminder",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color(0xFFF43F5E)
                        )
                    }
                }
                
                if (nextOccurrence != null) {
                    Row(
                        modifier = Modifier
                            .clickable { onPay(nextOccurrence) }
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Record",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF7C3AED)
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else if (item.status == "ACTIVE") {
                    Surface(
                        color = Color(0xFF10B981).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ON SCHEDULE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
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

private fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun getStartOfToday(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
