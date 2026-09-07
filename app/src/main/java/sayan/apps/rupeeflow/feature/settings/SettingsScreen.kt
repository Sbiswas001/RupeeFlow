package sayan.apps.rupeeflow.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.LockTimeout
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showLockTimeoutDialog by remember { mutableStateOf(false) }
    
    var showAccountDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinVerifyDisableDialog by remember { mutableStateOf(false) }
    var showPinChangeDialog by remember { mutableStateOf(false) }
    var showResetAppDialog by remember { mutableStateOf(false) }
    
    var pinInput by remember { mutableStateOf("") }
    var pinConfirmInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    
    var versionTapCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 🌐 General
            SettingsSection(title = "GENERAL", icon = Icons.Rounded.Public) {
                PreferenceRow(
                    title = "Currency",
                    value = preferences.currency,
                    onClick = { showCurrencyDialog = true }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "First Day of Week",
                    value = if (preferences.firstDayOfWeek == Calendar.MONDAY) "Monday" else "Sunday",
                    onClick = { showFirstDayDialog = true }
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Financial Year",
                    subtitle = "Calendar Year / April to March",
                    checked = preferences.useFinancialYear,
                    onCheckedChange = viewModel::updateUseFinancialYear
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Indian Number Format",
                    subtitle = "Lakhs and Crores (e.g. 1,00,000)",
                    checked = preferences.indianNumberFormat,
                    onCheckedChange = viewModel::updateIndianNumberFormat
                )
            }

            // 💳 Transactions
            SettingsSection(title = "TRANSACTIONS", icon = Icons.Rounded.AccountBalanceWallet) {
                val selectedAccount = accounts.find { it.id == preferences.defaultAccountId }
                PreferenceRow(
                    title = "Default Account",
                    value = selectedAccount?.name ?: "None",
                    onClick = { showAccountDialog = true }
                )
                SettingsDivider()
                val selectedCategory = categories.find { it.id == preferences.defaultCategoryId }
                PreferenceRow(
                    title = "Default Category",
                    value = selectedCategory?.let { "${it.icon} ${it.name}" } ?: "None",
                    onClick = { showCategoryDialog = true }
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Confirm Before Delete",
                    checked = preferences.confirmBeforeDelete,
                    onCheckedChange = viewModel::updateConfirmBeforeDelete
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Auto Save Drafts",
                    checked = preferences.autoSaveDrafts,
                    onCheckedChange = viewModel::updateAutoSaveDrafts
                )
            }

            // 🎨 Appearance
            SettingsSection(title = "APPEARANCE", icon = Icons.Rounded.Palette) {
                TogglePreferenceRow(
                    title = "AMOLED Black",
                    subtitle = "Use pure black for OLED displays",
                    checked = preferences.amoledBlack,
                    onCheckedChange = viewModel::updateAmoledBlack
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Dynamic Color",
                    subtitle = "Use colors from your device",
                    checked = preferences.dynamicColor,
                    onCheckedChange = viewModel::updateDynamicColor
                )
            }

            // 🔔 Notifications
            SettingsSection(title = "NOTIFICATIONS", icon = Icons.Rounded.Notifications) {
                TogglePreferenceRow(
                    title = "Bill Reminders",
                    checked = preferences.billReminders,
                    onCheckedChange = viewModel::updateBillReminders
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Budget Alerts",
                    checked = preferences.budgetAlerts,
                    onCheckedChange = viewModel::updateBudgetAlerts
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Goal Reminders",
                    checked = preferences.goalReminders,
                    onCheckedChange = viewModel::updateGoalReminders
                )
            }

            // 🔒 Privacy & Security
            SettingsSection(title = "PRIVACY & SECURITY", icon = Icons.Rounded.Security) {
                TogglePreferenceRow(
                    title = "App Lock",
                    subtitle = if (preferences.appLock) "Protect RupeeFlow with PIN/Biometric" else "Enable security layer",
                    checked = preferences.appLock,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showPinSetupDialog = true
                        } else {
                            showPinVerifyDisableDialog = true
                        }
                    }
                )
                if (preferences.appLock) {
                    SettingsDivider()
                    PreferenceRow(
                        title = "Change PIN",
                        onClick = { showPinChangeDialog = true }
                    )
                    SettingsDivider()
                    PreferenceRow(
                        title = "Lock Timeout",
                        value = preferences.lockTimeout.description,
                        onClick = { showLockTimeoutDialog = true }
                    )
                    SettingsDivider()
                    TogglePreferenceRow(
                        title = "Fingerprint Unlock",
                        checked = preferences.fingerprintUnlock,
                        onCheckedChange = viewModel::updateFingerprintUnlock
                    )
                    SettingsDivider()
                    PreferenceRow(
                        title = "Lock App Now",
                        onClick = { viewModel.lockAppNow() }
                    )
                }
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Hide Balances",
                    checked = preferences.hideBalances,
                    onCheckedChange = viewModel::updateHideBalances
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Screenshot Protection",
                    subtitle = "Prevent screenshots in app and recent apps",
                    checked = preferences.screenshotProtection,
                    onCheckedChange = viewModel::updateScreenshotProtection
                )
            }

            // 💾 Storage
            SettingsSection(title = "STORAGE", icon = Icons.Rounded.Storage) {
                PreferenceRow(
                    title = "Backup & Restore",
                    onClick = onNavigateToBackupRestore
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Database Size",
                    value = "1.2 MB",
                    showChevron = false,
                    enabled = false,
                    onClick = { }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Cache Size",
                    value = "450 KB",
                    showChevron = false,
                    enabled = false,
                    onClick = { }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Reset App Data",
                    showChevron = false,
                    textColor = Color.Red,
                    onClick = { showResetAppDialog = true }
                )
            }

            if (preferences.developerModeEnabled) {
                SettingsSection(title = "DEVELOPER OPTIONS", icon = Icons.Rounded.Code) {
                    PreferenceRow(title = "Show Room Database Info", onClick = {})
                    SettingsDivider()
                    PreferenceRow(title = "Reset Sample Data", onClick = {})
                    SettingsDivider()
                    PreferenceRow(title = "Populate Dummy Data", onClick = {})
                    SettingsDivider()
                    PreferenceRow(title = "Debug Logs", onClick = {})
                    SettingsDivider()
                    PreferenceRow(title = "Performance Overlay", onClick = {})
                    SettingsDivider()
                    PreferenceRow(title = "Compose Recomposition Counter", onClick = {})
                    SettingsDivider()
                    TogglePreferenceRow(title = "Force Dark Theme", checked = true, onCheckedChange = {})
                    SettingsDivider()
                    PreferenceRow(title = "Export Database", onClick = {})
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A8A8A),
                    modifier = Modifier
                        .clickable {
                            if (!preferences.developerModeEnabled) {
                                versionTapCount++
                                if (versionTapCount >= 7) {
                                    viewModel.updateDeveloperModeEnabled(true)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Developer mode enabled!")
                                    }
                                } else if (versionTapCount > 3) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("You are now ${7 - versionTapCount} steps away from being a developer.")
                                    }
                                }
                            }
                        }
                        .padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showCurrencyDialog) {
        val currencies = listOf(
            "INR", "USD", "EUR", "GBP", "JPY", 
            "CAD", "AUD", "AED", "SGD", "CNY", 
            "HKD", "CHF", "MYR", "THB", "KWD",
            "SAR", "QAR", "BHD", "OMR", "NZD"
        )
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    currencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateCurrency(currency)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currency == preferences.currency,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(currency, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showFirstDayDialog) {
        val days = listOf(Calendar.SUNDAY to "Sunday", Calendar.MONDAY to "Monday")
        AlertDialog(
            onDismissRequest = { showFirstDayDialog = false },
            title = { Text("First Day of Week") },
            text = {
                Column {
                    days.forEach { (dayInt, dayName) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateFirstDayOfWeek(dayInt)
                                    showFirstDayDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = dayInt == preferences.firstDayOfWeek,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(dayName)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFirstDayDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showLockTimeoutDialog) {
        val timeouts = LockTimeout.entries
        AlertDialog(
            onDismissRequest = { showLockTimeoutDialog = false },
            title = { Text("Select Lock Timeout") },
            text = {
                Column {
                    timeouts.forEach { timeout ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateLockTimeout(timeout)
                                    showLockTimeoutDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = timeout == preferences.lockTimeout,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(timeout.description, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLockTimeoutDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showPinSetupDialog) {
        var step by remember { mutableStateOf(1) } // 1: Enter, 2: Confirm
        AlertDialog(
            onDismissRequest = { 
                showPinSetupDialog = false
                pinInput = ""
                pinConfirmInput = ""
                pinError = null
            },
            title = { Text(if (step == 1) "Create PIN" else "Confirm PIN") },
            text = {
                Column {
                    Text(
                        if (step == 1) "Enter a 4-6 digit security PIN" else "Re-enter your PIN to confirm",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = if (step == 1) pinInput else pinConfirmInput,
                        onValueChange = { val clean = it.filter { c -> c.isDigit() }.take(6)
                            if (step == 1) pinInput = clean else pinConfirmInput = clean
                            pinError = null
                        },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        isError = pinError != null,
                        supportingText = { pinError?.let { Text(it, color = Color.Red) } }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Important: Your PIN cannot be recovered. If you forget it, you'll need to clear app data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF59E0B)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (step == 1) {
                            if (pinInput.length < 4) {
                                pinError = "PIN must be at least 4 digits"
                            } else {
                                step = 2
                            }
                        } else {
                            if (pinInput == pinConfirmInput) {
                                scope.launch {
                                    viewModel.setupAppLock(pinInput)
                                    showPinSetupDialog = false
                                    pinInput = ""
                                    pinConfirmInput = ""
                                }
                            } else {
                                pinError = "PINs do not match"
                                pinConfirmInput = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(if (step == 1) "Next" else "Finish")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showPinSetupDialog = false 
                    pinInput = ""
                    pinConfirmInput = ""
                    pinError = null
                }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showPinVerifyDisableDialog) {
        AlertDialog(
            onDismissRequest = { showPinVerifyDisableDialog = false; pinInput = ""; pinError = null },
            title = { Text("Disable App Lock") },
            text = {
                Column {
                    Text("Enter your current PIN to disable security", color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(6); pinError = null },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        isError = pinError != null,
                        supportingText = { pinError?.let { Text(it, color = Color.Red) } }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val success = viewModel.verifyAndDisableAppLock(pinInput)
                            if (success) {
                                showPinVerifyDisableDialog = false
                                pinInput = ""
                            } else {
                                pinError = "Incorrect PIN"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Disable")
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White
        )
    }

    if (showResetAppDialog) {
        AlertDialog(
            onDismissRequest = { showResetAppDialog = false },
            title = { Text("Reset RupeeFlow?") },
            text = { Text("This will permanently delete all your accounts, transactions, and settings. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAppData {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            val mainIntent = android.content.Intent.makeRestartActivityTask(intent?.component)
                            context.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAppDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showAccountDialog) {
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = { Text("Select Default Account") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Option for "None"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateDefaultAccountId(-1L)
                                showAccountDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.defaultAccountId == -1L,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("None", color = Color.White)
                    }
                    
                    accounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultAccountId(account.id)
                                    showAccountDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = account.id == preferences.defaultAccountId,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(account.name, color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    if (showCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showCategoryDialog = false },
            title = { Text("Select Default Category") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    // Option for "None"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateDefaultCategoryId(-1L)
                                showCategoryDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = preferences.defaultCategoryId == -1L,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("None", color = Color.White)
                    }

                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateDefaultCategoryId(category.id)
                                    showCategoryDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = category.id == preferences.defaultCategoryId,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("${category.icon} ${category.name}", color = Color.White)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1F2937),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF8A8A8A),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8A8A8A),
                    letterSpacing = 1.sp
                )
            )
        }
        Surface(
            color = Color(0xFF111827).copy(alpha = 0.8f),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun PreferenceRow(
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            if (showChevron) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF8A8A8A),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = Color(0xFF1F2937).copy(alpha = 0.5f)
    )
}

@Composable
fun TogglePreferenceRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A8A8A)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null, // Handled by Row clickable to avoid conflicts
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color(0xFF8A8A8A),
                uncheckedTrackColor = Color(0xFF1F2937),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}
