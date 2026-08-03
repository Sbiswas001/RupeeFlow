package sayan.apps.rupeeflow.feature.recurring

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
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.model.RecurringItem
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
    val preferences = LocalUserPreferences.current
    val items by viewModel.recurringItems.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val categories = listOf("All", "Bills", "Subscriptions", "EMIs", "Credit Cards", "Insurance", "SIP")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Recurring")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = "Recurring",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                val totalDue = items.filter { it.status == "PENDING" }.sumOf { it.amount }
                Text(
                    text = "${CurrencyFormatter.format(totalDue, preferences)} Due This Month",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    SummaryStat(label = "Active", value = items.size.toString())
                    Spacer(modifier = Modifier.width(48.dp))
                    SummaryStat(label = "Due Today", value = items.count { isToday(it.dueDate) }.toString())
                    Spacer(modifier = Modifier.width(48.dp))
                    SummaryStat(label = "Overdue", value = items.count { it.status == "OVERDUE" }.toString())
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Active", "Due Today", "This Week", "This Month", "Paid", "Overdue").forEach { label ->
                    FilterChip(
                        selected = false,
                        onClick = { /* Filter */ },
                        label = { Text(label) },
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            borderWidth = 1.dp
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            labelColor = MaterialTheme.colorScheme.onBackground
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ScrollableTabRow(
                selectedTabIndex = categories.indexOf(selectedCategory),
                containerColor = Color.Transparent,
                divider = {},
                edgePadding = 24.dp,
                indicator = { tabPositions ->
                    if (categories.indexOf(selectedCategory) < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[categories.indexOf(selectedCategory)]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                categories.forEach { category ->
                    Tab(
                        selected = selectedCategory == category,
                        onClick = { viewModel.onCategoryChange(category) },
                        text = { 
                            Text(
                                text = category, 
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (selectedCategory == category) FontWeight.Bold else FontWeight.Normal
                                ),
                                maxLines = 1
                            ) 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No recurring payments", style = MaterialTheme.typography.bodyLarge)
                        Button(onClick = { viewModel.addSampleRecurringItems() }) {
                            Text("Add Sample Data")
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(items) { item ->
                        RecurringItemCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onPay = { viewModel.markAsPaid(item) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun SummaryStat(label: String, value: String) {
    Column {
        Text(
            text = value, 
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = label, 
            style = MaterialTheme.typography.labelLarge, 
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun isToday(timestamp: Long): Boolean {
    val cal1 = Calendar.getInstance()
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
