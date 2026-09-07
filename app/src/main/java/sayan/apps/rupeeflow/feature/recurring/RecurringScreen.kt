package sayan.apps.rupeeflow.feature.recurring

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.feature.recurring.components.RecurringItemCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    modifier: Modifier = Modifier,
    viewModel: RecurringViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit = {},
    onNavigateToDetail: (Long) -> Unit = {}
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Recurring")
            }
        },
        containerColor = Color.Black
    ) { innerPadding ->
        RecurringTabContent(
            modifier = Modifier.padding(innerPadding),
            onNavigateToDetail = onNavigateToDetail,
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringTabContent(
    modifier: Modifier = Modifier,
    onNavigateToDetail: (Long) -> Unit = {},
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val preferences = LocalUserPreferences.current
    val items by viewModel.recurringItems.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val availableCategories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    val categories = remember(availableCategories) {
        listOf("All") + availableCategories.filter { it.type == TransactionType.EXPENSE && !it.isDeleted }.map { it.name }
    }

    LaunchedEffect(categories) {
        if (selectedCategory != "All" && !categories.contains(selectedCategory)) {
            viewModel.onCategoryChange("All")
        }
    }

    var occurrenceToPay by remember { mutableStateOf<RecurringOccurrence?>(null) }
    var showAccountSelection by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val totalDue = items.sumOf { item ->
                item.occurrences.filter { it.status == "PENDING" && it.scheduledDate <= getEndOfMonth(System.currentTimeMillis()) }.sumOf { item.amount }
            }
            
            val totalAnnual = items.filter { it.status == "ACTIVE" }.sumOf { item ->
                val interval = if (item.frequencyInterval > 0) item.frequencyInterval else 1
                val unit = item.frequencyUnit.uppercase()
                val amount = item.amount
                when (unit) {
                    "DAYS" -> (amount / interval) * 365.25
                    "WEEKS" -> (amount / interval) * 52.17
                    "MONTHS" -> (amount / interval) * 12.0
                    "YEARS" -> amount / interval
                    else -> {
                        when (item.frequency.uppercase()) {
                            "MONTHLY" -> amount * 12
                            "YEARLY" -> amount
                            "WEEKLY" -> amount * 52.17
                            "DAILY" -> amount * 365.25
                            else -> amount
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = CurrencyFormatter.format(totalDue, preferences),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = Color.White
            )
            
            Text(
                text = "Due this month",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp
                ),
                color = Color(0xFF9CA3AF)
            )

            Spacer(modifier = Modifier.height(12.dp))

            val activeCount = items.count { it.status == "ACTIVE" }
            val dueTodayCount = items.sumOf { item -> 
                item.occurrences.count { it.status == "PENDING" && isToday(it.scheduledDate) } 
            }
            val overdueCount = items.sumOf { item ->
                item.occurrences.count { it.status == "PENDING" && it.scheduledDate < getStartOfToday() }
            }
            val annualStr = formatAnnualCompact(totalAnnual, preferences)

            Text(
                text = "$activeCount Active  ·  $dueTodayCount Due  ·  $overdueCount Overdue  ·  $annualStr",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF9CA3AF)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Horizontal Scrollable Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                Surface(
                    modifier = Modifier
                        .clickable { viewModel.onCategoryChange(category) }
                        .height(36.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) Color(0xFF7C3AED) else Color(0xFF1F2937)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            ),
                            color = if (isSelected) Color.White else Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No recurring payments", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF9CA3AF))
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 100.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items, key = { it.id }) { item ->
                    RecurringItemCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.id) },
                        onPay = { occurrence ->
                            occurrenceToPay = occurrence
                            val defaultAccountId = item.accountId
                            if (defaultAccountId != null && accounts.any { it.id == defaultAccountId }) {
                                viewModel.markAsPaid(occurrence.id, defaultAccountId)
                                occurrenceToPay = null
                            } else {
                                showAccountSelection = true
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showAccountSelection && occurrenceToPay != null) {
        AccountSelectionDialog(
            accounts = accounts,
            onDismiss = { 
                showAccountSelection = false
                occurrenceToPay = null
            },
            onAccountSelected = { account ->
                viewModel.markAsPaid(occurrenceToPay!!.id, account.id)
                showAccountSelection = false
                occurrenceToPay = null
            }
        )
    }
}

@Composable
fun AccountSelectionDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onAccountSelected: (Account) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181818),
        title = { Text("Select Payment Account", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (accounts.isEmpty()) {
                    Text("No accounts found. Please add an account first.", color = Color.Gray)
                }
                accounts.forEach { account ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAccountSelected(account) },
                        color = Color(0xFF1F2937),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(account.name, color = Color.White)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatAnnualCompact(amount: Double, preferences: sayan.apps.rupeeflow.domain.model.UserPreferences): String {
    val symbol = CurrencyFormatter.getSymbol(preferences)
    val formattedNumber = when {
        amount >= 10_000_000 -> String.format(Locale.US, "%.1fCr", amount / 10_000_000)
        amount >= 100_000 -> String.format(Locale.US, "%.1fL", amount / 100_000)
        amount >= 1_000 -> String.format(Locale.US, "%.1fK", amount / 1_000)
        else -> String.format(Locale.US, "%.0f", amount)
    }
    return "$symbol$formattedNumber/year"
}

fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

fun getStartOfToday(): Long {
    return Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

fun getEndOfMonth(timestamp: Long): Long {
    return Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
}
