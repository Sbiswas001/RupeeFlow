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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val preferences by viewModel.userPreferences.collectAsState()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showThemeBottomSheet by remember { mutableStateOf(false) }
    
    var versionTapCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val sheetState = rememberModalBottomSheetState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                PreferenceRow(
                    title = "Default Account",
                    value = if (preferences.defaultAccountId == -1L) "None" else "Account ID: ${preferences.defaultAccountId}",
                    onClick = { /* Picker */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Default Category",
                    value = if (preferences.defaultCategoryId == -1L) "None" else "Category ID: ${preferences.defaultCategoryId}",
                    onClick = { /* Picker */ }
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
                PreferenceRow(
                    title = "Theme",
                    value = preferences.theme.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = { showThemeBottomSheet = true }
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "AMOLED Black",
                    checked = preferences.amoledBlack,
                    onCheckedChange = viewModel::updateAmoledBlack
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Dynamic Color",
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
                SettingsDivider()
                PreferenceRow(
                    title = "Daily Summary",
                    value = "Preview",
                    onClick = { /* Future */ }
                )
            }

            // 🔒 Privacy & Security
            SettingsSection(title = "PRIVACY & SECURITY", icon = Icons.Rounded.Security) {
                TogglePreferenceRow(
                    title = "App Lock",
                    checked = preferences.appLock,
                    onCheckedChange = viewModel::updateAppLock
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Fingerprint Unlock",
                    checked = preferences.fingerprintUnlock,
                    onCheckedChange = viewModel::updateFingerprintUnlock
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Hide Balances",
                    checked = preferences.hideBalances,
                    onCheckedChange = viewModel::updateHideBalances
                )
                SettingsDivider()
                TogglePreferenceRow(
                    title = "Screenshot Protection",
                    checked = preferences.screenshotProtection,
                    onCheckedChange = viewModel::updateScreenshotProtection
                )
            }

            // 💾 Storage
            SettingsSection(title = "STORAGE", icon = Icons.Rounded.Storage) {
                PreferenceRow(
                    title = "Backup & Restore",
                    onClick = { /* Future */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Import",
                    onClick = { /* Future */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Export",
                    onClick = { /* Future */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Last Backup",
                    value = if (preferences.lastBackupTimestamp == -1L) "Never" else {
                        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                        sdf.format(Date(preferences.lastBackupTimestamp))
                    },
                    onClick = { /* Run backup */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Database Size",
                    value = "1.2 MB",
                    onClick = { /* Info */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Cache Size",
                    value = "450 KB",
                    onClick = { /* Info */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Clear Cache",
                    onClick = { /* Action */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "Optimize Database",
                    onClick = { /* Action */ }
                )
            }

            // 🤖 Advanced
            SettingsSection(title = "ADVANCED", icon = Icons.Rounded.SmartToy) {
                PreferenceRow(
                    title = "AI Settings",
                    value = "Preview",
                    onClick = { /* Future */ }
                )
                SettingsDivider()
                PreferenceRow(
                    title = "OCR Settings",
                    value = "Preview",
                    onClick = { /* Future */ }
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

    if (showThemeBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showThemeBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1F2937),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = "Choose Theme",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ThemeOption(
                    title = "System Default",
                    selected = preferences.theme == AppTheme.SYSTEM,
                    onClick = {
                        viewModel.updateTheme(AppTheme.SYSTEM)
                        showThemeBottomSheet = false
                    }
                )
                ThemeOption(
                    title = "Light",
                    selected = preferences.theme == AppTheme.LIGHT,
                    onClick = {
                        viewModel.updateTheme(AppTheme.LIGHT)
                        showThemeBottomSheet = false
                    }
                )
                ThemeOption(
                    title = "Dark",
                    selected = preferences.theme == AppTheme.DARK,
                    onClick = {
                        viewModel.updateTheme(AppTheme.DARK)
                        showThemeBottomSheet = false
                    }
                )
            }
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
                                    selectedColor = Color(0xFF7C3AED)
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
                    Text("Cancel", color = Color(0xFF7C3AED))
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF7C3AED),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF8A8A8A),
                modifier = Modifier.size(20.dp)
            )
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
                checkedTrackColor = Color(0xFF7C3AED),
                uncheckedThumbColor = Color(0xFF8A8A8A),
                uncheckedTrackColor = Color(0xFF1F2937),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = Color(0xFF7C3AED)
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (selected) Color(0xFF7C3AED) else Color.White
        )
    }
}

