package sayan.apps.rupeeflow.feature.planning

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.database.entity.GoalContributionEntity
import sayan.apps.rupeeflow.core.designsystem.components.BudgetProgressBar
import sayan.apps.rupeeflow.core.designsystem.components.MonthPickerDialog
import sayan.apps.rupeeflow.core.financial.ForecastResult
import sayan.apps.rupeeflow.core.financial.SafeToSpendResult
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Goal
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UserPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanningScreen(
    onNavigateToAddRecurring: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    viewModel: PlanningViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val uiState by viewModel.uiState.collectAsState()
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToContribute by remember { mutableStateOf<Goal?>(null) }
    var goalForDetail by remember { mutableStateOf<Goal?>(null) }
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }
    var selectedBudgetForActions by remember { mutableStateOf<BudgetWithProgress?>(null) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            if (showTitle) {
                item {
                    PlanningHeader(
                        selectedMonth = uiState.selectedMonth,
                        onMonthClick = { showMonthPicker = true }
                    )
                }
            }

            // Safe Spending Hero Card
            item {
                SafeSpendingHero(
                    safeToSpendResult = uiState.safeToSpendResult,
                    onSetBudgetClick = { showAddBudgetDialog = true },
                    preferences = preferences
                )
            }

            // Planning Summary Row
            item {
                PlanningSummaryRow(
                    budgetUsagePercent = uiState.budgetUsageResult.overallUsagePercent.toInt(),
                    recurringTotal = uiState.recurringItems.sumOf { it.amount },
                    forecastAmount = uiState.forecastResult.estimatedMonthEnd,
                    preferences = preferences
                )
            }

            // Monthly Budgets Section Header
            item {
                SectionHeader(title = "Monthly Budgets", onAdd = { showAddBudgetDialog = true })
            }

            if (uiState.budgets.isEmpty()) {
                item {
                    EmptySectionCard(
                        text = "No budgets set for this month. Tap + to set a spending limit.",
                        onAdd = { showAddBudgetDialog = true }
                    )
                }
            } else {
                items(uiState.budgets) { budget ->
                    BudgetCard(
                        budget = budget,
                        onClick = { selectedBudgetForActions = budget },
                        preferences = preferences
                    )
                }
            }

            // Savings Goals Section Header
            item {
                SectionHeader(title = "Savings Goals", onAdd = { showAddGoalDialog = true })
            }

            if (uiState.goals.isEmpty()) {
                item {
                    EmptySectionCard(
                        text = "No savings goals created yet. Tap + to create your first goal.",
                        onAdd = { showAddGoalDialog = true }
                    )
                }
            } else {
                items(uiState.goals) { goal ->
                    ImprovedGoalCard(
                        goal = goal,
                        onClick = { goalForDetail = goal },
                        onContribute = { goalToContribute = goal },
                        onDelete = { goalToDelete = goal },
                        preferences = preferences
                    )
                }
            }

            // Upcoming Expenses / Recurring
            if (uiState.recurringItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Upcoming Expenses", onAdd = { onNavigateToAddRecurring() })
                }
                item {
                    UpcomingExpensesCard(items = uiState.recurringItems, preferences = preferences)
                }
            }

            // Forecast Card
            item {
                ForecastCard(forecast = uiState.forecastResult, preferences = preferences)
            }

            // Insights Section
            if (uiState.insights.isNotEmpty()) {
                item {
                    PlanningInsightsSection(insights = uiState.insights)
                }
            }
        }
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = uiState.categories,
            budget = selectedBudgetForActions?.budget,
            onDismiss = {
                showAddBudgetDialog = false
                selectedBudgetForActions = null
            },
            onConfirm = { catId: Long, amount: Double, period: String ->
                if (selectedBudgetForActions == null) {
                    viewModel.addBudget(catId, amount, period)
                } else {
                    viewModel.updateBudget(
                        selectedBudgetForActions!!.budget.copy(
                            limitAmount = amount,
                            period = period
                        )
                    )
                }
                showAddBudgetDialog = false
                selectedBudgetForActions = null
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title, targetAmount, startingAmount, targetDate ->
                viewModel.addGoal(title, targetAmount, startingAmount, targetDate)
                showAddGoalDialog = false
            }
        )
    }

    goalToContribute?.let { goal ->
        AddContributionDialog(
            goal = goal,
            onDismiss = { goalToContribute = null },
            onConfirm = { amount ->
                viewModel.addGoalContribution(goal, amount)
                goalToContribute = null
            },
            preferences = preferences
        )
    }

    goalForDetail?.let { goal ->
        GoalDetailBottomSheet(
            goal = goal,
            onDismiss = { goalForDetail = null },
            onContributeClick = {
                goalToContribute = goal
                goalForDetail = null
            },
            onDeleteContribution = { contributionId, amount ->
                viewModel.deleteGoalContribution(goal.id, contributionId, amount)
            },
            viewModel = viewModel,
            preferences = preferences
        )
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

    selectedBudgetForActions?.let { budget ->
        BudgetActionBottomSheet(
            budget = budget,
            onDismiss = { selectedBudgetForActions = null },
            onDelete = {
                viewModel.deleteBudget(budget.budget)
                selectedBudgetForActions = null
            },
            onEdit = {
                showAddBudgetDialog = true
            },
            onReset = {
                viewModel.resetBudgetProgress(budget.budget)
                selectedBudgetForActions = null
            }
        )
    }

    budgetToDelete?.let { budget: Budget ->
        DeleteConfirmationDialog(
            title = "Delete Budget",
            message = "Are you sure you want to delete the budget for '${budget.categoryName}'?",
            onConfirm = {
                viewModel.deleteBudget(budget)
                budgetToDelete = null
            },
            onDismiss = { budgetToDelete = null }
        )
    }

    goalToDelete?.let { goal ->
        DeleteConfirmationDialog(
            title = "Delete Savings Goal",
            message = "Are you sure you want to delete '${goal.title}'?",
            onConfirm = {
                viewModel.deleteGoal(goal)
                goalToDelete = null
            },
            onDismiss = { goalToDelete = null }
        )
    }
}

@Composable
fun PlanningHeader(selectedMonth: Calendar, onMonthClick: () -> Unit) {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = Color(0xFF181818),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.clickable { onMonthClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(monthFormat.format(selectedMonth.time), style = MaterialTheme.typography.labelLarge, color = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun SafeSpendingHero(
    safeToSpendResult: SafeToSpendResult,
    onSetBudgetClick: () -> Unit,
    preferences: UserPreferences
) {
    var showFormulaDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Safe to Spend Today", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                }

                if (safeToSpendResult.hasConfiguredBudget) {
                    IconButton(
                        onClick = { showFormulaDialog = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "How is this calculated?", tint = Color(0xFF9CA3AF), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!safeToSpendResult.hasConfiguredBudget) {
                Text(
                    text = "Set a monthly budget",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Set a monthly budget to calculate your safe spending limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSetBudgetClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Monthly Budget", fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = CurrencyFormatter.format(safeToSpendResult.safeToSpendToday, preferences),
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Remaining Budget", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        Text(CurrencyFormatter.format(safeToSpendResult.remainingBudget, preferences), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Days Left", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                        Text("${safeToSpendResult.daysRemaining} days", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = safeToSpendResult.statusMessage,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (safeToSpendResult.statusMessage.contains("over budget") || safeToSpendResult.statusMessage.contains("above target") || safeToSpendResult.statusMessage.contains("exceeded")) Color(0xFFEF4444) else Color(0xFF10B981),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
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
                        color = Color(0xFF10B981)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFormulaDialog = false }) {
                    Text("Got it", color = Color(0xFF10B981))
                }
            },
            containerColor = Color(0xFF181818),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun PlanningSummaryRow(
    budgetUsagePercent: Int,
    recurringTotal: Double,
    forecastAmount: Double,
    preferences: UserPreferences
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMiniCard("Budget Used", "$budgetUsagePercent%", Modifier.weight(1f))
        SummaryMiniCard("Recurring", CurrencyFormatter.format(recurringTotal, preferences), Modifier.weight(1.2f))
        SummaryMiniCard("Forecast", CurrencyFormatter.format(forecastAmount, preferences), Modifier.weight(1.2f))
    }
}

@Composable
fun SummaryMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xFF181818),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = if (value.length > 10) MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BudgetCard(
    budget: BudgetWithProgress,
    onClick: () -> Unit,
    preferences: UserPreferences
) {
    val progressColor = when {
        budget.progress > 1f || budget.remaining < 0 -> Color(0xFFEF4444) // Over budget
        budget.progress >= 1f || budget.remaining <= 0.0 -> Color(0xFFEF4444) // Limit reached
        budget.progress >= 0.8f -> Color(0xFFF59E0B) // Approaching limit
        else -> Color(0xFF10B981) // On track
    }

    val statusText = when {
        budget.progress > 1f || budget.remaining < 0 -> "Over budget"
        budget.progress >= 1f || budget.remaining <= 0.0 -> "Limit reached"
        budget.progress >= 0.8f -> "Approaching limit"
        else -> "On track"
    }

    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().clickable { onClick() },
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(budget.budget.categoryIcon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = budget.budget.categoryName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                Text(
                    text = CurrencyFormatter.format(budget.budget.spentAmount, preferences),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${CurrencyFormatter.format(budget.budget.limitAmount, preferences)} Budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9CA3AF)
                )

                Surface(
                    color = progressColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = progressColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                BudgetProgressBar(
                    progress = budget.progress,
                    modifier = Modifier.weight(1f).height(8.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("${(budget.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (budget.remaining >= 0) "${CurrencyFormatter.format(budget.remaining, preferences)} left" else "${CurrencyFormatter.format(Math.abs(budget.remaining), preferences)} over budget",
                style = MaterialTheme.typography.labelSmall,
                color = if (budget.remaining >= 0) Color(0xFF9CA3AF) else Color(0xFFEF4444)
            )
        }
    }
}

@Composable
fun ImprovedGoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    preferences: UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().clickable { onClick() },
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = goal.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = null, tint = Color(0xFFEF4444).copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "${CurrencyFormatter.format(goal.currentAmount, preferences)} saved of ${CurrencyFormatter.format(goal.targetAmount, preferences)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = if (goal.isCompleted) "🎉 Goal Reached!" else "${(goal.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF10B981)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { goal.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (goal.isCompleted) Color(0xFF10B981) else Color(0xFF8B5CF6),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (goal.isCompleted) "Goal complete!" else "${CurrencyFormatter.format(goal.remainingAmount, preferences)} remaining",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9CA3AF)
                )

                if (!goal.isCompleted) {
                    TextButton(onClick = onContribute, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add contribution", style = MaterialTheme.typography.labelMedium, color = Color(0xFF10B981))
                    }
                }
            }

            if (goal.targetDate != null && !goal.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                Spacer(modifier = Modifier.height(12.dp))

                val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(goal.targetDate))
                val daysLeft = ((goal.targetDate - System.currentTimeMillis()) / 86400000L).coerceAtLeast(1)

                val pacingText = if (daysLeft < 30) {
                    val weeksLeft = (daysLeft / 7L).coerceAtLeast(1)
                    val weeklySave = goal.remainingAmount / weeksLeft
                    "Save ${CurrencyFormatter.format(weeklySave, preferences)}/week"
                } else {
                    val monthsLeft = (daysLeft / 30L).coerceAtLeast(1)
                    val monthlySave = goal.remainingAmount / monthsLeft
                    "Save ${CurrencyFormatter.format(monthlySave, preferences)}/month"
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target: $dateStr", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))

                    if (goal.remainingAmount > 0) {
                        Text(
                            text = pacingText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailBottomSheet(
    goal: Goal,
    onDismiss: () -> Unit,
    onContributeClick: () -> Unit,
    onDeleteContribution: (Long, Double) -> Unit,
    viewModel: PlanningViewModel,
    preferences: UserPreferences
) {
    val contributions by viewModel.getGoalContributions(goal.id).collectAsState(initial = emptyList())

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.1f)) }
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(goal.icon, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(goal.title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }

                if (!goal.isCompleted) {
                    Button(
                        onClick = onContributeClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contribute")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${CurrencyFormatter.format(goal.currentAmount, preferences)} saved of ${CurrencyFormatter.format(goal.targetAmount, preferences)}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(if (goal.isCompleted) "🎉 Goal Reached!" else "${(goal.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
            }

            LinearProgressIndicator(
                progress = { goal.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (goal.isCompleted) Color(0xFF10B981) else Color(0xFF8B5CF6),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeCap = StrokeCap.Round
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Text("Contribution History", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)

            if (contributions.isEmpty()) {
                Text("No contributions recorded yet. Tap + Contribute to add your first contribution.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9CA3AF))
            } else {
                val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    contributions.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("+${CurrencyFormatter.format(item.amount, preferences)}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color(0xFF10B981))
                                Text(dateFormat.format(Date(item.createdAt)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                            }

                            IconButton(onClick = { onDeleteContribution(item.id, item.amount) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Undo Contribution", tint = Color(0xFFEF4444).copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddContributionDialog(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    preferences: UserPreferences
) {
    var amountText by remember { mutableStateOf("") }
    val quickChips = listOf(500.0, 1000.0, 2500.0, 5000.0)

    val currentInputAmount = amountText.toDoubleOrNull() ?: 0.0
    val isExceedingRemaining = goal.remainingAmount > 0 && currentInputAmount > goal.remainingAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contribute to ${goal.title}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Remaining: ${CurrencyFormatter.format(goal.remainingAmount, preferences)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9CA3AF)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickChips.forEach { chip ->
                        FilterChip(
                            selected = amountText == chip.toInt().toString(),
                            onClick = { amountText = chip.toInt().toString() },
                            label = { Text("+₹${chip.toInt()}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.Black
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                if (isExceedingRemaining) {
                    Text(
                        text = "You only need ${CurrencyFormatter.format(goal.remainingAmount, preferences)} to reach this goal. Amount will be capped at ${CurrencyFormatter.format(goal.remainingAmount, preferences)}.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF59E0B)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                },
                enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
            ) {
                Text("Contribute")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF181818),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun UpcomingExpensesCard(items: List<RecurringItem>, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                            if (item.status == "PENDING") {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Recurring",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF3B82F6)
                                    )
                                }
                            }
                        }
                        Text(dateFormat.format(Date(item.dueDate)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                    Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
                if (index < items.size - 1) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun ForecastCard(forecast: ForecastResult, preferences: UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Forecast", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                Surface(
                    color = if (forecast.isPacingOverBudget || forecast.isProjectedDeficit) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when {
                            forecast.isPacingOverBudget -> "Pacing Alert"
                            forecast.isProjectedDeficit -> "Balance Alert"
                            else -> "On Track"
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (forecast.isPacingOverBudget || forecast.isProjectedDeficit) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ForecastRow("Current Balance", forecast.currentBalance, Color.White, preferences = preferences)
            ForecastRow("Scheduled Income", forecast.expectedIncome, Color(0xFF10B981), preferences = preferences)
            ForecastRow("Upcoming Bills", if (forecast.upcomingBills == 0.0) 0.0 else -forecast.upcomingBills, Color(0xFFEF4444), preferences = preferences)
            ForecastRow("Projected Spending", if (forecast.projectedRemainingSpending == 0.0) 0.0 else -forecast.projectedRemainingSpending, Color(0xFFF59E0B), preferences = preferences)

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))

            ForecastRow("Estimated Month End", forecast.estimatedMonthEnd, Color(0xFF8B5CF6), isBold = true, preferences = preferences)

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = forecast.statusMessage,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF9CA3AF)
            )
        }
    }
}

@Composable
fun EmptySectionCard(text: String, onAdd: () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .clickable { onAdd() },
        color = Color(0xFF181818),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Add, contentDescription = null, tint = Color(0xFF10B981))
        }
    }
}

@Composable
fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        IconButton(onClick = onAdd) {
            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = Color(0xFF10B981))
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetDialog(
    categories: List<Category>,
    budget: Budget? = null,
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String) -> Unit
) {
    val expenseCategories = remember(categories) {
        categories.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }
    }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: expenseCategories.firstOrNull()?.id ?: 0L) }
    var amount by remember { mutableStateOf(budget?.limitAmount?.toString() ?: "") }
    var period by remember { mutableStateOf(budget?.period ?: "MONTHLY") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Set Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (budget == null) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        val selectedCat = expenseCategories.find { it.id == selectedCategoryId }
                        OutlinedTextField(
                            value = selectedCat?.name ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = budget.categoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly Limit") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedCategoryId, amount.toDoubleOrNull() ?: 0.0, period)
                },
                enabled = amount.isNotEmpty() && selectedCategoryId != 0L,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
            ) {
                Text(if (budget == null) "Set Budget" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, targetAmount: Double, startingAmount: Double, targetDate: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var startingAmountText by remember { mutableStateOf("") }
    var showEarmarkedSection by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you saving for?") },
                    placeholder = { Text("e.g. Galaxy Watch 8") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("How much do you need?") },
                    placeholder = { Text("e.g. 21000") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Surface(
                    onClick = { showEarmarkedSection = !showEarmarkedSection },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("Already have money set aside?", style = MaterialTheme.typography.bodySmall, color = Color(0xFF10B981))
                        Icon(
                            imageVector = if (showEarmarkedSection) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF10B981)
                        )
                    }
                }

                if (showEarmarkedSection) {
                    OutlinedTextField(
                        value = startingAmountText,
                        onValueChange = { startingAmountText = it },
                        label = { Text("Amount already set aside") },
                        placeholder = { Text("e.g. 3000") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetVal = target.toDoubleOrNull() ?: 0.0
                    val startVal = startingAmountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && targetVal > 0) {
                        onConfirm(title.trim(), targetVal, startVal, null)
                    }
                },
                enabled = title.isNotBlank() && (target.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981), contentColor = Color.Black)
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF181818),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@Composable
fun ForecastRow(label: String, amount: Double, valueColor: Color, isBold: Boolean = false, preferences: UserPreferences) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF9CA3AF))
        Text(
            text = CurrencyFormatter.format(amount, preferences),
            style = if (isBold) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) else MaterialTheme.typography.bodyMedium,
            color = valueColor
        )
    }
}

@Composable
fun PlanningInsightsSection(insights: List<PlanningInsight>) {
    Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        insights.forEach { insight ->
            val bgColor = when (insight.type) {
                InsightType.SUCCESS -> Color(0xFF10B981).copy(alpha = 0.1f)
                InsightType.WARNING -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                InsightType.ALERT -> Color(0xFFEF4444).copy(alpha = 0.1f)
                InsightType.INFO -> Color(0xFF3B82F6).copy(alpha = 0.1f)
            }
            val icon = when (insight.type) {
                InsightType.SUCCESS -> Icons.Rounded.CheckCircle
                InsightType.WARNING -> Icons.Rounded.Warning
                InsightType.ALERT -> Icons.Rounded.NotificationsActive
                InsightType.INFO -> Icons.Rounded.Info
            }
            val tint = when (insight.type) {
                InsightType.SUCCESS -> Color(0xFF10B981)
                InsightType.WARNING -> Color(0xFFF59E0B)
                InsightType.ALERT -> Color(0xFFEF4444)
                InsightType.INFO -> Color(0xFF3B82F6)
            }

            Surface(color = bgColor, shape = RoundedCornerShape(16.dp), border = BorderStroke(0.5.dp, tint.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(insight.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetActionBottomSheet(
    budget: BudgetWithProgress,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onReset: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.1f)) }
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(budget.budget.categoryName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            ActionItem("Edit Budget", Icons.Rounded.Edit, onClick = onEdit)
            ActionItem("Reset Progress", Icons.Rounded.RestartAlt, onClick = onReset)
            ActionItem("Delete", Icons.Rounded.Delete, tint = Color(0xFFEF4444), onClick = onDelete)
        }
    }
}

@Composable
fun ActionItem(label: String, icon: ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
        }
    }
}
