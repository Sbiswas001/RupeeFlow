package sayan.apps.rupeeflow.feature.categories

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.TransactionType
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats by viewModel.stats.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedCategoryForEdit by remember { mutableStateOf<Category?>(null) }

    if (showAddSheet) {
        AddEditCategoryBottomSheet(
            category = selectedCategoryForEdit,
            onDismiss = { showAddSheet = false },
            onConfirm = { 
                if (selectedCategoryForEdit == null) {
                    viewModel.addCategory(it)
                } else {
                    viewModel.updateCategory(it)
                }
                showAddSheet = false 
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedCategoryForEdit = null
                    showAddSheet = true 
                },
                containerColor = Color(0xFF7C3AED),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Category")
            }
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                modifier = Modifier.padding(16.dp)
            )

            // Overview Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatCard(
                    title = "Expense",
                    value = stats.expenseCount.toString(),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFEF4444)
                )
                CompactStatCard(
                    title = "Income",
                    value = stats.incomeCount.toString(),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF10B981)
                )
                CompactStatCard(
                    title = "Total",
                    value = stats.totalCount.toString(),
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF6366F1)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Chips
            FilterChipsRow(
                selectedFilter = uiState.filterType,
                onFilterChange = viewModel::onFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Category List
            val groupedCategories = uiState.categories.groupBy { it.type }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedCategories.forEach { (type, categories) ->
                    item {
                        Text(
                            text = if (type == TransactionType.EXPENSE) "Expense" else "Income",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Gray
                        )
                    }
                    items(categories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            statsFlow = viewModel.getCategoryStatsFlow(category.id),
                            onClick = { onNavigateToDetail(category.id) },
                            onLongClick = {
                                selectedCategoryForEdit = category
                                showAddSheet = true
                            }
                        )
                    }
                }
                
                if (uiState.categories.isEmpty()) {
                    item {
                        EmptyCategoriesState()
                    }
                }
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        placeholder = { Text("Search categories...", color = Color.Gray) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1F2937),
            unfocusedContainerColor = Color(0xFF1F2937),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun CompactStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF111827),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

@Composable
fun FilterChipsRow(
    selectedFilter: CategoryFilter,
    onFilterChange: (CategoryFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CategoryFilter.entries.take(3).forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterChange(filter) },
                label = { Text(filter.name.lowercase().capitalize()) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedLabelColor = Color.White,
                    containerColor = Color(0xFF1F2937),
                    labelColor = Color.Gray
                ),
                border = null
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryItem(
    category: Category,
    statsFlow: Flow<Pair<Int, Double>>,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val stats by statsFlow.collectAsState(initial = Pair(0, 0.0))
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(android.graphics.Color.parseColor(category.colorHex)).copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(category.icon, fontSize = 24.sp)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White
                )
                Text(
                    text = "${stats.first} Transactions",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${String.format("%,.0f", stats.second)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = if (category.type == TransactionType.EXPENSE) "spent" else "earned",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun EmptyCategoriesState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.FolderOpen,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No custom categories yet.",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )
        Text(
            "Create one to organize your transactions.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
