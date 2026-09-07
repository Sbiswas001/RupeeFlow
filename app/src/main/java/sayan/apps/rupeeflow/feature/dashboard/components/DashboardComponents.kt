package sayan.apps.rupeeflow.feature.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sayan.apps.rupeeflow.core.designsystem.components.LineChart
import sayan.apps.rupeeflow.core.designsystem.components.LineChartPoint
import sayan.apps.rupeeflow.core.designsystem.theme.EmeraldGreen
import sayan.apps.rupeeflow.core.designsystem.theme.VibrantRed
import sayan.apps.rupeeflow.core.financial.FinancialHealthResult
import sayan.apps.rupeeflow.core.financial.SafeToSpendResult
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.feature.dashboard.AttentionItem
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun NetWorthCard(
    netWorth: Double,
    trend: Double,
    trendLabel: String,
    history: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    var isVisible by remember(preferences.hideBalances) { mutableStateOf(!preferences.hideBalances) }
    val currentMonthName = remember {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldGreen)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Net worth · $currentMonthName",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isVisible) CurrencyFormatter.format(netWorth, preferences, overrideHideBalances = true) else "••••••",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-1).sp
                        ),
                        color = Color.Black,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    IconButton(
                        onClick = { isVisible = !isVisible }
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = if (isVisible) "Hide net worth" else "Show net worth",
                            tint = Color.Black.copy(alpha = 0.7f)
                        )
                    }
                }

                // Sparkline Graph (Shown only when Net Worth is visible and history has >= 2 points)
                if (isVisible && history.size >= 2) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val sortedHistory = remember(history) { history.sortedBy { it.first } }
                    val points = sortedHistory.mapIndexed { index, pair -> 
                        LineChartPoint(index.toFloat(), pair.second)
                    }
                    LineChart(
                        points = points,
                        lineColor = Color.Black.copy(alpha = 0.35f),
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val isPositive = trend >= 0
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val formattedTrend = if (trend != 0.0) {
                        if (isVisible) CurrencyFormatter.format(abs(trend), preferences, overrideHideBalances = true) else "••••••"
                    } else ""
                    Text(
                        text = "${if (isPositive && trend != 0.0) "↗" else if (trend < 0.0) "↘" else ""} $formattedTrend $trendLabel",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        color = Color.Black.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun SafeToSpendDashboardCard(
    safeToSpendResult: SafeToSpendResult,
    onClick: () -> Unit,
    onSetBudgetClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    var showFormulaDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF062A1D),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, EmeraldGreen.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Shield Icon + Title + Help Dialog Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = EmeraldGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SAFE TO SPEND TODAY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = EmeraldGreen
                    )
                }

                if (safeToSpendResult.hasConfiguredBudget) {
                    IconButton(
                        onClick = { showFormulaDialog = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                            contentDescription = "How is this calculated?",
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!safeToSpendResult.hasConfiguredBudget) {
                Text(
                    text = "Set a monthly budget",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Set a monthly budget to calculate your safe spending limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSetBudgetClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Monthly Budget", fontWeight = FontWeight.Bold)
                }
            } else {
                // Dominant Hero Amount + "today" subtitle
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Text(
                        text = CurrencyFormatter.format(safeToSpendResult.safeToSpendToday, preferences),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1.5).sp,
                            fontSize = 38.sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "today",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = EmeraldGreen,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row: Budget Remaining & Days Left
                Surface(
                    color = Color.Black.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = CurrencyFormatter.format(safeToSpendResult.remainingBudget, preferences),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "budget remaining",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF9CA3AF)
                            )
                        }

                        Text(
                            text = "${safeToSpendResult.daysRemaining} days left",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actionable Pace Alert Badge (Softer non-error deviation styling)
                val isOverBudget = safeToSpendResult.statusMessage.contains("over budget") || safeToSpendResult.statusMessage.contains("above target") || safeToSpendResult.statusMessage.contains("exceeded")
                val alertBg = if (isOverBudget) Color(0xFFEF4444).copy(alpha = 0.08f) else EmeraldGreen.copy(alpha = 0.12f)
                val alertBorder = if (isOverBudget) Color(0xFFEF4444).copy(alpha = 0.15f) else EmeraldGreen.copy(alpha = 0.25f)
                val alertColor = if (isOverBudget) Color(0xFFF87171) else EmeraldGreen

                Surface(
                    color = alertBg,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.5.dp, alertBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isOverBudget) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = alertColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = safeToSpendResult.statusMessage,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = alertColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    if (showFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaDialog = false },
            title = { Text("Safe to Spend Calculation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Available To Spend = Total Budget - Actual Spending - Upcoming Bills - Savings Goals",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    Text(
                        text = "Safe To Spend Today = Available To Spend / Days Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Text(
                        text = safeToSpendResult.explanation,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldGreen
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormulaDialog = false }) {
                    Text("Got it", color = EmeraldGreen)
                }
            },
            containerColor = Color(0xFF181818),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun FinancialHealthDashboardCard(
    healthResult: FinancialHealthResult,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (!healthResult.isDataSufficient || healthResult.overallScore == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Financial Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("Not enough data", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                }
            } else {
                val score = healthResult.overallScore
                val scoreColor = when {
                    score >= 80 -> EmeraldGreen
                    score >= 60 -> Color(0xFFFACC15)
                    score >= 40 -> Color(0xFFFB923C)
                    else -> VibrantRed
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            CircularProgressIndicator(
                                progress = { score / 100f },
                                modifier = Modifier.fillMaxSize(),
                                color = scoreColor,
                                strokeWidth = 5.dp,
                                trackColor = Color(0xFF1F2937),
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = "$score",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text("Financial Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(healthResult.statusLabel, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = scoreColor)
                            if (healthResult.confidenceLabel.isNotBlank()) {
                                Text(healthResult.confidenceLabel, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = Color(0xFF9CA3AF))
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "View details",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                if (healthResult.subScores.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        healthResult.subScores.take(4).forEach { sub ->
                            val label = when (sub.name) {
                                "Budget Adherence", "Spending Control" -> "Spending"
                                "Recurring Commitments", "Commitment Load" -> "Bills"
                                "Savings Rate", "Savings" -> "Savings"
                                else -> sub.name
                            }
                            Surface(
                                color = Color(0xFF262626),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = Color(0xFF9CA3AF),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${sub.score}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (sub.isAvailable) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActivityRow(
    lastTransaction: Transaction?,
    nextUpcoming: RecurringOccurrence?,
    onTransactionClick: (Long) -> Unit,
    onUpcomingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LastTransactionCard(
            transaction = lastTransaction,
            onClick = { lastTransaction?.id?.toLongOrNull()?.let(onTransactionClick) },
            modifier = Modifier.weight(1f)
        )
        NextUpcomingCard(
            occurrence = nextUpcoming,
            onClick = onUpcomingClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LastTransactionCard(
    transaction: Transaction?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    Surface(
        modifier = modifier.clickable(enabled = transaction != null, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "LAST ACTIVITY",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (transaction != null) {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                val isIncome = transaction.isIncome || transaction.type == TransactionType.INCOME
                val amountText = (if (isIncome) "+" else "−") + CurrencyFormatter.format(transaction.amount, preferences)
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isIncome) EmeraldGreen else VibrantRed
                )
            } else {
                Text(
                    text = "No recent activity",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NextUpcomingCard(
    occurrence: RecurringOccurrence?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "NEXT UPCOMING",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp, fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (occurrence != null) {
                Text(
                    text = occurrence.recurringItemNameSnapshot ?: "Subscription",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatRelativeDate(occurrence.scheduledDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = CurrencyFormatter.format(occurrence.amount ?: 0.0, preferences),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            } else {
                Text(
                    text = "No upcoming bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CashFlowSummary(
    income: Double,
    spending: Double,
    savings: Double,
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "This Month",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Top Row: Income & Spending side-by-side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(EmeraldGreen.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Income",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedIncome = if (preferences.hideBalances) "••••••" else ("+" + CurrencyFormatter.format(income, preferences)).replace("₹", "₹ ").replace("₹  ", "₹ ")
                        Text(
                            text = formattedIncome,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                            color = Color.White
                        )
                    }

                    // Spending
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(VibrantRed.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = VibrantRed, modifier = Modifier.size(12.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Spending",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        val formattedSpending = if (preferences.hideBalances) "••••••" else ("−" + CurrencyFormatter.format(spending, preferences)).replace("₹", "₹ ").replace("₹  ", "₹ ")
                        Text(
                            text = formattedSpending,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Row: Net Cash Flow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Net Cash Flow",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val netText = if (preferences.hideBalances) "••••••" else ((if (savings > 0) "+" else "") + CurrencyFormatter.format(savings, preferences)).replace("₹", "₹ ").replace("₹  ", "₹ ")
                    Text(
                        text = netText,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (savings >= 0) EmeraldGreen else VibrantRed
                    )
                }
            }
        }
    }
}

@Composable
fun NeedsAttentionSection(
    items: List<AttentionItem>,
    onAttentionClick: (AttentionItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Needs Attention",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                AttentionCard(item, onClick = { onAttentionClick(item) })
            }
        }
    }
}

@Composable
private fun AttentionCard(
    item: AttentionItem,
    onClick: () -> Unit
) {
    val preferences = LocalUserPreferences.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = VibrantRed.copy(alpha = 0.1f),
        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantRed.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = VibrantRed, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (item) {
                        is AttentionItem.ReconcileAccount -> "${item.account.name} needs reconciliation"
                        is AttentionItem.OverduePayment -> "${item.occurrence.recurringItemNameSnapshot} is overdue"
                        is AttentionItem.MissingAutoPayAccount -> "Account missing for ${item.itemName}"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = when (item) {
                        is AttentionItem.ReconcileAccount -> "Last checked ${formatDate(item.account.lastReconciledAt ?: 0L)}"
                        is AttentionItem.OverduePayment -> "${CurrencyFormatter.format(item.occurrence.amount ?: 0.0, preferences)} · Due ${formatDate(item.occurrence.scheduledDate)}"
                        is AttentionItem.MissingAutoPayAccount -> "Upcoming payment needs an account"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibrantRed.copy(alpha = 0.2f),
                    contentColor = VibrantRed
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    "Fix",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

@Composable
fun AccountsSnapshot(
    accounts: List<Account>,
    onAccountClick: (Account) -> Unit = {},
    onViewAllClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) return
    
    val assetsCount = accounts.count { it.category != "LIABILITIES" }
    val liabilitiesCount = accounts.count { it.category == "LIABILITIES" }
    val displayAccounts = accounts.take(2)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$assetsCount assets · $liabilitiesCount liabilities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onViewAllClick)
                    .padding(vertical = 4.dp, horizontal = 6.dp)
            ) {
                Text(
                    text = "View all",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = "View all accounts",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            displayAccounts.forEach { account ->
                AccountSmallCard(
                    account = account,
                    onClick = { onAccountClick(account) },
                    modifier = Modifier.weight(1f)
                )
            }
            if (displayAccounts.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun AccountSmallCard(
    account: Account,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val preferences = LocalUserPreferences.current
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = CurrencyFormatter.format(account.balance, preferences),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (account.category == "LIABILITIES") VibrantRed else Color.White
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp == 0L) return "Never"
    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatRelativeDate(timestamp: Long): String {
    val now = Calendar.getInstance()
    val due = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    val nowStart = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    
    val dueStart = (due.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return when {
        nowStart.timeInMillis == dueStart.timeInMillis -> "Today"
        dueStart.timeInMillis == nowStart.timeInMillis + 86400000 -> "Tomorrow"
        dueStart.timeInMillis < nowStart.timeInMillis -> {
            val days = (nowStart.timeInMillis - dueStart.timeInMillis) / 86400000
            if (days <= 0) "Today" else "$days days overdue"
        }
        else -> {
            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
