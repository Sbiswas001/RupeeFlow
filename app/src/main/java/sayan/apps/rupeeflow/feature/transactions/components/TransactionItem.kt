package sayan.apps.rupeeflow.feature.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit = {}
) {
    val preferences = LocalUserPreferences.current
    val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = transaction.categoryColor?.let { Color(android.graphics.Color.parseColor(it)).copy(alpha = 0.1f) }
                            ?: if (transaction.isIncome) EmeraldGreen.copy(alpha = 0.1f) else VibrantRed.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (transaction.categoryIcon != null) {
                    Text(transaction.categoryIcon, fontSize = 24.sp)
                } else {
                    Icon(
                        imageVector = if (transaction.isIncome) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                        contentDescription = null,
                        tint = if (transaction.isIncome) EmeraldGreen else VibrantRed,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = transaction.category + " • " + dateFormat.format(Date(transaction.timestamp)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (transaction.upiMetadata != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "UPI: ${transaction.upiMetadata.app?.name?.replace("_", " ") ?: "Digital"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "${if (transaction.isIncome) "+" else "-"} ${CurrencyFormatter.format(transaction.amount, preferences)}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isIncome) EmeraldGreen else VibrantRed
                )
            )
        }
    }
}
