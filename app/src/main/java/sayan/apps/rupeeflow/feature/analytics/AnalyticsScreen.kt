package sayan.apps.rupeeflow.feature.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.DonutChart
import sayan.apps.rupeeflow.core.designsystem.components.DonutChartData
import sayan.apps.rupeeflow.core.designsystem.components.LineChart
import sayan.apps.rupeeflow.core.designsystem.components.LineChartPoint
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.UserPreferences

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Filters Row
        item {
            AnalyticsFilters(
                selectedFilter = uiState.selectedFilter,
                isFinancialYear = uiState.isFinancialYear,
                onFilterSelect = { viewModel.onFilterSelected(it) },
                onToggleFY = { viewModel.toggleFinancialYear() }
            )
        }

        // Summary Cards Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "Total Spent",
                    amount = uiState.totalSpent,
                    trendPercent = uiState.spentTrendPercent,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    iconColor = Color(0xFFEF4444),
                    preferences = preferences
                )
                SummaryCard(
                    title = "Total Income",
                    amount = uiState.totalIncome,
                    trendPercent = uiState.incomeTrendPercent,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    iconColor = Color(0xFF10B981),
                    preferences = preferences
                )
                SummaryCard(
                    title = "Net Savings",
                    amount = uiState.netSavings,
                    trendPercent = uiState.savingsTrendPercent,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    iconColor = Color(0xFF10B981),
                    preferences = preferences
                )
                SummaryCard(
                    title = "Transactions",
                    amount = uiState.transactionCount.toDouble(),
                    trendPercent = uiState.transTrendPercent,
                    icon = Icons.Rounded.PieChart,
                    iconColor = Color(0xFF3B82F6),
                    isCurrency = false,
                    preferences = preferences
                )
            }
        }

        // Spending Trend
        item {
            SpendingTrendCard(uiState.spendingTrend, uiState.dailyAverage, preferences)
        }

        // Category Breakdown
        item {
            CategoryBreakdownCard(uiState.categorySpending, uiState.totalSpent, preferences)
        }

        // Additional Insights
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InsightMiniCard(
                    title = "Highest Expense",
                    subtitle = uiState.highestExpense?.title ?: "N/A",
                    value = CurrencyFormatter.format(uiState.highestExpense?.amount ?: 0.0, preferences),
                    footer = uiState.highestExpense?.category ?: "",
                    modifier = Modifier.weight(1f)
                )
                InsightMiniCard(
                    title = "Top Merchant",
                    subtitle = uiState.topMerchantName,
                    value = "${uiState.topMerchantTrans} Trans",
                    footer = CurrencyFormatter.format(uiState.topMerchantAmount, preferences),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Highlight Banner
        item {
            InsightBanner()
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
        modifier = modifier,
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
            Text(footer, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
        }
    }
}

@Composable
fun AnalyticsFilters(
    selectedFilter: String,
    isFinancialYear: Boolean,
    onFilterSelect: (String) -> Unit,
    onToggleFY: () -> Unit
) {
    val filters = listOf("7D", "30D", "3M", "6M", "YTD", "1Y", "Custom")
    
    Column(modifier = Modifier.padding(top = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            filters.forEach { filter ->
                val isSelected = selectedFilter == filter
                Surface(
                    onClick = { onFilterSelect(filter) },
                    color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
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
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("FY", style = MaterialTheme.typography.labelMedium, color = Color.White)
                Switch(
                    checked = isFinancialYear,
                    onCheckedChange = { onToggleFY() },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                )
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    trendPercent: Double,
    icon: ImageVector,
    iconColor: Color,
    isCurrency: Boolean = true,
    preferences: UserPreferences
) {
    Surface(
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.width(160.dp)
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
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val isUp = trendPercent > 0
                Icon(
                    imageVector = if (isUp) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = null,
                    tint = if (isUp) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${Math.abs(trendPercent).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isUp) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
fun SpendingTrendCard(
    trend: List<sayan.apps.rupeeflow.domain.repository.TrendPoint>, 
    dailyAverage: Double,
    preferences: UserPreferences
) {
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
                Column {
                    Text("Spending Trend", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    Text("Daily average: ${CurrencyFormatter.format(dailyAverage, preferences)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                }
                
                Surface(
                    color = Color(0xFF1E1E1E),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { }
                ) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.StackedLineChart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Line Chart", style = MaterialTheme.typography.labelSmall, color = Color.White)
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            LineChart(
                points = trend.map { LineChartPoint(it.timestamp.toFloat(), it.amount) },
                modifier = Modifier.height(180.dp),
                lineColor = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun CategoryBreakdownCard(
    spending: List<sayan.apps.rupeeflow.domain.repository.CategorySpending>, 
    total: Double,
    preferences: UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Category Breakdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("View All", style = MaterialTheme.typography.labelLarge, color = Color(0xFF10B981), modifier = Modifier.clickable { })
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                    DonutChart(
                        data = spending.map { 
                            DonutChartData(it.amount, Color(android.graphics.Color.parseColor(it.colorHex))) 
                        },
                        thickness = 20.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(CurrencyFormatter.format(total, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                }
                
                Spacer(modifier = Modifier.width(24.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                    spending.take(5).forEach { item ->
                        CategoryBreakdownItem(item, total, preferences)
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownItem(item: sayan.apps.rupeeflow.domain.repository.CategorySpending, total: Double, preferences: UserPreferences) {
    val percent = if (total > 0) ((item.amount / total) * 100).toInt() else 0
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(modifier = Modifier.size(24.dp), shape = CircleShape, color = Color(android.graphics.Color.parseColor(item.colorHex)).copy(alpha = 0.2f)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Category, contentDescription = null, tint = Color(android.graphics.Color.parseColor(item.colorHex)), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(item.categoryName, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
        }
        Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.labelSmall, color = Color.White)
        Spacer(modifier = Modifier.width(8.dp))
        Text("$percent%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFFEF4444))
    }
}

@Composable
fun InsightBanner() {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Stars, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Keep it up! 🎊", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Text("You are tracking your expenses perfectly this period.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
            }
            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color(0xFF9CA3AF))
        }
    }
}
