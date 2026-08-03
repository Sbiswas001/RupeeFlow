package sayan.apps.rupeeflow.feature.planning

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Goal
import sayan.apps.rupeeflow.domain.model.RecurringItem
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
    var budgetToDelete by remember { mutableStateOf<Budget?>(null) }
    var goalToDelete by remember { mutableStateOf<Goal?>(null) }
    var selectedBudgetForActions by remember { mutableStateOf<BudgetWithProgress?>(null) }
    var isFabExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                PlanningHeader(
                    selectedMonth = uiState.selectedMonth,
                    onMonthClick = { /* Month Picker */ }
                )
            }

            item {
                SafeSpendingHero(
                    safeToSpend = uiState.safeSpendingLimit,
                    remainingBudget = uiState.remainingMonthlyBudget,
                    daysLeft = uiState.daysLeftInMonth,
                    dailyTarget = uiState.dailyTarget,
                    preferences = preferences
                )
            }

            item {
                PlanningSummaryRow(
                    budgetUsed = uiState.totalBudgetUsedPercent,
                    recurringTotal = uiState.totalRecurringBills,
                    forecastAmount = uiState.forecast.estimatedMonthEnd,
                    preferences = preferences
                )
            }

            // Budgets Section
            item {
                SectionHeader(title = "Monthly Budgets", onAdd = { showAddBudgetDialog = true })
            }

            items(uiState.budgets) { budget ->
                BudgetCard(
                    budget = budget,
                    onClick = { selectedBudgetForActions = budget },
                    preferences = preferences
                )
            }

            // Savings Goals
            item {
                SectionHeader(title = "Savings Goals", onAdd = { showAddGoalDialog = true })
            }

            items(uiState.goals) { goal ->
                ImprovedGoalCard(
                    goal = goal,
                    onDelete = { goalToDelete = goal },
                    preferences = preferences
                )
            }

            // Upcoming Expenses
            if (uiState.recurringItems.isNotEmpty()) {
                item {
                    SectionHeader(title = "Upcoming Expenses", onAdd = {})
                }
                item {
                    UpcomingExpensesCard(items = uiState.recurringItems, preferences = preferences)
                }
            }

            // Forecast Card
            item {
                ForecastCard(forecast = uiState.forecast, preferences = preferences)
            }

            // Insights
            if (uiState.insights.isNotEmpty()) {
                item {
                    PlanningInsightsSection(insights = uiState.insights)
                }
            }
        }

        // Floating Action Button (One UI Style)
        ExpandingFAB(
            isExpanded = isFabExpanded,
            onExpandChange = { isFabExpanded = it },
            onAddBudget = { showAddBudgetDialog = true; isFabExpanded = false },
            onAddGoal = { showAddGoalDialog = true; isFabExpanded = false },
            onAddRecurring = { onNavigateToAddRecurring(); isFabExpanded = false },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            categories = uiState.categories,
            onDismiss = { showAddBudgetDialog = false },
            onConfirm = { catId: Long, amount: Double, period: String ->
                viewModel.addBudget(catId, amount, period)
                showAddBudgetDialog = false
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onConfirm = { title: String, target: Double, current: Double ->
                viewModel.addGoal(title, target, current)
                showAddGoalDialog = false
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
    safeToSpend: Double,
    remainingBudget: Double,
    daysLeft: Int,
    dailyTarget: Double,
    preferences: sayan.apps.rupeeflow.domain.model.UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(28.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Safe to Spend Today", style = MaterialTheme.typography.titleMedium, color = Color(0xFF10B981))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = CurrencyFormatter.format(safeToSpend, preferences),
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Remaining Budget", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text(CurrencyFormatter.format(remainingBudget, preferences), style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Days Left", style = MaterialTheme.typography.labelMedium, color = Color(0xFF9CA3AF))
                    Text("$daysLeft days", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "≈ ${CurrencyFormatter.format(dailyTarget, preferences)}/day to stay on budget",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PlanningSummaryRow(
    budgetUsed: Int,
    recurringTotal: Double,
    forecastAmount: Double,
    preferences: sayan.apps.rupeeflow.domain.model.UserPreferences
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMiniCard("Budget Used", "$budgetUsed%", Modifier.weight(1f))
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
fun BudgetCard(
    budget: BudgetWithProgress,
    onClick: () -> Unit,
    preferences: sayan.apps.rupeeflow.domain.model.UserPreferences
) {
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
                    text = "${CurrencyFormatter.format(budget.budget.spentAmount, preferences)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "${CurrencyFormatter.format(budget.budget.limitAmount, preferences)} Budget",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF9CA3AF)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            val progressColor = when {
                budget.progress > 1f -> Color(0xFFEF4444) // Exceeded
                budget.progress > 0.9f -> Color(0xFFF59E0B) // Alert
                budget.progress > 0.7f -> Color(0xFFFBBF24) // Warning
                else -> Color(0xFF10B981) // Good
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { budget.progress.coerceIn(0f, 1f) },
                    modifier = Modifier.weight(1f).height(10.dp),
                    color = progressColor,
                    trackColor = Color.White.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
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
    onDelete: () -> Unit,
    preferences: sayan.apps.rupeeflow.domain.model.UserPreferences
) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
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
                    text = "${CurrencyFormatter.format(goal.currentAmount, preferences)} / ${CurrencyFormatter.format(goal.targetAmount, preferences)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text("${(goal.progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { goal.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(0xFF7C3AED),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (goal.targetDate != null) {
                Spacer(modifier = Modifier.height(16.dp))
                val dateStr = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(goal.targetDate))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target: $dateStr", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    
                    val monthsLeft = ((goal.targetDate - System.currentTimeMillis()) / (1000L * 60 * 60 * 24 * 30)).coerceAtLeast(1)
                    val needed = (goal.targetAmount - goal.currentAmount) / monthsLeft
                    if (needed > 0) {
                        Text("Need ${CurrencyFormatter.format(needed, preferences)}/month", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }
                }
            }
        }
    }
}

@Composable
fun UpcomingExpensesCard(items: List<RecurringItem>, preferences: sayan.apps.rupeeflow.domain.model.UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            items.forEachIndexed { index, item ->
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(item.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
                        Text(dateFormat.format(Date(item.dueDate)), style = MaterialTheme.typography.labelSmall, color = Color(0xFF9CA3AF))
                    }
                    Text(CurrencyFormatter.format(item.amount, preferences), style = MaterialTheme.typography.bodyLarge, color = Color.White)
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
fun ForecastCard(forecast: ForecastData, preferences: sayan.apps.rupeeflow.domain.model.UserPreferences) {
    Surface(
        modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        color = Color(0xFF181818),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Forecast", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(24.dp))
            ForecastRow("Current Balance", forecast.currentBalance, Color.White, preferences = preferences)
            ForecastRow("Expected Income", forecast.expectedIncome, Color(0xFF10B981), preferences = preferences)
            ForecastRow("Upcoming Bills", -forecast.upcomingBills, Color(0xFFEF4444), preferences = preferences)
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            ForecastRow("Estimated Month End", forecast.estimatedMonthEnd, Color(0xFF7C3AED), isBold = true, preferences = preferences)
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
    onDismiss: () -> Unit,
    onConfirm: (Long, Double, String) -> Unit
) {
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: 0L) }
    var amount by remember { mutableStateOf("") }
    var period by remember { mutableStateOf("MONTHLY") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    val selectedCat = categories.find { it.id == selectedCategoryId }
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
                        categories.forEach { cat ->
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

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly Limit") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(selectedCategoryId, amount.toDoubleOrNull() ?: 0.0, period) 
            }, enabled = amount.isNotEmpty() && selectedCategoryId != 0L) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var current by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text("Already Saved") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(title, target.toDoubleOrNull() ?: 0.0, current.toDoubleOrNull() ?: 0.0) 
            }, enabled = title.isNotEmpty() && target.isNotEmpty()) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ForecastRow(label: String, amount: Double, valueColor: Color, isBold: Boolean = false, preferences: sayan.apps.rupeeflow.domain.model.UserPreferences) {
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
            
            Surface(color = bgColor, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(0.5.dp, tint.copy(alpha = 0.2f))) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(insight.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun ExpandingFAB(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAddBudget: () -> Unit,
    onAddGoal: () -> Unit,
    onAddRecurring: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AnimatedVisibility(visible = isExpanded, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFABItem("Budget", Icons.Rounded.PieChart, onAddBudget)
                SmallFABItem("Goal", Icons.Rounded.Flag, onAddGoal)
                SmallFABItem("Recurring", Icons.Rounded.EventRepeat, onAddRecurring)
            }
        }
        
        FloatingActionButton(
            onClick = { onExpandChange(!isExpanded) },
            containerColor = Color(0xFF7C3AED),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            val rotation by animateFloatAsState(if (isExpanded) 45f else 0f, label = "rotation")
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.rotate(rotation))
        }
    }
}

@Composable
fun SmallFABItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp)) {
            Text(label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
        Spacer(modifier = Modifier.width(12.dp))
        FloatingActionButton(
            onClick = onClick,
            containerColor = Color(0xFF1E1E1E),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetActionBottomSheet(
    budget: BudgetWithProgress,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.1f)) }
    ) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(budget.budget.categoryName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            ActionItem("Edit Budget", Icons.Rounded.Edit, onClick = { /* Edit */ })
            ActionItem("Add Expense", Icons.Rounded.Add, onClick = { /* Add */ })
            ActionItem("Reset Progress", Icons.Rounded.RestartAlt, onClick = { /* Reset */ })
            ActionItem("Delete", Icons.Rounded.Delete, tint = Color(0xFFEF4444), onClick = onDelete)
        }
    }
}

@Composable
fun ActionItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
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
