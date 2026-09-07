package sayan.apps.rupeeflow

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.google.android.gms.auth.api.identity.Identity
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.designsystem.theme.RupeeFlowTheme
import sayan.apps.rupeeflow.core.navigation.*
import sayan.apps.rupeeflow.core.security.AppLockManager
import sayan.apps.rupeeflow.core.security.AppLockState
import sayan.apps.rupeeflow.core.util.LocalUserPreferences
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import sayan.apps.rupeeflow.feature.about.AboutScreen
import sayan.apps.rupeeflow.feature.accounts.AccountScreen
import sayan.apps.rupeeflow.feature.accounts.NetWorthScreen
import sayan.apps.rupeeflow.feature.analytics.AllCategoryBreakdownScreen
import sayan.apps.rupeeflow.feature.categories.CategoriesScreen
import sayan.apps.rupeeflow.feature.categories.CategoryDetailScreen
import sayan.apps.rupeeflow.feature.dashboard.DashboardScreen
import sayan.apps.rupeeflow.feature.insights.InsightsScreen
import sayan.apps.rupeeflow.feature.recurring.AddRecurringScreen
import sayan.apps.rupeeflow.feature.recurring.RecurringDetailScreen
import sayan.apps.rupeeflow.feature.search.SearchScreen
import sayan.apps.rupeeflow.feature.security.AppLockScreen
import sayan.apps.rupeeflow.feature.settings.BackupRestoreScreen
import sayan.apps.rupeeflow.feature.settings.NotificationsScreen
import sayan.apps.rupeeflow.feature.settings.SettingsScreen
import sayan.apps.rupeeflow.feature.settings.SettingsViewModel
import sayan.apps.rupeeflow.feature.settings.UserPreferencesUiState
import sayan.apps.rupeeflow.feature.transactions.ActivityPagerScreen
import sayan.apps.rupeeflow.feature.transactions.AddTransactionScreen
import sayan.apps.rupeeflow.feature.transactions.TransactionDetailScreen
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> appLockManager.onAppForegrounded()
                Lifecycle.Event.ON_PAUSE -> appLockManager.onAppPaused()
                Lifecycle.Event.ON_STOP -> appLockManager.onAppBackgrounded()
                else -> {}
            }
        })

        enableEdgeToEdge()
        setContent {
            val lockState by appLockManager.lockState.collectAsStateWithLifecycle()
            val preferences by userPreferencesRepository.userPreferences.collectAsStateWithLifecycle(initialValue = null)
            
            // Apply FLAG_SECURE if locked or screenshot protection is enabled
            LaunchedEffect(lockState, preferences?.screenshotProtection) {
                if (lockState != AppLockState.UNLOCKED || preferences?.screenshotProtection == true) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            RupeeFlowTheme(
                amoledBlack = preferences?.amoledBlack ?: false,
                dynamicColor = preferences?.dynamicColor ?: false
            ) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (lockState) {
                            AppLockState.UNLOCKED -> {
                                MainApp()
                            }
                            AppLockState.INITIALIZING -> {
                                // Show neutral black background (handled by Surface)
                            }
                            AppLockState.LOCKED, AppLockState.AUTHENTICATING, AppLockState.COOLDOWN -> {
                                AppLockScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRestoreLoading by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var isRestoreSuccess by remember { mutableStateOf(false) }

    val isGoogleSignedIn by viewModel.isGoogleSignedIn.collectAsStateWithLifecycle()

    // To store what action to take after authorization
    var pendingDriveAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val authorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                scope.launch {
                    try {
                        val authResult = Identity.getAuthorizationClient(context)
                            .getAuthorizationResultFromIntent(result.data)
                        val token = authResult.accessToken
                        if (token != null) {
                            pendingDriveAction?.invoke()
                        } else {
                            restoreMessage = "Failed to get access token"
                        }
                    } catch (e: Exception) {
                        restoreMessage = "Authorization failed: ${e.message}"
                    } finally {
                        pendingDriveAction = null
                    }
                }
            } else {
                pendingDriveAction = null
            }
        }
    )

    fun performWithDriveAccess(action: (String) -> Unit) {
        val requestedScopes = listOf(DriveScopes.DRIVE_APPDATA)
        val authClient = Identity.getAuthorizationClient(context)
        val authRequest = viewModel.authorizationHelper.getAuthorizationRequest(requestedScopes)
        
        authClient.authorize(authRequest)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    pendingDriveAction = {
                        performWithDriveAccess(action)
                    }
                    val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent!!.intentSender).build()
                    authorizationLauncher.launch(intentSenderRequest)
                } else {
                    val token = result.accessToken
                    if (token != null) {
                        viewModel.fetchDriveBackupInfo(token)
                        action(token)
                    } else {
                        restoreMessage = "Failed to get access token"
                    }
                }
            }
            .addOnFailureListener { e ->
                restoreMessage = "Drive access failed: ${e.message}"
            }
    }

    when (val state = uiState) {
        UserPreferencesUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UserPreferencesUiState.Success -> {
            val preferences = state.preferences
            
            if (preferences.isFirstRun || isRestoreSuccess) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss by tapping outside */ },
                    title = { Text(if (isRestoreSuccess) "Restore Complete" else "Welcome to RupeeFlow") },
                    text = {
                        Column {
                            if (!isRestoreSuccess) {
                                Text("Would you like to restore your data from Google Drive or start fresh?")
                            }
                            if (restoreMessage != null) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(restoreMessage!!, color = if (isRestoreSuccess) Color(0xFF10B981) else Color.Red)
                            }
                            if (isRestoreLoading) {
                                Spacer(modifier = Modifier.height(16.dp))
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                            }
                        }
                    },
                    confirmButton = {
                        if (isRestoreSuccess) {
                            Button(
                                onClick = { 
                                    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                                    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
                                    context.startActivity(mainIntent)
                                    Runtime.getRuntime().exit(0)
                                }
                            ) {
                                Text("Restart App")
                            }
                        } else {
                            Button(
                                onClick = { 
                                    scope.launch {
                                        isRestoreLoading = true
                                        if (isGoogleSignedIn) {
                                            performWithDriveAccess { token ->
                                                viewModel.restoreFromGoogleDrive(
                                                    accessToken = token,
                                                    onSuccess = {
                                                        isRestoreLoading = false
                                                        isRestoreSuccess = true
                                                        restoreMessage = "Restore successful. The app needs to restart to apply changes."
                                                        viewModel.updateFirstRun(false)
                                                    },
                                                    onError = { message ->
                                                        isRestoreLoading = false
                                                        restoreMessage = "Restore failed: $message"
                                                    }
                                                )
                                            }
                                        } else {
                                            val credential = viewModel.authHelper.signInWithGoogle(context)
                                            if (credential != null) {
                                                viewModel.setGoogleSignedIn(true, credential.id)
                                                performWithDriveAccess { token ->
                                                    viewModel.restoreFromGoogleDrive(
                                                        accessToken = token,
                                                        onSuccess = {
                                                            isRestoreLoading = false
                                                            isRestoreSuccess = true
                                                            restoreMessage = "Restore successful. The app needs to restart to apply changes."
                                                            viewModel.updateFirstRun(false)
                                                        },
                                                        onError = { message ->
                                                            isRestoreLoading = false
                                                            restoreMessage = "Restore failed: $message"
                                                        }
                                                    )
                                                }
                                            } else {
                                                isRestoreLoading = false
                                                restoreMessage = "Sign in failed"
                                            }
                                        }
                                    }
                                },
                                enabled = !isRestoreLoading
                            ) {
                                Text("Restore from Drive")
                            }
                        }
                    },
                    dismissButton = {
                        if (!isRestoreSuccess) {
                            TextButton(
                                onClick = { viewModel.updateFirstRun(false) },
                                enabled = !isRestoreLoading
                            ) {
                                Text("Start Fresh")
                            }
                        }
                    },
                    containerColor = Color(0xFF111827),
                    titleContentColor = Color.White,
                    textContentColor = Color.Gray
                )
            }

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
            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

            // Drawer items that use the global top bar (Hamburger menu)
            val showTopBar = currentRoute is Dashboard || 
                             currentRoute is Activity ||
                             currentRoute is Accounts || 
                             currentRoute is Insights || 
                             currentRoute is Categories
            val showBottomBar = currentRoute !is Search

            val navigateTo = { route: NavRoute ->
                if (currentRoute::class != route::class) {
                    // Keep history for all screens to allow back navigation between tabs/sidebar items
                    navigationStack.add(route)
                }
            }

            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = showBottomBar,
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
                                visible = showTopBar,
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
                                visible = showBottomBar,
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                            ) {
                                OneUIBottomNavigation(
                                    currentRoute = currentRoute,
                                    onNavigate = navigateTo
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.background
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
        is Dashboard, is Activity, is Accounts, is Insights -> "RupeeFlow"
        is NetWorth -> "Net Worth"
        is Categories -> "Categories"
        is BackupRestore -> "Backup & Restore"
        is Notifications -> "Notifications"
        is Settings -> "Settings"
        is About -> "About"
        is AddTransaction -> "Add Transaction"
        is EditTransaction -> "Edit Transaction"
        is TransactionDetail -> "Details"
        is RecurringDetail -> "Recurring Details"
        is AddRecurring -> "Add Recurring"
        is EditRecurring -> "Edit Recurring"
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
                    IconButton(onClick = { navigationStack.add(EditRecurring(currentRoute.id)) }) {
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
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
            actionIconContentColor = MaterialTheme.colorScheme.onBackground
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
                label = { Text("Activity") },
                selected = currentRoute is Activity,
                onClick = { onNavigate(Activity()) },
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
                onClick = { onNavigate(Insights()) },
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 28.dp), color = Color(0xFF1F2937))

            DrawerSecondaryItem("Backup & Restore", Icons.Rounded.Backup, currentRoute is BackupRestore) { onNavigate(BackupRestore) }
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
        NavigationItem("Activity", Icons.AutoMirrored.Rounded.ReceiptLong, Activity()),
        NavigationItem("Insights", Icons.Rounded.AutoGraph, Insights()),
        NavigationItem("Accounts", Icons.Rounded.AccountBalance, Accounts)
    )

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(80.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
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
                            interactionSource = remember { MutableInteractionSource() },
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
    scope: CoroutineScope
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
                    onNavigateToDetail = { id -> navigationStack.add(TransactionDetail(id)) },
                    onNavigateToUpcoming = { navigationStack.add(sayan.apps.rupeeflow.core.navigation.Activity(initialTab = 1)) },
                    onNavigateToEditRecurring = { id -> navigationStack.add(EditRecurring(id)) },
                    onNavigateToAccountDetails = { _ -> navigationStack.add(Accounts) },
                    onNavigateToInsightsTab = { tab -> navigationStack.add(Insights(initialTab = tab)) }
                )
            }
            entry<Activity> {
                ActivityPagerScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    initialTab = it.initialTab,
                    onNavigateToAddTransaction = { navigationStack.add(AddTransaction) },
                    onNavigateToAddRecurring = { navigationStack.add(AddRecurring) },
                    onNavigateToTransactionDetail = { id -> navigationStack.add(TransactionDetail(id)) },
                    onNavigateToRecurringDetail = { id -> navigationStack.add(RecurringDetail(id)) }
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
            entry<Insights> { key ->
                InsightsScreen(
                    initialTab = key.initialTab,
                    onNavigateToAddRecurring = { navigationStack.add(AddRecurring) },
                    onNavigateToViewAll = { navigationStack.add(AllCategoryBreakdown) },
                    onNavigateToCategoryDetail = { id -> navigationStack.add(CategoryDetail(id)) }
                )
            }
            entry<AllCategoryBreakdown> {
                AllCategoryBreakdownScreen(
                    onNavigateBack = navigateBack,
                    onNavigateToCategoryDetail = { id -> navigationStack.add(CategoryDetail(id)) }
                )
            }
            // Recurring is now part of Activity
            entry<AddRecurring> {
                AddRecurringScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<EditRecurring> {
                AddRecurringScreen(
                    itemId = it.id,
                    onNavigateBack = navigateBack
                )
            }
            entry<RecurringDetail> {
                RecurringDetailScreen(
                    itemId = it.id,
                    onNavigateBack = navigateBack,
                    onNavigateToEdit = { id -> navigationStack.add(EditRecurring(id)) }
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
            entry<BackupRestore> {
                BackupRestoreScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<Notifications> {
                NotificationsScreen(
                    onNavigateBack = navigateBack
                )
            }
            entry<Settings> {
                SettingsScreen(
                    onNavigateBack = navigateBack,
                    onNavigateToBackupRestore = { navigationStack.add(BackupRestore) }
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
