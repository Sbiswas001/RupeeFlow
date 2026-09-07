package sayan.apps.rupeeflow.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit = {},
    isCompact: Boolean = false
) {
    val preferences = LocalUserPreferences.current
    val isAdjustment = transaction.type == TransactionType.BALANCE_ADJUSTMENT
    val isTransfer = transaction.type == TransactionType.TRANSFER
    
    val verticalPadding = if (isCompact) 8.dp else 12.dp
    val horizontalPadding = if (isCompact) 10.dp else 14.dp
    val iconSize = if (isCompact) 32.dp else 36.dp
    val titleFontSize = if (isCompact) 15.sp else 16.sp
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = when {
            isAdjustment -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            isTransfer -> Color(0xFF7C3AED).copy(alpha = 0.03f)
            else -> Color(0xFF181818)
        },
        shape = RoundedCornerShape(20.dp),
        border = when {
            isAdjustment -> androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            isTransfer -> androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED).copy(alpha = 0.1f))
            else -> null
        }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .background(
                        color = when {
                            isAdjustment -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                            isTransfer -> Color(0xFF7C3AED).copy(alpha = 0.1f)
                            transaction.categoryColor != null -> Color(android.graphics.Color.parseColor(transaction.categoryColor)).copy(alpha = 0.15f)
                            transaction.isIncome -> EmeraldGreen.copy(alpha = 0.2f)
                            else -> VibrantRed.copy(alpha = 0.15f)
                        },
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isAdjustment -> Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (isCompact) 16.dp else 18.dp))
                    isTransfer -> Icon(Icons.Rounded.SwapHoriz, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(if (isCompact) 16.dp else 18.dp))
                    transaction.categoryIcon != null -> Text(transaction.categoryIcon, fontSize = if (isCompact) 16.sp else 18.sp)
                    else -> Icon(
                        imageVector = if (transaction.isIncome) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = if (transaction.isIncome) EmeraldGreen else VibrantRed,
                        modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            isAdjustment -> "Account correction"
                            isTransfer -> "Transfer"
                            else -> transaction.title
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = titleFontSize
                        ),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isAdjustment) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ADJUSTMENT",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Text(
                    text = when {
                        isAdjustment -> "${transaction.accountNameSnapshot ?: "Account"} • ${formatTransactionDate(transaction.timestamp)}"
                        isTransfer -> "${if (transaction.isIncoming) "From" else "To"} ${transaction.transferAccountNameSnapshot ?: "Unknown"} • ${formatTransactionDate(transaction.timestamp)}"
                        else -> "${transaction.category.split("&")[0].trim()} • ${formatTransactionDate(transaction.timestamp)}"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color(0xFF9CA3AF)
                )
            }

            val amountString = remember(transaction.amount, preferences) {
                val formatted = CurrencyFormatter.format(kotlin.math.abs(transaction.amount), preferences)
                if (formatted.contains(".")) {
                    val parts = formatted.split(".")
                    buildAnnotatedString {
                        append(parts[0])
                        withStyle(SpanStyle(fontSize = if (isCompact) 11.sp else 12.sp, color = Color.White.copy(alpha = 0.5f))) {
                            append(".${parts[1]}")
                        }
                    }
                } else {
                    buildAnnotatedString { append(formatted) }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = buildAnnotatedString {
                        val color = when {
                            isAdjustment -> Color.White
                            isTransfer -> Color.White
                            transaction.isIncome -> EmeraldGreen
                            else -> Color.White
                        }
                        val sign = when {
                            isTransfer -> ""
                            transaction.isIncome -> "+"
                            isAdjustment -> if (transaction.amount >= 0) "+" else "-"
                            else -> "-"
                        }
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = titleFontSize, color = color)) {
                            if (sign.isNotEmpty()) {
                                append(sign)
                                append(" ")
                            }
                            append(amountString)
                        }
                    }
                )
                if (transaction.upiMetadata != null && !isTransfer && !isCompact) {
                    Text(
                        text = transaction.upiMetadata.app?.name?.replace("_", " ")?.uppercase() ?: "UPI",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }
    }
}

private fun formatTransactionDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val time = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    return when {
        isSameDay(now, time) -> "Today • ${timeFormat.format(Date(timestamp))}"
        isYesterday(now, time) -> "Yesterday • ${timeFormat.format(Date(timestamp))}"
        else -> "${dateFormat.format(Date(timestamp))} • ${timeFormat.format(Date(timestamp))}"
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, then: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply { 
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1) 
    }
    return isSameDay(yesterday, then)
}
