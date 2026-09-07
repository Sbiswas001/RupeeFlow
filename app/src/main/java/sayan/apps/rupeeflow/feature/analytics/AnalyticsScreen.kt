package sayan.apps.rupeeflow.feature.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.*
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.financial.CategoryTrend
import sayan.apps.rupeeflow.core.financial.FinancialCalculations
import sayan.apps.rupeeflow.core.financial.FormattedCategoryShare
import sayan.apps.rupeeflow.core.financial.NetCashFlow
import sayan.apps.rupeeflow.core.financial.SpendingAnomaly
import sayan.apps.rupeeflow.core.financial.TrendDirection
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.UserPreferences
import java.text.SimpleDateFormat
import java.util.*

val categoryPalette = listOf(
    Color(0xFF3B82F6), // Blue
    Color(0xFF10B981), // Emerald
    Color(0xFF8B5CF6), // Purple
    Color(0xFFF59E0B), // Amber
    Color(0xFFEC4899), // Pink
    Color(0xFF06B6D4), // Cyan
    Color(0xFF6366F1), // Indigo
    Color(0xFF14B8A6), // Teal
    Color(0xFFF97316)  // Orange
)

fun parseCategoryColor(colorHex: String, index: Int): Color {
    return try {
        if (colorHex.isNotBlank() && colorHex != "#000000") {
            Color(android.graphics.Color.parseColor(colorHex))
        } else {
            categoryPalette[index % categoryPalette.size]
        }
    } catch (e: Exception) {
        categoryPalette[index % categoryPalette.size]
    }
}

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onNavigateToViewAll: () -> Unit = {},
    onNavigateToCategoryDetail: (Long) -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filters Row & Explicit Date Range Label
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                AnalyticsFilters(
                    selectedFilter = uiState.selectedFilter,
                    selectedMonth = uiState.selectedMonth,
                    customRange = uiState.customRange,
                    onFilterSelect = { viewModel.onFilterSelected(it) },
                    onClearMonth = { viewModel.onClearMonth() }
                )

                if (uiState.formattedDateRange.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.formattedDateRange,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color(0xFF10B981),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }

        // Hero Net Cash Flow Summary Card
        item {
            NetCashFlowHeroCard(
                netCashFlow = uiState.netCashFlow,
                preferences = preferences
            )
        }

        // 2x2 KPI Grid
        item {
            val periodLabel = "vs previous period"

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryCard(
                        title = "Total Income",
                        amount = uiState.totalIncome,
                        trendPercent = uiState.incomeTrendPercent,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        iconColor = RupeeFlowTheme.colors.income,
                        periodLabel = periodLabel,
                        modifier = Modifier.weight(1f),
                        preferences = preferences
                    )
                    SummaryCard(
                        title = "Total Spent",
                        amount = uiState.totalSpent,
                        trendPercent = uiState.spentTrendPercent,
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        iconColor = RupeeFlowTheme.colors.expense,
                        isInverseTrend = true,
                        periodLabel = periodLabel,
                        modifier = Modifier.weight(1f),
                        preferences = preferences
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SummaryCard(
                        title = "Avg Daily Spend",
                        amount = uiState.averageAmount,
                        trendPercent = null,
                        icon = Icons.Rounded.Schedule,
                        iconColor = Color(0xFF3B82F6),
                        periodLabel = "",
                        modifier = Modifier.weight(1f),
                        preferences = preferences
                    )
                    SummaryCard(
                        title = "Transactions",
                        amount = uiState.transactionCount.toDouble(),
                        trendPercent = uiState.transTrendPercent,
                        icon = Icons.Rounded.Receipt,
                        iconColor = Color(0xFF8B5CF6),
                        isCurrency = false,
                        periodLabel = periodLabel,
                        modifier = Modifier.weight(1f),
                        preferences = preferences
                    )
                }
            }
        }

        // Key Insights Section
        if (uiState.insights.isNotEmpty()) {
            item {
                KeyInsightsSection(uiState.insights, preferences)
            }
        }

        // Spending Trend Chart with Tooltip payload
        item {
            InteractiveSpendingTrendSection(
                interactivePoints = uiState.interactiveTrendPoints,
                averageLabel = uiState.averageLabel,
                averageAmount = uiState.averageAmount,
                preferences = preferences
            )
        }

        // Category Breakdown with Rank, Exact Amount, <1% Logic & Other
        item {
            RankedCategoryBreakdownCard(
                categoryShares = uiState.formattedCategoryShares,
                totalSpent = uiState.totalSpent,
                onViewAll = onNavigateToViewAll,
                onCategoryClick = onNavigateToCategoryDetail,
                preferences = preferences
            )
        }

        // Category Trend ("What's changing?")
        if (uiState.categoryTrends.isNotEmpty()) {
            item {
                CategoryTrendSection(
                    trends = uiState.categoryTrends,
                    onCategoryClick = onNavigateToCategoryDetail,
                    preferences = preferences
                )
            }
        }

        // Merchant Analysis & Spending Anomalies
        item {
            MerchantAndAnomaliesSection(
                highestExpense = uiState.highestExpense,
                topMerchant = uiState.topMerchant,
                anomalies = uiState.anomalies,
                preferences = preferences
            )
        }
    }
}

@Composable
fun NetCashFlowHeroCard(
    netCashFlow: NetCashFlow,
    preferences: UserPreferences
) {
    val isPositive = netCashFlow.netCashFlow >= 0.0
    val heroColor = if (isPositive) RupeeFlowTheme.colors.income else RupeeFlowTheme.colors.expense

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, heroColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositive) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = heroColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Net Cash Flow",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }

                Surface(
                    color = heroColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isPositive) "Positive" else "Deficit",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = heroColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = (if (isPositive) "+" else "") + CurrencyFormatter.format(netCashFlow.netCashFlow, preferences),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = heroColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(
                        CurrencyFormatter.format(netCashFlow.income, preferences),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = RupeeFlowTheme.colors.income
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Spending", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    Text(
                        CurrencyFormatter.format(netCashFlow.spending, preferences),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = RupeeFlowTheme.colors.expense
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = netCashFlow.textSummary,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color(0xFFD1D5DB)
            )
        }
    }
}

@Composable
fun AnalyticsFilters(
    selectedFilter: String,
    selectedMonth: Calendar?,
    customRange: Pair<Long, Long>?,
    onFilterSelect: (String) -> Unit,
    onClearMonth: () -> Unit
) {
    val filters = listOf("1D", "1W", "1M", "3M", "6M", "1Y", "Custom")
    val monthFormat = remember { SimpleDateFormat("MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedMonth != null) {
            Surface(
                onClick = onClearMonth,
                color = RupeeFlowTheme.colors.income,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthFormat.format(selectedMonth.time),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.height(24.dp).padding(horizontal = 4.dp),
                color = Color(0xFF1F2937)
            )
        }

        filters.forEach { filter ->
            val isSelected = (selectedFilter == filter && selectedMonth == null) || (filter == "Custom" && customRange != null)
            Surface(
                onClick = { onFilterSelect(filter) },
                color = if (isSelected) RupeeFlowTheme.colors.income else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF1F2937))
            ) {
                Text(
                    text = filter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    trendPercent: Double?,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    isCurrency: Boolean = true,
    isInverseTrend: Boolean = false,
    periodLabel: String = "vs previous period",
    preferences: UserPreferences
) {
    Surface(
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isCurrency) CurrencyFormatter.format(amount, preferences) else amount.toInt().toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            val changeText = FinancialCalculations.formatPercentChangeText(trendPercent)
            val isUp = (trendPercent ?: 0.0) > 0
            val isPositiveOutcome = if (isInverseTrend) !isUp else isUp
            val badgeColor = if (changeText == "New") Color(0xFF9CA3AF) else if (isPositiveOutcome) RupeeFlowTheme.colors.income else RupeeFlowTheme.colors.expense

            Text(
                text = if (changeText == "New") "New" else "$changeText $periodLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = badgeColor
            )
        }
    }
}

@Composable
fun KeyInsightsSection(
    insights: List<Insight>,
    preferences: UserPreferences
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Key Insights",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )

        insights.forEach { insight ->
            Surface(
                color = Color(0xFF181818),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                modifier = Modifier.clickable { }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(insight.icon, fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(insight.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text(insight.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
                    }
                    if (insight.amount != null && insight.amount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            CurrencyFormatter.format(insight.amount, preferences),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveSpendingTrendSection(
    interactivePoints: List<InteractiveTrendPoint>,
    averageLabel: String,
    averageAmount: Double,
    preferences: UserPreferences
) {
    var selectedPoint by remember { mutableStateOf<InteractiveTrendPoint?>(null) }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Spending Trend", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("$averageLabel: ${CurrencyFormatter.format(averageAmount, preferences)}", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
            }

            Spacer(modifier = Modifier.height(16.dp))

            when {
                interactivePoints.isEmpty() || interactivePoints.all { it.amount == 0.0 } -> {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("No spending data for this period", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
                    }
                }
                else -> {
                    LineChart(
                        points = interactivePoints.map { LineChartPoint(it.timestamp.toFloat(), it.amount) },
                        modifier = Modifier.height(130.dp),
                        lineColor = RupeeFlowTheme.colors.income
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    selectedPoint?.let { point ->
                        Surface(
                            color = Color(0xFF262626),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(point.dateLabel, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                                    Text(point.topTransactionTitle ?: point.topCategory ?: "Daily Total", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                                }
                                Text(
                                    CurrencyFormatter.format(point.amount, preferences),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = RupeeFlowTheme.colors.expense
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankedCategoryBreakdownCard(
    categoryShares: List<FormattedCategoryShare>,
    totalSpent: Double,
    onViewAll: () -> Unit,
    onCategoryClick: (Long) -> Unit = {},
    preferences: UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                TextButton(onClick = onViewAll, contentPadding = PaddingValues(0.dp)) {
                    Text("View All", style = MaterialTheme.typography.labelLarge, color = RupeeFlowTheme.colors.income)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categoryShares.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No category spending data", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    categoryShares.forEachIndexed { index, share ->
                        RankedCategoryRow(
                            rank = index + 1,
                            share = share,
                            totalSpent = totalSpent,
                            onCategoryClick = onCategoryClick,
                            onViewAll = onViewAll,
                            preferences = preferences
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RankedCategoryRow(
    rank: Int,
    share: FormattedCategoryShare,
    totalSpent: Double,
    onCategoryClick: (Long) -> Unit = {},
    onViewAll: () -> Unit = {},
    preferences: UserPreferences
) {
    val color = parseCategoryColor(share.colorHex, rank)
    val progress = if (totalSpent > 0) (share.amount / totalSpent).toFloat().coerceIn(0f, 1f) else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (share.categoryId != null) {
                    onCategoryClick(share.categoryId)
                } else if (share.isOther) {
                    onViewAll()
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text("#$rank", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF9CA3AF))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = share.categoryName,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(share.amount, preferences),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = share.formattedPercentage,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = color
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = color,
            trackColor = Color(0xFF262626),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun CategoryTrendSection(
    trends: List<CategoryTrend>,
    onCategoryClick: (Long) -> Unit = {},
    preferences: UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("What's Changing?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                trends.take(5).forEach { trend ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                trend.categoryId?.let { onCategoryClick(it) }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(trend.categoryName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(CurrencyFormatter.format(trend.currentAmount, preferences), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        }

                        val badgeColor = when (trend.direction) {
                            TrendDirection.UP -> RupeeFlowTheme.colors.expense
                            TrendDirection.DOWN -> RupeeFlowTheme.colors.income
                            TrendDirection.NO_CHANGE -> Color(0xFF9CA3AF)
                            TrendDirection.NEW -> Color(0xFF3B82F6)
                        }

                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = trend.formattedChange,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MerchantAndAnomaliesSection(
    highestExpense: Transaction?,
    topMerchant: MerchantInfo?,
    anomalies: List<SpendingAnomaly>,
    preferences: UserPreferences
) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            InsightMiniCard(
                title = "Biggest Expense",
                subtitle = highestExpense?.title ?: "N/A",
                value = CurrencyFormatter.format(highestExpense?.amount ?: 0.0, preferences),
                footer = highestExpense?.category ?: "",
                modifier = Modifier.weight(1f)
            )
            InsightMiniCard(
                title = "Most Frequent",
                subtitle = topMerchant?.name ?: "N/A",
                value = "${topMerchant?.transactionCount ?: 0} Trans",
                footer = CurrencyFormatter.format(topMerchant?.totalAmount ?: 0.0, preferences),
                modifier = Modifier.weight(1f)
            )
        }

        if (anomalies.isNotEmpty()) {
            anomalies.forEach { anomaly ->
                Surface(
                    color = RupeeFlowTheme.colors.expense.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, RupeeFlowTheme.colors.expense.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = RupeeFlowTheme.colors.expense, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(anomaly.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                            Text(anomaly.description, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD1D5DB))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InsightMiniCard(
    title: String,
    subtitle: String,
    value: String,
    footer: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { },
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = RupeeFlowTheme.colors.income)
            Text(footer, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
