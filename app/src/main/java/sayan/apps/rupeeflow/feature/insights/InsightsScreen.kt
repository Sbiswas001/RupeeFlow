package sayan.apps.rupeeflow.feature.insights

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.MonthPickerDialog
import sayan.apps.rupeeflow.core.designsystem.components.CustomDateRangePickerDialog
import sayan.apps.rupeeflow.feature.analytics.AnalyticsScreen
import sayan.apps.rupeeflow.feature.analytics.AnalyticsViewModel
import sayan.apps.rupeeflow.feature.planning.PlanningScreen
import sayan.apps.rupeeflow.feature.planning.PlanningViewModel
import sayan.apps.rupeeflow.feature.reports.ReportsScreen
import sayan.apps.rupeeflow.feature.reports.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun InsightsScreen(
    onNavigateToAddRecurring: () -> Unit,
    onNavigateToViewAll: () -> Unit = {},
    onNavigateToCategoryDetail: (Long) -> Unit = {},
    initialTab: Int = 0,
    modifier: Modifier = Modifier,
    analyticsViewModel: AnalyticsViewModel = hiltViewModel(),
    planningViewModel: PlanningViewModel = hiltViewModel(),
    reportsViewModel: ReportsViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Analytics", "Planning", "Reports")
    val icons = listOf(Icons.Rounded.Assessment, Icons.Rounded.EventNote, Icons.Rounded.Description)

    var showMonthPicker by remember { mutableStateOf(false) }
    var showCustomRangePicker by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf<Calendar?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Global Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = "Understand your money better",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF9CA3AF)
                )
            }

            IconButton(
                onClick = {
                    if (selectedTabIndex == 0) {
                        showCustomRangePicker = true
                    } else {
                        showMonthPicker = true
                    }
                },
                modifier = Modifier
                    .background(Color(0xFF1F2937), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = "Select Date Range",
                    tint = if (selectedMonth != null) Color(0xFF10B981) else Color.White
                )
            }
        }

        // Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            tabs.forEachIndexed { index, title ->
                val isSelected = selectedTabIndex == index
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTabIndex = index }
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icons[index],
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF10B981) else Color(0xFF9CA3AF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isSelected) Color(0xFF10B981) else Color(0xFF9CA3AF)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AnimatedVisibility(
                        visible = isSelected,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTabIndex) {
                0 -> AnalyticsScreen(
                    onNavigateToViewAll = onNavigateToViewAll,
                    onNavigateToCategoryDetail = onNavigateToCategoryDetail,
                    showTitle = false,
                    viewModel = analyticsViewModel
                )
                1 -> PlanningScreen(
                    onNavigateToAddRecurring = onNavigateToAddRecurring,
                    showTitle = false,
                    viewModel = planningViewModel
                )
                2 -> ReportsScreen(showTitle = false, viewModel = reportsViewModel)
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            initialMonth = selectedMonth ?: Calendar.getInstance(),
            onDismiss = { showMonthPicker = false },
            onConfirm = { cal ->
                selectedMonth = cal
                planningViewModel.onMonthSelected(cal)
                reportsViewModel.onMonthSelected(cal)
                showMonthPicker = false
            }
        )
    }

    if (showCustomRangePicker) {
        CustomDateRangePickerDialog(
            onDismiss = { showCustomRangePicker = false },
            onConfirm = { start, end ->
                analyticsViewModel.onCustomRangeSelected(start, end)
                showCustomRangePicker = false
            }
        )
    }
}
