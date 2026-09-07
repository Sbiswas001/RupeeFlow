package sayan.apps.rupeeflow.feature.reports

import android.content.Intent
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.BudgetProgressBar
import sayan.apps.rupeeflow.core.designsystem.components.MonthPickerDialog
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.FinancialHealthResult
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportsScreen(
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences = LocalUserPreferences.current
    var showMonthPicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ReportsMonthNavigationHeader(
                    formattedMonth = uiState.formattedMonthLabel,
                    onPrevMonth = { viewModel.onPreviousMonth() },
                    onNextMonth = { viewModel.onNextMonth() },
                    onMonthClick = { showMonthPicker = true }
                )
            }

            item {
                ReportTypeSelector(
                    selectedType = uiState.selectedReportType,
                    onTypeSelected = { viewModel.onReportTypeSelected(it) }
                )
            }

            when (uiState.selectedReportType) {
                ReportType.OVERVIEW -> overviewReportItems(uiState, preferences)
                ReportType.SPENDING -> spendingReportItems(uiState, preferences)
                ReportType.INCOME -> incomeReportItems(uiState, preferences)
                ReportType.BUDGET -> budgetReportItems(uiState, preferences)
                ReportType.ACCOUNTS -> accountsReportItems(uiState, preferences)
                ReportType.CATEGORIES -> entitiesReportItems(uiState, preferences)
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initialMonth = uiState.selectedMonth,
            onDismiss = { showMonthPicker = false },
            onConfirm = {
                viewModel.onMonthSelected(it)
                showMonthPicker = false
            }
        )
    }
}

@Composable
fun ReportsMonthNavigationHeader(
    formattedMonth: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevMonth,
            modifier = Modifier.background(Color(0xFF181818), CircleShape).size(36.dp)
        ) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Month", tint = Color.White)
        }

        Surface(
            color = Color(0xFF181818),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clickable { onMonthClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formattedMonth.ifEmpty { "Select Month" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
            }
        }

        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.background(Color(0xFF181818), CircleShape).size(36.dp)
        ) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Month", tint = Color.White)
        }
    }
}

fun LazyListScope.overviewReportItems(state: ReportsState, preferences: UserPreferences) {
    if (state.monthlyStory.isNotEmpty()) {
        item {
            MonthlyStoryCard(state.monthlyStory)
        }
    }

    item {
        ReportSummaryCard(state.summary, state.formattedMonthLabel, state.exportSummaryText, preferences)
    }

    item {
        IncomeExpenseChart(state.summary.income, state.summary.expense, preferences)
    }

    item {
        FinancialHealthCard(state.healthResult)
    }

    item {
        MonthlyComparisonCard(state.comparison, preferences)
    }

    item {
        SpendingCalendarCard(state.dailySpending, state.selectedMonth)
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
        RankedCategoryCard(state.categorySpending, preferences, title = "Top Spending Categories")
    }
}

fun LazyListScope.incomeReportItems(state: ReportsState, preferences: UserPreferences) {
    item {
        RankedCategoryCard(state.incomeSpending, preferences, title = "Top Income Sources")
    }
}

fun LazyListScope.budgetReportItems(state: ReportsState, preferences: UserPreferences) {
    items(state.budgets) { budget ->
        BudgetAdherenceCard(budget, preferences)
    }
}

fun LazyListScope.accountsReportItems(state: ReportsState, preferences: UserPreferences) {
    val totalNetWorth = state.accounts.sumOf { it.balance }.coerceAtLeast(1.0)
    items(state.accounts) { account ->
        AccountDistributionCard(account, totalNetWorth, preferences)
    }
}

fun LazyListScope.entitiesReportItems(state: ReportsState, preferences: UserPreferences) {
    item {
        TopSpendingEntitiesCard(state.spendingBreakdown, preferences)
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
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Monthly Review", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))
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
    exportText: String,
    preferences: UserPreferences
) {
    val context = LocalContext.current
    val isNetPositive = summary.netCashFlow >= 0

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "$monthLabel Snapshot",
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF9CA3AF)
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItemGrid("Income", summary.income, RupeeFlowTheme.colors.income, preferences, Modifier.weight(1f))
                SummaryItemGrid("Expenses", summary.expense, RupeeFlowTheme.colors.expense, preferences, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Net Cash Flow", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(
                        text = (if (isNetPositive) "+" else "") + CurrencyFormatter.format(summary.netCashFlow, preferences),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isNetPositive) RupeeFlowTheme.colors.income else RupeeFlowTheme.colors.expense
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Transactions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(
                        text = "${summary.transactionCount}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "RupeeFlow Report - $monthLabel")
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Export Report"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Report")
                }

                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, exportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Summary"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF1F2937)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
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
fun IncomeExpenseChart(income: Double, expense: Double, preferences: UserPreferences) {
    val totalSum = income + expense
    val incomePercent = if (totalSum > 0) (income / totalSum * 100) else 0.0
    val expensePercent = if (totalSum > 0) (expense / totalSum * 100) else 0.0
    val incomeRatio = if (totalSum > 0) (income / totalSum).toFloat() else 0f
    val expenseRatio = if (totalSum > 0) (expense / totalSum).toFloat() else 0f

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Income vs Expense Split",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(20.dp))

            IncomeExpenseLabelRow(
                label = "Income",
                formattedAmount = CurrencyFormatter.format(income, preferences),
                percent = incomePercent,
                color = RupeeFlowTheme.colors.income
            )
            Spacer(modifier = Modifier.height(12.dp))

            IncomeExpenseLabelRow(
                label = "Expenses",
                formattedAmount = CurrencyFormatter.format(expense, preferences),
                percent = expensePercent,
                color = RupeeFlowTheme.colors.expense
            )
            Spacer(modifier = Modifier.height(16.dp))

            DualColorSplitBar(
                incomeRatio = incomeRatio,
                expenseRatio = expenseRatio,
                incomeColor = RupeeFlowTheme.colors.income,
                expenseColor = RupeeFlowTheme.colors.expense
            )
        }
    }
}

@Composable
private fun IncomeExpenseLabelRow(
    label: String,
    formattedAmount: String,
    percent: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$label ($formattedAmount)",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF)
            )
        }
        Text(
            text = "${String.format(Locale.US, "%.1f", percent)}%",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

@Composable
private fun DualColorSplitBar(
    incomeRatio: Float,
    expenseRatio: Float,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(CircleShape)
            .background(Color(0xFF1F2937))
    ) {
        if (incomeRatio > 0f || expenseRatio > 0f) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (incomeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(incomeRatio)
                            .background(incomeColor)
                    )
                }
                if (incomeRatio > 0f && expenseRatio > 0f) {
                    Spacer(modifier = Modifier.width(2.dp))
                }
                if (expenseRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(expenseRatio)
                            .background(expenseColor)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun IncomeExpenseChartPreview() {
    RupeeFlowTheme {
        IncomeExpenseChart(
            income = 20085.60,
            expense = 15727.78,
            preferences = UserPreferences()
        )
    }
}

@Composable
fun FinancialHealthCard(healthResult: FinancialHealthResult) {
    if (!healthResult.isDataSufficient || healthResult.overallScore == null) {
        Surface(
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
            color = Color(0xFF181818),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Financial Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Not enough data yet", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
            }
        }
        return
    }

    val score = healthResult.overallScore
    val scoreColor = when {
        score >= 80 -> RupeeFlowTheme.colors.income
        score >= 60 -> Color(0xFFFACC15)
        score >= 40 -> Color(0xFFFB923C)
        else -> RupeeFlowTheme.colors.expense
    }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                    CircularProgressIndicator(
                        progress = { score / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = scoreColor,
                        strokeWidth = 7.dp,
                        trackColor = Color(0xFF1F2937),
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Financial Health", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(healthResult.statusLabel, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = scoreColor)
                    if (healthResult.confidenceLabel.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(healthResult.confidenceLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                    if (healthResult.keyInsight.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(healthResult.keyInsight, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
                    }
                }
            }

            if (healthResult.subScores.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                healthResult.subScores.forEach { sub ->
                    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(if (sub.isAvailable) scoreColor else Color.Gray, CircleShape))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("${sub.name}: ${sub.explanation}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
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
            Text("What Changed? (MoM)", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            ComparisonItemDetailed("Income", comparison.prevIncome, comparison.currentIncome, comparison.incomeChange, preferences)
            Spacer(modifier = Modifier.height(16.dp))
            ComparisonItemDetailed("Expense", comparison.prevExpense, comparison.currentExpense, comparison.expenseChange, preferences, isNegativeGood = true)
            Spacer(modifier = Modifier.height(16.dp))
            ComparisonItemDetailed("Net Cash Flow", comparison.prevNetCashFlow, comparison.currentNetCashFlow, comparison.netCashFlowChange, preferences)
        }
    }
}

@Composable
fun ComparisonItemDetailed(title: String, prev: Double, current: Double, percent: Double?, preferences: UserPreferences, isNegativeGood: Boolean = false) {
    val changeText = FinancialCalculations.formatPercentChangeText(percent)
    val isUp = (percent ?: 0.0) > 0
    val isGood = if (isNegativeGood) !isUp else isUp
    val badgeColor = if (changeText == "New") Color(0xFF9CA3AF) else if (isGood) RupeeFlowTheme.colors.income else RupeeFlowTheme.colors.expense

    Column {
        Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(CurrencyFormatter.format(prev, preferences), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color(0xFF4B5563), modifier = Modifier.size(14.dp).padding(horizontal = 4.dp))
                Text(CurrencyFormatter.format(current, preferences), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            }
            Surface(
                color = badgeColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = changeText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = badgeColor
                )
            }
        }
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
            Spacer(modifier = Modifier.height(20.dp))

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

                    val alpha = when {
                        amount == 0.0 -> 0.05f
                        amount < maxSpending * 0.2 -> 0.2f
                        amount < maxSpending * 0.5 -> 0.5f
                        amount < maxSpending * 0.8 -> 0.8f
                        else -> 1.0f
                    }
                    val color = if (amount > 0) RupeeFlowTheme.colors.income.copy(alpha = alpha) else Color(0xFF1F2937)

                    Box(
                        modifier = Modifier
                            .padding(3.dp)
                            .size(34.dp)
                            .background(color, RoundedCornerShape(6.dp))
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
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        color = Color(0xFF262626),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${day.day} ${SimpleDateFormat("MMMM", Locale.getDefault()).format(month.time)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                Text("₹${day.amount.toInt()}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = RupeeFlowTheme.colors.expense)
                            }
                            day.topTransactions.forEach { tx ->
                                Row(modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(tx.title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    Text(tx.category, style = MaterialTheme.typography.labelSmall, color = RupeeFlowTheme.colors.income)
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
fun ReportHighlightsCard(highlights: ReportHighlights, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Monthly Highlights", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HighlightMiniCard("Largest Expense", highlights.largestExpense?.title ?: "N/A", highlights.largestExpense?.let { CurrencyFormatter.format(it.amount, preferences) } ?: "₹0", Modifier.weight(1f), RupeeFlowTheme.colors.expense)
                HighlightMiniCard("Highest Income", highlights.highestIncome?.title ?: "N/A", highlights.highestIncome?.let { CurrencyFormatter.format(it.amount, preferences) } ?: "₹0", Modifier.weight(1f), RupeeFlowTheme.colors.income)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HighlightMiniCard("Top Category", highlights.mostUsedCategory, "Most Active", Modifier.weight(1f), Color(0xFFFACC15))
                HighlightMiniCard("Top Account", highlights.mostUsedAccount, "Most Used", Modifier.weight(1f), Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
fun HighlightMiniCard(label: String, title: String, value: String, modifier: Modifier = Modifier, color: Color) {
    Surface(
        modifier = modifier,
        color = Color(0xFF262626),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            Spacer(modifier = Modifier.height(20.dp))

            if (timeline.isEmpty()) {
                Text("No transactions this month", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
            } else {
                timeline.take(10).forEachIndexed { index, tx ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(8.dp).background(if (tx.isIncome) RupeeFlowTheme.colors.income else RupeeFlowTheme.colors.expense, CircleShape))
                            if (index < timeline.size - 1 && index < 9) {
                                Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFF1F2937)))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tx.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        }
                        Text(
                            text = (if (tx.isIncome) "+" else "-") + CurrencyFormatter.format(tx.amount, preferences),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (tx.isIncome) RupeeFlowTheme.colors.income else Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankedCategoryCard(
    spending: List<CategorySpending>,
    preferences: UserPreferences,
    title: String = "Top Spending Categories"
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            val total = spending.sumOf { it.amount }.coerceAtLeast(1.0)
            val maxAmount = spending.firstOrNull()?.amount ?: 1.0

            spending.take(10).forEachIndexed { index, item ->
                RankedCategoryItem(index + 1, item, maxAmount, (item.amount / total * 100), preferences)
                if (index < spending.size - 1) {
                    Spacer(modifier = Modifier.height(16.dp))
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
fun TopSpendingEntitiesCard(breakdown: List<SpendingEntityBreakdown>, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Top Spending Entities", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            breakdown.take(10).forEachIndexed { index, item ->
                SpendingEntityItem(item, preferences)
                if (index < breakdown.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF1F2937).copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun SpendingEntityItem(item: SpendingEntityBreakdown, preferences: UserPreferences) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${item.transactionCount} Transactions", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        }
        Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun BudgetAdherenceCard(budget: BudgetWithProgress, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(budget.budget.categoryIcon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(budget.budget.categoryName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                Text("${(budget.progress * 100).toInt()}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = if (budget.progress > 1f) RupeeFlowTheme.colors.expense else RupeeFlowTheme.colors.income)
            }
            Spacer(modifier = Modifier.height(16.dp))
            BudgetProgressBar(
                progress = budget.progress,
                modifier = Modifier.fillMaxWidth().height(10.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${CurrencyFormatter.format(budget.budget.spentAmount, preferences)} of ${CurrencyFormatter.format(budget.budget.limitAmount, preferences)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
                if (budget.remaining < 0) {
                    Text("Over by ${CurrencyFormatter.format(Math.abs(budget.remaining), preferences)}", style = MaterialTheme.typography.labelSmall, color = RupeeFlowTheme.colors.expense)
                }
            }
        }
    }
}

@Composable
fun AccountDistributionCard(account: Account, totalNetWorth: Double, preferences: UserPreferences) {
    val weight = (account.balance / totalNetWorth).toFloat()
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(Color(android.graphics.Color.parseColor(account.colorHex ?: "#7C3AED")).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = Color(android.graphics.Color.parseColor(account.colorHex ?: "#7C3AED")), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text(account.subType.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { weight.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = Color(android.graphics.Color.parseColor(account.colorHex ?: "#7C3AED")),
                    trackColor = Color(0xFF1F2937),
                    strokeCap = StrokeCap.Round
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(CurrencyFormatter.format(account.balance, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("${(weight * 100).toInt()}% weight", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            }
        }
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
                color = if (isSelected) RupeeFlowTheme.colors.income else Color.Transparent,
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
