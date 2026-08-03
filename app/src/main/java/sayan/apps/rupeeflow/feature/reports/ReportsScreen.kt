package sayan.apps.rupeeflow.feature.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import java.util.*

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences = LocalUserPreferences.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ReportTypeSelector(
                selectedType = uiState.selectedReportType,
                onTypeSelected = { viewModel.onReportTypeSelected(it) }
            )
        }
        
        when (uiState.selectedReportType) {
            ReportType.OVERVIEW -> overviewReportItems(uiState, preferences)
            ReportType.SPENDING -> spendingReportItems(uiState, preferences)
            ReportType.MERCHANTS -> merchantReportItems(uiState, preferences)
            else -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("Coming Soon: ${uiState.selectedReportType.name}", color = Color.White)
                    }
                }
            }
        }
    }
}

fun LazyListScope.overviewReportItems(state: ReportsState, preferences: UserPreferences) {
    item {
        MonthlyStoryCard(state.monthlyStory)
    }

    item {
        ReportSummaryCard(state.summary, "July 2026", preferences)
    }
    
    item {
        IncomeExpenseChart(state.summary.income, state.summary.expense, state.summary.savings)
    }

    item {
        SpendingCalendarCard(state.dailySpending, state.selectedMonth)
    }

    item {
        FinancialHealthCard(state.financialScore, state.scoreReasons)
    }

    item {
        MonthlyComparisonCard(state.comparison, preferences)
    }

    item {
        ReportHighlightsCard(state.highlights, preferences)
    }

    item {
        TimelineCard(state.timeline, preferences)
    }
}

fun LazyListScope.spendingReportItems(state: ReportsState, preferences: UserPreferences) {
    item {
        RankedCategoryCard(state.categorySpending, preferences)
    }
}

fun LazyListScope.merchantReportItems(state: ReportsState, preferences: UserPreferences) {
    item {
        TopMerchantsCard(state.merchantSpending, preferences)
    }
}

@Composable
fun MonthlyStoryCard(story: List<String>) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("July 2026 Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            story.forEach { line ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("•", color = Color(0xFF10B981), modifier = Modifier.padding(end = 8.dp))
                    Text(line, style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD1D5DB))
                }
            }
        }
    }
}

@Composable
fun ReportSummaryCard(
    summary: ReportSummary,
    monthLabel: String,
    preferences: UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "$monthLabel Report",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9CA3AF)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // 2x2 Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItemGrid("Income", summary.income, Color(0xFF10B981), preferences, Modifier.weight(1f))
                SummaryItemGrid("Expense", summary.expense, Color(0xFFEF4444), preferences, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItemGrid("Savings", summary.savings, Color(0xFF3B82F6), preferences, Modifier.weight(1f))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(
                        text = (if (summary.netCashFlow >= 0) "+" else "") + CurrencyFormatter.format(summary.netCashFlow, preferences),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (summary.netCashFlow >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { /* Export PDF */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.VerticalAlignBottom, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
                OutlinedButton(
                    onClick = { /* Share */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.NorthEast, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Report")
                }
            }
        }
    }
}

@Composable
fun SummaryItemGrid(title: String, amount: Double, color: Color, preferences: UserPreferences, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        Text(
            CurrencyFormatter.format(amount, preferences),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}

@Composable
fun IncomeExpenseChart(income: Double, expense: Double, savings: Double) {
    val total = (income + expense + savings).coerceAtLeast(1.0)
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Income vs Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            ChartBarWithPercent("Income", income / total, (income / total * 100), Color(0xFF10B981))
            Spacer(modifier = Modifier.height(20.dp))
            ChartBarWithPercent("Expense", expense / total, (expense / total * 100), Color(0xFFEF4444))
            Spacer(modifier = Modifier.height(20.dp))
            ChartBarWithPercent("Savings", savings / total, (savings / total * 100), Color(0xFF3B82F6))
        }
    }
}

@Composable
fun ChartBarWithPercent(label: String, progress: Double, percent: Double, color: Color) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress.toFloat() },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = color,
            trackColor = Color(0xFF1F2937),
            strokeCap = StrokeCap.Round
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendingCalendarCard(dailySpending: List<DaySpending>, month: Calendar) {
    var selectedDay by remember { mutableStateOf<DaySpending?>(null) }
    
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Spending Calendar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            val daysInMonth = month.getActualMaximum(Calendar.DAY_OF_MONTH)
            val spendingMap = dailySpending.associateBy { it.day }
            val maxSpending = dailySpending.maxOfOrNull { it.amount } ?: 1.0
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                maxItemsInEachRow = 7
            ) {
                for (day in 1..daysInMonth) {
                    val dayData = spendingMap[day]
                    val amount = dayData?.amount ?: 0.0
                    
                    // Heatmap intensity
                    val alpha = when {
                        amount == 0.0 -> 0.05f
                        amount < maxSpending * 0.2 -> 0.2f
                        amount < maxSpending * 0.5 -> 0.5f
                        amount < maxSpending * 0.8 -> 0.8f
                        else -> 1.0f
                    }
                    val color = if (amount > 0) Color(0xFF10B981).copy(alpha = alpha) else Color(0xFF1F2937)
                    
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(36.dp)
                            .background(color, RoundedCornerShape(4.dp))
                            .clickable { selectedDay = dayData ?: DaySpending(day, 0.0) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$day",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (amount > 0 && alpha > 0.5f) Color.Black else Color.White
                        )
                    }
                }
            }

            selectedDay?.let { day ->
                if (day.amount > 0) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        color = Color(0xFF1E1E1E),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${day.day} July", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("₹${day.amount.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFEF4444))
                            }
                            day.topTransactions.forEach { tx ->
                                Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(tx.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFF10B981))
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
fun FinancialHealthCard(score: Int, reasons: List<String>) {
    val scoreColor = when {
        score >= 80 -> Color(0xFF10B981) // Green
        score >= 60 -> Color(0xFFFACC15) // Yellow
        score >= 40 -> Color(0xFFFB923C) // Orange
        else -> Color(0xFFEF4444) // Red
    }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 8.dp,
                        trackColor = Color(0xFF1F2937),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Column {
                    Text("Financial Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    val status = when {
                        score >= 80 -> "Excellent"
                        score >= 60 -> "Good"
                        score >= 40 -> "Average"
                        else -> "Needs Work"
                    }
                    Text(status, style = MaterialTheme.typography.titleSmall, color = scoreColor)
                }
            }
            
            if (reasons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                reasons.forEach { reason ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(scoreColor, CircleShape))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(reason, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonCard(comparison: MonthlyComparison, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("June vs July", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            ComparisonItemDetailed("Income", comparison.prevIncome, comparison.currentIncome, comparison.incomeChange, preferences)
            Spacer(modifier = Modifier.height(20.dp))
            ComparisonItemDetailed("Expense", comparison.prevExpense, comparison.currentExpense, comparison.expenseChange, preferences, isNegativeGood = true)
            Spacer(modifier = Modifier.height(20.dp))
            ComparisonItemDetailed("Savings", comparison.prevSavings, comparison.currentSavings, comparison.savingsChange, preferences)
        }
    }
}

@Composable
fun ComparisonItemDetailed(title: String, prev: Double, current: Double, percent: Double, preferences: UserPreferences, isNegativeGood: Boolean = false) {
    val isPositive = percent >= 0
    val isGood = if (isNegativeGood) !isPositive else isPositive
    val color = if (isGood) Color(0xFF10B981) else Color(0xFFEF4444)
    
    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(CurrencyFormatter.format(prev, preferences), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(14.dp).padding(horizontal = 4.dp))
                Text(CurrencyFormatter.format(current, preferences), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isPositive) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${Math.abs(percent).toInt()}%",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }
    }
}

@Composable
fun ReportHighlightsCard(highlights: ReportHighlights, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Monthly Highlights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HighlightMiniCard("Largest Expense", highlights.largestExpense?.title ?: "N/A", highlights.largestExpense?.let { CurrencyFormatter.format(it.amount, preferences) } ?: "₹0", Modifier.weight(1f), Color(0xFFEF4444))
                HighlightMiniCard("Highest Income", highlights.highestIncome?.title ?: "N/A", highlights.highestIncome?.let { CurrencyFormatter.format(it.amount, preferences) } ?: "₹0", Modifier.weight(1f), Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HighlightMiniCard("Top Category", highlights.mostUsedCategory, "Most Active", Modifier.weight(1f), Color(0xFFFACC15))
                HighlightMiniCard("Top Account", highlights.mostUsedAccount, "Most Used", Modifier.weight(1f), Color(0xFF3B82F6))
            }
        }
    }
}

@Composable
fun HighlightMiniCard(label: String, title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}

@Composable
fun TimelineCard(timeline: List<Transaction>, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Monthly Timeline", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            timeline.forEachIndexed { index, tx ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(8.dp).background(if (tx.isIncome) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape))
                        if (index < timeline.size - 1) {
                            Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFF1F2937)))
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(tx.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                    Text(
                        text = (if (tx.isIncome) "+" else "-") + CurrencyFormatter.format(tx.amount, preferences),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (tx.isIncome) Color(0xFF10B981) else Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RankedCategoryCard(spending: List<CategorySpending>, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Top Spending Categories", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            val total = spending.sumOf { it.amount }.coerceAtLeast(1.0)
            val maxAmount = spending.firstOrNull()?.amount ?: 1.0
            
            spending.take(10).forEachIndexed { index, item ->
                RankedCategoryItem(index + 1, item, maxAmount, (item.amount / total * 100), preferences)
                if (index < spending.size - 1) {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun RankedCategoryItem(rank: Int, item: CategorySpending, maxAmount: Double, percent: Double, preferences: UserPreferences) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("$rank. ${item.categoryName}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text("${String.format(Locale.getDefault(), "%.1f", percent)}%", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        val progress = (item.amount / maxAmount).toFloat()
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Color(android.graphics.Color.parseColor(item.colorHex)),
            trackColor = Color(0xFF1F2937),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun TopMerchantsCard(merchants: List<MerchantSpending>, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Top Merchants", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            
            merchants.take(10).forEachIndexed { index, item ->
                MerchantItem(item, preferences)
                if (index < merchants.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF1F2937).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MerchantItem(item: MerchantSpending, preferences: UserPreferences) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.merchantName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.transactionCount} Transactions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        }
        Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun ReportTypeSelector(
    selectedType: ReportType,
    onTypeSelected: (ReportType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReportType.entries.forEach { type ->
            val isSelected = selectedType == type
            Surface(
                onClick = { onTypeSelected(type) },
                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}
