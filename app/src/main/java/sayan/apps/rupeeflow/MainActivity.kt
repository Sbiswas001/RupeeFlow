package sayan.apps.rupeeflow

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import kotlinx.coroutines.launch
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import dagger.hilt.android.AndroidEntryPoint
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.navigation.*
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.feature.about.AboutScreen
import sayan.apps.rupeeflow.feature.accounts.AccountScreen
import sayan.apps.rupeeflow.feature.accounts.NetWorthScreen
import sayan.apps.rupeeflow.feature.categories.CategoriesScreen
import sayan.apps.rupeeflow.feature.categories.CategoryDetailScreen
import sayan.apps.rupeeflow.feature.dashboard.DashboardScreen
import sayan.apps.rupeeflow.feature.insights.InsightsScreen
import sayan.apps.rupeeflow.feature.recurring.AddRecurringScreen
import sayan.apps.rupeeflow.feature.recurring.RecurringDetailScreen
import sayan.apps.rupeeflow.feature.recurring.RecurringScreen
import sayan.apps.rupeeflow.feature.search.SearchScreen
import sayan.apps.rupeeflow.feature.settings.SettingsScreen
import sayan.apps.rupeeflow.feature.settings.SettingsViewModel
import sayan.apps.rupeeflow.feature.transactions.AddTransactionScreen
import sayan.apps.rupeeflow.feature.transactions.TransactionDetailScreen
import sayan.apps.rupeeflow.feature.transactions.TransactionsScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RupeeFlowTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()

    // Permission for Android 13+ Notifications
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { _ -> }
        )
        LaunchedEffect(Unit) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val navigationStack = rememberNavBackStack(Dashboard)
    val currentRoute = navigationStack.lastOrNull() ?: Dashboard
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // We show the global navigation top bar and bottom bar on most screens
    val showNavigation = currentRoute !is Search

    val navigateTo = { route: NavRoute ->
        if (currentRoute::class != route::class) {
            // Keep history for all screens to allow back navigation between tabs/sidebar items
            navigationStack.add(route)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showNavigation,
        drawerContent = {
            RupeeFlowDrawerContent(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    navigateTo(route)
                }
            )
        }
    ) {
        CompositionLocalProvider(LocalUserPreferences provides preferences) {
            Scaffold(
                topBar = {
                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        RupeeFlowTopAppBar(
                            currentRoute = currentRoute,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onNavigate = navigateTo,
                            navigationStack = navigationStack
                        )
                    }
                },
                bottomBar = {
                    AnimatedVisibility(
                        visible = showNavigation,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        OneUIBottomNavigation(
                            currentRoute = currentRoute,
                            onNavigate = navigateTo
                        )
                    }
                },
                containerColor = Color.Black
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    AppContent(
                        navigationStack = navigationStack,
                        drawerState = drawerState,
                        scope = scope
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RupeeFlowTopAppBar(
    currentRoute: NavKey,
    onMenuClick: () -> Unit,
    onNavigate: (NavRoute) -> Unit,
    navigationStack: NavBackStack<NavKey>
) {
    val title = when (currentRoute) {
        is Dashboard -> "Dashboard"
        is Activity -> "Activity"
        is Accounts -> "Portfolio"
        is Insights -> "Insights"
        is Recurring -> "Recurring"
        is NetWorth -> "Net Worth"
        is Categories -> "Categories"
        is Merchants -> "Merchants"
        is Tags -> "Tags"
        is Archived -> "Archived"
        is BackupRestore -> "Backup & Restore"
        is ImportExport -> "Import & Export"
        is Notifications -> "Notifications"
        is Settings -> "Settings"
        is AiChat -> "RupeeFlow Assistant"
        is About -> "About"
        is AddTransaction -> "Add Transaction"
        is EditTransaction -> "Edit Transaction"
        is TransactionDetail -> "Details"
        is RecurringDetail -> "Recurring Details"
        is AddRecurring -> "Add Recurring"
        else -> "RupeeFlow"
    }

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            when (currentRoute) {
                is TransactionDetail -> {
                    IconButton(onClick = { navigationStack.add(EditTransaction(currentRoute.id)) }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
                is RecurringDetail -> {
                    IconButton(onClick = { /* navigate to edit if exists */ }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                    }
                }
                is Search -> {
                    // Search screen has its own bar logic
                }
                else -> {
                    IconButton(onClick = { onNavigate(Search) }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Black,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White
        )
    )
}

@Composable
fun RupeeFlowDrawerContent(
    currentRoute: NavKey,
    onNavigate: (NavRoute) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFF111827),
        drawerContentColor = Color.White,
        modifier = Modifier.width(320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "RupeeFlow",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 28.dp),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            NavigationDrawerItem(
                label = { Text("Dashboard") },
                selected = currentRoute is Dashboard,
                onClick = { onNavigate(Dashboard) },
                icon = { Icon(Icons.Rounded.Dashboard, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            NavigationDrawerItem(
                label = { Text("Transactions") },
                selected = currentRoute is Activity,
                onClick = { onNavigate(Activity) },
                icon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            NavigationDrawerItem(
                label = { Text("Insights") },
                selected = currentRoute is Insights,
                onClick = { onNavigate(Insights) },
                icon = { Icon(Icons.Rounded.AutoGraph, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            NavigationDrawerItem(
                label = { Text("RupeeFlow AI") },
                selected = currentRoute is AiChat,
                onClick = { onNavigate(AiChat) },
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            NavigationDrawerItem(
                label = { Text("Recurring") },
                selected = currentRoute is Recurring,
                onClick = { onNavigate(Recurring) },
                icon = { Icon(Icons.Rounded.CalendarToday, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            NavigationDrawerItem(
                label = { Text("Accounts") },
                selected = currentRoute is Accounts,
                onClick = { onNavigate(Accounts) },
                icon = { Icon(Icons.Rounded.AccountBalance, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = Color(0xFF7C3AED),
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedTextColor = Color(0xFF9CA3AF),
                    unselectedIconColor = Color(0xFF9CA3AF)
                )
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), color = Color(0xFF1F2937))

            DrawerSecondaryItem("Categories", Icons.Rounded.Category, currentRoute is Categories) { onNavigate(Categories) }
            DrawerSecondaryItem("Merchants", Icons.Rounded.Store, currentRoute is Merchants) { onNavigate(Merchants) }
            DrawerSecondaryItem("Tags", Icons.Rounded.Label, currentRoute is Tags) { onNavigate(Tags) }
            DrawerSecondaryItem("Archived", Icons.Rounded.Archive, currentRoute is Archived) { onNavigate(Archived) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), color = Color(0xFF1F2937))

            DrawerSecondaryItem("Backup & Restore", Icons.Rounded.Backup, currentRoute is BackupRestore) { onNavigate(BackupRestore) }
            DrawerSecondaryItem("Import & Export", Icons.Rounded.ImportExport, currentRoute is ImportExport) { onNavigate(ImportExport) }
            DrawerSecondaryItem("Notifications", Icons.Rounded.Notifications, currentRoute is Notifications) { onNavigate(Notifications) }
            DrawerSecondaryItem("Settings", Icons.Rounded.Settings, currentRoute is Settings) { onNavigate(Settings) }
            DrawerSecondaryItem("About", Icons.Rounded.Info, currentRoute is About) { onNavigate(About) }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DrawerSecondaryItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Color(0xFF7C3AED).copy(alpha = 0.2f),
            selectedTextColor = Color.White,
            selectedIconColor = Color.White,
            unselectedTextColor = Color(0xFF9CA3AF),
            unselectedIconColor = Color(0xFF9CA3AF)
        )
    )
}

@Composable
fun OneUIBottomNavigation(
    currentRoute: NavKey,
    onNavigate: (NavRoute) -> Unit
) {
    val items = listOf(
        NavigationItem("Dashboard", Icons.Rounded.Dashboard, Dashboard),
        NavigationItem("Activity", Icons.AutoMirrored.Rounded.ReceiptLong, Activity),
        NavigationItem("Insights", Icons.Rounded.AutoGraph, Insights),
        NavigationItem("Recurring", Icons.Rounded.CalendarToday, Recurring),
        NavigationItem("Accounts", Icons.Rounded.AccountBalance, Accounts)
    )

    Surface(
        color = Color(0xFF000000),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(80.dp),
        border = BorderStroke(0.5.dp, Color(0xFF1F2937))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute::class == item.route::class
                val pillWidth by animateDpAsState(if (isSelected) 64.dp else 0.dp, label = "pillWidth")
                val backgroundColor by animateColorAsState(if (isSelected) Color(0xFF7C3AED) else Color.Transparent, label = "bg")

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onNavigate(item.route) }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(32.dp).width(64.dp)
                    ) {
                        if (isSelected) {
                            Surface(
                                color = backgroundColor,
                                shape = CapsuleShape,
                                modifier = Modifier.width(pillWidth).fillMaxHeight()
                            ) {}
                        }
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (isSelected) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    } else {
                        // Invisible text to maintain layout stability
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Transparent
                        )
                    }
                }
            }
        }
    }
}

val CapsuleShape = RoundedCornerShape(100.dp)

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val route: NavRoute
)

@Composable
fun AppContent(
    navigationStack: NavBackStack<NavKey>,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val navigateBack = {
        if (navigationStack.size > 1) {
            navigationStack.removeLastOrNull()
        } else if (navigationStack.lastOrNull() !is Dashboard) {
            navigationStack.clear()
            navigationStack.add(Dashboard)
        }
        Unit
    }

    NavDisplay(
        backStack = navigationStack,
        onBack = navigateBack,
        modifier = Modifier.fillMaxSize(),
        entryProvider = entryProvider {
            entry<Dashboard> {
                DashboardScreen(
                    onNavigateToAdd = { navigationStack.add(AddTransaction) },
                    onNavigateToDetail = { id -> navigationStack.add(TransactionDetail(id)) }
                )
            }
            entry<Activity> {
                TransactionsScreen(
                    onNavigateToAdd = { navigationStack.add(AddTransaction) },
                    onNavigateToDetail = { id -> navigationStack.add(TransactionDetail(id)) }
                )
            }
            entry<AddTransaction> {
                AddTransactionScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<EditTransaction> {
                AddTransactionScreen(
                    transactionId = it.id,
                    onNavigateBack = navigateBack
                )
            }
            entry<TransactionDetail> {
                TransactionDetailScreen(
                    transactionId = it.id,
                    onNavigateBack = navigateBack,
                    onNavigateToEdit = { id -> navigationStack.add(EditTransaction(id)) }
                )
            }
            entry<Accounts> {
                AccountScreen(
                    onNavigateToNetWorth = { navigationStack.add(NetWorth) }
                )
            }
            entry<NetWorth> {
                NetWorthScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<Insights> {
                InsightsScreen(
                    onNavigateToAddRecurring = { navigationStack.add(AddRecurring) }
                )
            }
            entry<Recurring> {
                RecurringScreen(
                    onNavigateToAdd = { navigationStack.add(AddRecurring) },
                    onNavigateToDetail = { id -> navigationStack.add(RecurringDetail(id)) }
                )
            }
            entry<AddRecurring> {
                AddRecurringScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<RecurringDetail> {
                RecurringDetailScreen(
                    itemId = it.id,
                    onNavigateBack = navigateBack
                )
            }
            entry<Search> {
                SearchScreen(
                    onNavigateBack = navigateBack,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onNavigateToTransactionDetail = { id: Long -> navigationStack.add(TransactionDetail(id)) }
                )
            }
            entry<Categories> {
                CategoriesScreen(
                    onNavigateToDetail = { id -> navigationStack.add(CategoryDetail(id)) }
                )
            }
            entry<CategoryDetail> {
                CategoryDetailScreen(
                    categoryId = it.id,
                    onNavigateBack = navigateBack
                )
            }
            entry<Merchants> { PlaceholderScreen("Merchants") }
            entry<Tags> { PlaceholderScreen("Tags") }
            entry<Archived> { PlaceholderScreen("Archived") }
            entry<BackupRestore> { PlaceholderScreen("Backup & Restore") }
            entry<ImportExport> { PlaceholderScreen("Import & Export") }
            entry<Notifications> { PlaceholderScreen("Notifications") }
            entry<Settings> {
                SettingsScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<AiChat> {
                sayan.apps.rupeeflow.feature.ai.AiChatScreen(
                    onBackClick = navigateBack
                )
            }
            entry<AiDeveloper> {
                sayan.apps.rupeeflow.feature.ai.AiDeveloperScreen(
                    onBackClick = navigateBack
                )
            }
            entry<About> {
                AboutScreen(
                    onNavigateBack = navigateBack
                )
            }
        }
    )
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, color = Color.White, style = MaterialTheme.typography.headlineMedium)
    }
}
