package sayan.apps.rupeeflow.feature.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import sayan.apps.rupeeflow.core.designsystem.components.BudgetProgressBar
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryId: Long,
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    var category by remember { mutableStateOf<Category?>(null) }
    val transactions by viewModel.getTransactionsForCategory(categoryId).collectAsState(initial = emptyList<Transaction>())
    val stats by viewModel.getCategoryStatsFlow(categoryId).collectAsState(initial = Pair(0, 0.0))
    var showEditSheet by remember { mutableStateOf(false) }

    LaunchedEffect(categoryId) {
        category = viewModel.getCategoryById(categoryId)
    }

    if (showEditSheet) {
        AddEditCategoryBottomSheet(
            category = category,
            onDismiss = { showEditSheet = false },
            onConfirm = { 
                viewModel.updateCategory(it)
                category = it
                showEditSheet = false 
            },
            onDelete = {
                viewModel.deleteCategory(it)
                onNavigateBack()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(category?.name ?: "Category Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        category?.let { cat ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    CategoryHeader(cat, stats)
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                items(transactions) { transaction ->
                    TransactionItemSimple(transaction)
                    HorizontalDivider(color = Color(0xFF1F2937), modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF7C3AED))
        }
    }
}

@Composable
fun CategoryHeader(category: Category, stats: Pair<Int, Double>) {
    Surface(
        color = Color(0xFF111827),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 40.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                category.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Transactions", stats.first.toString())
                StatItem(
                    if (category.type == TransactionType.EXPENSE) "Spent" else "Earned",
                    "₹${String.format("%,.0f", stats.second)}"
                )
            }
            
            category.budget?.let { b ->
                Spacer(modifier = Modifier.height(24.dp))
                BudgetProgress(spent = stats.second, budget = b)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

@Composable
fun BudgetProgress(spent: Double, budget: Double) {
    val progress = if (budget > 0) (spent / budget).toFloat() else 0f
    val remaining = budget - spent
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budget", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text("₹${String.format("%,.0f", budget)}", style = MaterialTheme.typography.labelMedium, color = Color.White)
        }
        Spacer(modifier = Modifier.height(8.dp))
        BudgetProgressBar(
            progress = progress,
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (remaining >= 0) "₹${String.format("%,.0f", remaining)} remaining" else "₹${String.format("%,.0f", -remaining)} over budget",
            style = MaterialTheme.typography.labelSmall,
            color = if (remaining >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
        )
    }
}

@Composable
fun TransactionItemSimple(transaction: Transaction) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(transaction.title, style = MaterialTheme.typography.bodyLarge, color = Color.White)
            Text(
                java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(transaction.timestamp)),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Text(
            text = "${if (transaction.isIncome) "+" else "-"}₹${String.format("%,.2f", transaction.amount)}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFFEF4444)
        )
    }
}
