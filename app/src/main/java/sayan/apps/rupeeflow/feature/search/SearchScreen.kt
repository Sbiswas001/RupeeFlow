package sayan.apps.rupeeflow.feature.search

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import sayan.apps.rupeeflow.domain.model.*
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onMenuClick: () -> Unit,
    onNavigateToTransactionDetail: (Long) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isSearchBarFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val sheetState = rememberModalBottomSheetState()
    var showRichFilters by remember { mutableStateOf(false) }

    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (spokenText != null) {
                viewModel.onQueryChange(spokenText)
                viewModel.onSearchAction(spokenText)
            }
        }
    }

    val launchVoiceSearch = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to find transactions")
        }
        voiceSearchLauncher.launch(intent)
    }

    Scaffold(
        topBar = {
            SearchHeader(
                query = uiState.query,
                onQueryChange = viewModel::onQueryChange,
                onMenuClick = onMenuClick,
                onSearch = { 
                    viewModel.onSearchAction(it)
                    focusManager.clearFocus()
                },
                isFocused = isSearchBarFocused,
                onFocusChange = { isSearchBarFocused = it },
                focusRequester = focusRequester,
                onToggleFilters = { showRichFilters = true },
                activeFiltersCount = getActiveFiltersCount(uiState.filters),
                onVoiceSearch = launchVoiceSearch
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Quick Category Filters (Sticky below header)
            QuickCategoryFilters(
                activeCategory = uiState.activeCategory,
                onCategorySelected = viewModel::setCategory,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Active Filter Chips (Collapsible)
            ActiveFilterChips(
                filters = uiState.filters,
                onRemoveFilter = { viewModel.onFilterChange(it) },
                categories = uiState.categories,
                accounts = uiState.accounts
            )

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.query.isEmpty() && getActiveFiltersCount(uiState.filters) == 0) {
                    SearchSuggestionsContent(
                        recentSearches = uiState.recentSearches,
                        onSuggestionClick = { 
                            viewModel.onQueryChange(it)
                            viewModel.onSearchAction(it)
                            focusManager.clearFocus()
                        },
                        onDeleteRecent = viewModel::deleteRecentSearch,
                        onClearAllRecent = { viewModel.clearRecentSearches() }
                    )
                } else {
                    SearchResultsContent(
                        results = uiState.filteredResults,
                        query = uiState.query,
                        isSearching = uiState.isSearching,
                        onNavigateToTransactionDetail = onNavigateToTransactionDetail,
                        accounts = uiState.accounts
                    )
                }
            }
        }

        if (showRichFilters) {
            RichFiltersBottomSheet(
                filters = uiState.filters,
                categories = uiState.categories,
                accounts = uiState.accounts,
                onDismiss = { showRichFilters = false },
                onApply = { 
                    viewModel.onFilterChange(it)
                    showRichFilters = false
                },
                sheetState = sheetState
            )
        }
    }
}

@Composable
fun SearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit,
    onSearch: (String) -> Unit,
    isFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    onToggleFilters: () -> Unit,
    activeFiltersCount: Int,
    onVoiceSearch: () -> Unit
) {
    val searchBarHeight by animateDpAsState(if (isFocused) 64.dp else 56.dp, label = "searchBarHeight")

    Surface(
        color = Color.Black,
        modifier = Modifier.statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu", tint = Color.White)
            }
            
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(searchBarHeight)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) },
                placeholder = { Text("Find transactions", color = Color.Gray, fontSize = 14.sp) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1F2937),
                    unfocusedContainerColor = Color(0xFF1F2937),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(28.dp),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.Gray)
                            }
                        } else {
                            IconButton(onClick = onVoiceSearch) {
                                Icon(Icons.Rounded.Mic, contentDescription = "Voice Search", tint = Color.Gray)
                            }
                        }
                        
                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp), color = Color(0xFF374151))
                        
                        Box(contentAlignment = Alignment.TopEnd) {
                            IconButton(onClick = onToggleFilters) {
                                Icon(Icons.Rounded.FilterList, contentDescription = "Filters", tint = if (activeFiltersCount > 0) Color(0xFF7C3AED) else Color.White)
                            }
                            if (activeFiltersCount > 0) {
                                Surface(
                                    color = Color(0xFF7C3AED),
                                    shape = CircleShape,
                                    modifier = Modifier.size(16.dp).offset(x = (-4).dp, y = 4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(text = activeFiltersCount.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch(query) })
            )
        }
    }
}

@Composable
fun QuickCategoryFilters(
    activeCategory: SearchCategory,
    onCategorySelected: (SearchCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(SearchCategory.entries) { category ->
            FilterChip(
                selected = activeCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category.name.lowercase().capitalize()) },
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

@Composable
fun ActiveFilterChips(
    filters: SearchFilters,
    onRemoveFilter: (SearchFilters) -> Unit,
    categories: List<Category>,
    accounts: List<Account>
) {
    val preferences = LocalUserPreferences.current
    val activeChips = mutableListOf<Pair<String, () -> Unit>>()
    
    if (filters.categoryId != null) {
        val name = categories.find { it.id == filters.categoryId }?.name ?: "Category"
        activeChips.add(name to { onRemoveFilter(filters.copy(categoryId = null)) })
    }
    if (filters.accountId != null) {
        val name = accounts.find { it.id == filters.accountId }?.name ?: "Account"
        activeChips.add(name to { onRemoveFilter(filters.copy(accountId = null)) })
    }
    if (filters.minAmount != null || filters.maxAmount != null) {
        val symbol = CurrencyFormatter.getSymbol(preferences)
        val label = when {
            filters.minAmount != null && filters.maxAmount != null -> "${symbol}${filters.minAmount} - ${symbol}${filters.maxAmount}"
            filters.minAmount != null -> "> ${symbol}${filters.minAmount}"
            else -> "< ${symbol}${filters.maxAmount}"
        }
        activeChips.add(label to { onRemoveFilter(filters.copy(minAmount = null, maxAmount = null)) })
    }
    if (filters.type != null) {
        activeChips.add(filters.type.name.lowercase().capitalize() to { onRemoveFilter(filters.copy(type = null)) })
    }

    if (activeChips.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(activeChips) { chip ->
                InputChip(
                    selected = true,
                    onClick = chip.second,
                    label = { Text(chip.first) },
                    trailingIcon = { Icon(Icons.Rounded.Close, null, modifier = Modifier.size(16.dp)) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFC084FC)
                    )
                )
            }
        }
    }
}

@Composable
fun SearchSuggestionsContent(
    recentSearches: List<String>,
    onSuggestionClick: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearAllRecent: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        if (recentSearches.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recent Searches", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    Text(
                        "Clear All",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF7C3AED),
                        modifier = Modifier.clickable { onClearAllRecent() }
                    )
                }
            }
            items(recentSearches) { query ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onSuggestionClick(query) }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.History, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(query, color = Color.White, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteRecent(query) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            SuggestionSection("Time", listOf("Today", "This Week", "This Month"), onSuggestionClick)
        }
        item {
            SuggestionSection("Type", listOf("Income", "Expense", "Recurring"), onSuggestionClick)
        }
        item {
            SuggestionSection("Popular", listOf("Blinkit", "Netflix", "Food", "SBI", "Rent", "Salary"), onSuggestionClick)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestionSection(title: String, suggestions: List<String>, onClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = Color.DarkGray)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestions.forEach { label ->
                SuggestionChip(
                    onClick = { onClick(label) },
                    label = { Text(label) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF1F2937),
                        labelColor = Color.White
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun SearchResultsContent(
    results: List<SearchResult>,
    query: String,
    isSearching: Boolean,
    onNavigateToTransactionDetail: (Long) -> Unit,
    accounts: List<Account>
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (isSearching) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF7C3AED))
        }

        if (results.isEmpty() && !isSearching) {
            EmptySearchState(query)
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${results.size} ${if (results.size == 1) "Result" else "Results"}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val grouped = results.groupBy { it::class.simpleName }
                
                groupResults(grouped, "TransactionResult", "Transactions", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "AccountResult", "Accounts", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "RecurringResult", "Recurring", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "CategoryResult", "Categories", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "MerchantResult", "Merchants", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "BudgetResult", "Budgets", query, onNavigateToTransactionDetail, accounts)
                groupResults(grouped, "GoalResult", "Goals", query, onNavigateToTransactionDetail, accounts)
            }
        }
    }
}

fun LazyListScope.groupResults(
    grouped: Map<String?, List<SearchResult>>,
    key: String,
    title: String,
    query: String,
    onNavigateToTransactionDetail: (Long) -> Unit,
    accounts: List<Account>
) {
    val list = grouped[key] ?: return
    item {
        Surface(
            color = Color(0xFF1F2937).copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Text(
                text = "$title (${list.size})",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFFC084FC),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
    items(list) { result ->
        SearchResultItem(
            result = result,
            query = query,
            accounts = accounts,
            onClick = {
                if (result is SearchResult.TransactionResult) {
                    onNavigateToTransactionDetail(result.transaction.id.toLong())
                }
            }
        )
    }
}

@Composable
fun SearchResultItem(
    result: SearchResult,
    query: String,
    accounts: List<Account>,
    onClick: () -> Unit
) {
    val preferences = LocalUserPreferences.current
    when (result) {
        is SearchResult.TransactionResult -> {
            RichTransactionItem(
                transaction = result.transaction, 
                query = query, 
                accountName = accounts.find { it.id == result.transaction.accountId }?.name ?: "Account",
                onClick = onClick
            )
        }
        is SearchResult.AccountResult -> {
            GenericResultItem(
                title = result.account.name,
                subtitle = "${result.account.institutionName ?: ""} • ${result.account.subType}",
                icon = Icons.Rounded.AccountBalance,
                iconColor = Color(0xFF3B82F6),
                trailing = CurrencyFormatter.format(result.account.balance, preferences),
                bgColor = Color(0xFF1C2435),
                query = query,
                onClick = onClick
            )
        }
        is SearchResult.CategoryResult -> {
            GenericResultItem(
                title = result.category.name,
                subtitle = "Category",
                icon = Icons.Rounded.Category,
                iconColor = Color(result.category.colorHex.removePrefix("#").toLong(16) or 0xFF000000),
                bgColor = Color(0xFF202020),
                query = query,
                onClick = onClick
            )
        }
        is SearchResult.MerchantResult -> {
            GenericResultItem(
                title = result.name,
                subtitle = "Merchant",
                icon = Icons.Rounded.Store,
                iconColor = Color(0xFF10B981),
                bgColor = Color(0xFF151B1F),
                query = query,
                onClick = onClick
            )
        }
        is SearchResult.RecurringResult -> {
            GenericResultItem(
                title = result.item.title,
                subtitle = "Recurring • ${result.item.frequency}",
                icon = Icons.Rounded.CalendarToday,
                iconColor = Color(0xFFF59E0B),
                trailing = CurrencyFormatter.format(result.item.amount, preferences),
                bgColor = Color(0xFF2A2417),
                query = query,
                onClick = onClick
            )
        }
        is SearchResult.BudgetResult -> {
            GenericResultItem(
                title = "${result.categoryName} Budget",
                subtitle = "Limit: ${CurrencyFormatter.format(result.budget.limitAmount, preferences)}",
                icon = Icons.Rounded.PieChart,
                iconColor = Color(0xFFEC4899),
                bgColor = Color(0xFF202020),
                query = query,
                onClick = onClick
            )
        }
        is SearchResult.GoalResult -> {
            GenericResultItem(
                title = result.goal.title,
                subtitle = "Target: ${CurrencyFormatter.format(result.goal.targetAmount, preferences)}",
                icon = Icons.Rounded.Flag,
                iconColor = Color(0xFF8B5CF6),
                trailing = "${((result.goal.currentAmount / result.goal.targetAmount) * 100).toInt()}%",
                bgColor = Color(0xFF1F2025),
                query = query,
                onClick = onClick
            )
        }
    }
}

@Composable
fun RichTransactionItem(
    transaction: Transaction,
    query: String,
    accountName: String,
    onClick: () -> Unit
) {
    val preferences = LocalUserPreferences.current
    val date = remember(transaction.timestamp) {
        SimpleDateFormat("d MMM • h:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }

    Surface(
        onClick = onClick,
        color = Color(0xFF151515),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1F2937)),
                contentAlignment = Alignment.Center
            ) {
                val icon = getMerchantIcon(transaction.title)
                if (icon != null) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(transaction.title.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(transaction.title, query, MaterialTheme.typography.bodyLarge, Color.White, FontWeight.SemiBold)
                Text(
                    text = "${transaction.category} • $accountName",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Text(date, style = MaterialTheme.typography.labelSmall, color = Color.DarkGray)
            }
            
            Text(
                text = (if (transaction.isIncome) "+ " else "- ") + CurrencyFormatter.format(transaction.amount, preferences),
                style = MaterialTheme.typography.bodyLarge,
                color = if (transaction.isIncome) Color(0xFF10B981) else Color(0xFFEF4444),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getMerchantIcon(title: String): ImageVector? {
    val t = title.lowercase()
    return when {
        t.contains("blinkit") -> Icons.Rounded.ShoppingCart
        t.contains("netflix") -> Icons.Rounded.PlayCircle
        t.contains("amazon") -> Icons.Rounded.ShoppingBag
        t.contains("uber") || t.contains("ola") -> Icons.Rounded.DirectionsCar
        t.contains("swiggy") || t.contains("zomato") -> Icons.Rounded.Restaurant
        t.contains("rent") || t.contains("housing") -> Icons.Rounded.Home
        t.contains("sbi") || t.contains("bank") || t.contains("hdfc") -> Icons.Rounded.AccountBalance
        else -> null
    }
}

@Composable
fun GenericResultItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    trailing: String? = null,
    bgColor: Color,
    query: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = bgColor,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                HighlightedText(title, query, MaterialTheme.typography.bodyLarge, Color.White, FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            
            if (trailing != null) {
                Text(trailing, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HighlightedText(
    text: String,
    query: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight
) {
    if (query.isEmpty() || !text.contains(query, ignoreCase = true)) {
        Text(text, style = style, color = color, fontWeight = fontWeight)
        return
    }

    val annotatedString = buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        var start = 0
        while (start < text.length) {
            val index = lowerText.indexOf(lowerQuery, start)
            if (index == -1) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, index))
            withStyle(SpanStyle(
                background = Color(0xFF7C3AED).copy(alpha = 0.4f), 
                fontWeight = FontWeight.ExtraBold,
            )) {
                append(text.substring(index, index + query.length))
            }
            start = index + query.length
        }
    }
    Text(text = annotatedString, style = style, color = color)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RichFiltersBottomSheet(
    filters: SearchFilters,
    categories: List<Category>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onApply: (SearchFilters) -> Unit,
    sheetState: SheetState
) {
    var tempFilters by remember { mutableStateOf(filters) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF111827),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { tempFilters = SearchFilters() }) {
                    Text("Reset", color = Color.Gray)
                }
            }

            // Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transaction Type", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TransactionType.entries.forEach { type ->
                        FilterChip(
                            selected = tempFilters.type == type,
                            onClick = { tempFilters = tempFilters.copy(type = if (tempFilters.type == type) null else type) },
                            label = { Text(type.name.lowercase().capitalize()) }
                        )
                    }
                }
            }

            // Categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Category", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = tempFilters.categoryId == category.id,
                            onClick = { tempFilters = tempFilters.copy(categoryId = if (tempFilters.categoryId == category.id) null else category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
            }

            // Accounts
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    accounts.forEach { account ->
                        FilterChip(
                            selected = tempFilters.accountId == account.id,
                            onClick = { tempFilters = tempFilters.copy(accountId = if (tempFilters.accountId == account.id) null else account.id) },
                            label = { Text(account.name) }
                        )
                    }
                }
            }

            Button(
                onClick = { onApply(tempFilters) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Apply Filters", modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.SearchOff, null, tint = Color.DarkGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No matches found", color = Color.White, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Try another keyword or remove some filters.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

fun getActiveFiltersCount(filters: SearchFilters): Int {
    var count = 0
    if (filters.categoryId != null) count++
    if (filters.accountId != null) count++
    if (filters.minAmount != null || filters.maxAmount != null) count++
    if (filters.startDate != null || filters.endDate != null) count++
    if (filters.type != null) count++
    if (filters.isRecurring != null) count++
    return count
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
