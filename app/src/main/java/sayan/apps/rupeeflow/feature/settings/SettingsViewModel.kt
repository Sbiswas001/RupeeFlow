package sayan.apps.rupeeflow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.*
import sayan.apps.rupeeflow.domain.model.RecurringItem
import android.content.Context
import android.net.Uri
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.util.AuthorizationHelper
import sayan.apps.rupeeflow.core.util.BackupWorker
import sayan.apps.rupeeflow.core.util.CredentialManagerHelper
import sayan.apps.rupeeflow.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.core.database.mapper.toEntity
import androidx.room.withTransaction
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import sayan.apps.rupeeflow.core.security.AppLockManager
import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinator
import sayan.apps.rupeeflow.domain.model.LockTimeout
import java.util.*
import javax.inject.Inject

@Serializable
data class FullBackupData(
    val transactions: List<Transaction>,
    val accounts: List<sayan.apps.rupeeflow.domain.model.Account>,
    val categories: List<sayan.apps.rupeeflow.domain.model.Category>,
    val recurringItems: List<sayan.apps.rupeeflow.domain.model.RecurringItem>,
    val budgets: List<sayan.apps.rupeeflow.domain.model.Budget>,
    val goals: List<sayan.apps.rupeeflow.domain.model.Goal>,
    val preferences: UserPreferences
)

sealed interface UserPreferencesUiState {
    data object Loading : UserPreferencesUiState
    data class Success(val preferences: UserPreferences) : UserPreferencesUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val database: RupeeFlowDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val planningRepository: PlanningRepository,
    private val backupRepository: BackupRepository,
    val appLockManager: AppLockManager,
    val authHelper: CredentialManagerHelper,
    val authorizationHelper: AuthorizationHelper,
    private val widgetUpdateCoordinator: WidgetUpdateCoordinator
) : ViewModel() {

    private val _isGoogleSignedIn = MutableStateFlow(false)
    val isGoogleSignedIn = _isGoogleSignedIn.asStateFlow()

    private val _googleAccountEmail = MutableStateFlow<String?>(null)
    val googleAccountEmail = _googleAccountEmail.asStateFlow()

    private val _driveBackupTimestamp = MutableStateFlow<Long?>(null)
    val driveBackupTimestamp = _driveBackupTimestamp.asStateFlow()

    fun setGoogleSignedIn(signedIn: Boolean, email: String? = null) {
        _isGoogleSignedIn.value = signedIn
        _googleAccountEmail.value = email
        if (!signedIn) {
            _driveBackupTimestamp.value = null
        }
    }

    fun checkGoogleSignIn(context: Context) {
        viewModelScope.launch {
            val credential = authHelper.signInWithGoogle(context, filterByAuthorized = true)
            if (credential != null) {
                setGoogleSignedIn(true, credential.id)
            }
        }
    }

    fun fetchDriveBackupInfo(accessToken: String) {
        viewModelScope.launch {
            try {
                val requestInitializer = com.google.api.client.http.HttpRequestInitializer { request ->
                    request.headers.authorization = "Bearer $accessToken"
                }
                val googleDriveService = com.google.api.services.drive.Drive.Builder(
                    com.google.api.client.http.javanet.NetHttpTransport(),
                    com.google.api.client.json.gson.GsonFactory(),
                    requestInitializer
                ).setApplicationName("RupeeFlow").build()
                
                val driveHelper = sayan.apps.rupeeflow.core.util.DriveServiceHelper(googleDriveService)
                val backups = driveHelper.listBackups()
                val mainBackup = backups.find { it.name == "rupeeflow_backup.db" }
                _driveBackupTimestamp.value = mainBackup?.modifiedTime?.value ?: mainBackup?.createdTime?.value
            } catch (e: Exception) {
                _driveBackupTimestamp.value = null
            }
        }
    }

    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    val uiState: StateFlow<UserPreferencesUiState> = userPreferencesRepository.userPreferences
        .map { UserPreferencesUiState.Success(it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferencesUiState.Loading
        )

    val userPreferences = userPreferencesRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    val accounts = accountRepository.getAccounts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories = categoryRepository.getCategories().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateCurrency(currency: String) {
        viewModelScope.launch {
            userPreferencesRepository.updateCurrency(currency)
        }
    }

    fun updateFirstDayOfWeek(day: Int) {
        viewModelScope.launch {
            userPreferencesRepository.updateFirstDayOfWeek(day)
        }
    }

    fun updateUseFinancialYear(use: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateUseFinancialYear(use)
        }
    }

    fun updateIndianNumberFormat(use: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateIndianNumberFormat(use)
        }
    }

    fun updateDefaultAccountId(id: Long) {
        viewModelScope.launch {
            userPreferencesRepository.updateDefaultAccountId(id)
        }
    }

    fun updateDefaultCategoryId(id: Long) {
        viewModelScope.launch {
            userPreferencesRepository.updateDefaultCategoryId(id)
        }
    }

    fun updateHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHapticFeedbackEnabled(enabled)
        }
    }

    fun updateConfirmBeforeDelete(confirm: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateConfirmBeforeDelete(confirm)
        }
    }

    fun updateAutoSaveDrafts(autoSave: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutoSaveDrafts(autoSave)
        }
    }

    fun updateAmoledBlack(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAmoledBlack(enabled)
        }
    }

    fun updateDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDynamicColor(enabled)
        }
    }

    fun updateBillReminders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateBillReminders(enabled)
        }
    }

    fun updateBudgetAlerts(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateBudgetAlerts(enabled)
        }
    }

    fun updateGoalReminders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateGoalReminders(enabled)
        }
    }

    fun updateLockTimeout(timeout: LockTimeout) {
        viewModelScope.launch {
            userPreferencesRepository.updateLockTimeout(timeout)
        }
    }

    fun lockAppNow() {
        appLockManager.lock()
    }

    suspend fun verifyAndDisableAppLock(pin: String): Boolean {
        val success = appLockManager.verifyPin(pin)
        if (success) {
            appLockManager.disableAppLock()
        }
        return success
    }

    suspend fun setupAppLock(pin: String) {
        appLockManager.createPin(pin)
    }

    fun updateAppLock(enabled: Boolean) {
        // This is now handled through setup flow or disable flow
    }

    fun updateFingerprintUnlock(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateFingerprintUnlock(enabled)
        }
    }

    fun updateHideBalances(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHideBalances(enabled)
            widgetUpdateCoordinator.refreshAll()
        }
    }

    fun updateScreenshotProtection(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateScreenshotProtection(enabled)
        }
    }

    fun updateLastBackupTimestamp(timestamp: Long) {
        viewModelScope.launch {
            userPreferencesRepository.updateLastBackupTimestamp(timestamp)
        }
    }

    fun updateFirstRun(isFirstRun: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateFirstRun(isFirstRun)
        }
    }

    fun updateDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDeveloperModeEnabled(enabled)
        }
    }

    fun updateAutomaticBackupEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.updateAutomaticBackupEnabled(enabled)
            if (enabled) {
                BackupWorker.schedule(context)
            } else {
                BackupWorker.cancel(context)
            }
        }
    }

    fun exportToCsv(uri: Uri, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getTransactions().first()
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write("ID,Title,Amount,Date,Category,Type,Note\n")
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        transactions.forEach { trans ->
                            val line = "${trans.id},\"${trans.title}\",${trans.amount},${dateFormat.format(Date(trans.timestamp))},${trans.category},${if (trans.isIncome) "INCOME" else "EXPENSE"},\"${trans.note ?: ""}\"\n"
                            writer.write(line)
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun importFromCsv(uri: Uri, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readLine() // Skip header
                        val categories = categoryRepository.getCategories().first()
                        val accounts = accountRepository.getAccounts().first()
                        val defaultAccount = accounts.firstOrNull() ?: return@launch onError("No account found.")
                        val defaultCategory = categories.firstOrNull { it.name == "General" } ?: categories.firstOrNull() ?: return@launch onError("No category found.")

                        var line: String? = reader.readLine()
                        while (line != null) {
                            val parts = line.split(",")
                            if (parts.size >= 6) {
                                val title = parts[1].removeSurrounding("\"")
                                val amount = parts[2].toDoubleOrNull() ?: 0.0
                                val isIncome = parts[5].trim().uppercase() == "INCOME"
                                val note = if (parts.size > 6) parts[6].removeSurrounding("\"") else null
                                val categoryName = parts[4]
                                val category = categories.find { it.name.equals(categoryName, ignoreCase = true) } ?: defaultCategory
                                
                                transactionRepository.addTransaction(
                                    Transaction(
                                        id = "",
                                        title = title,
                                        amount = amount,
                                        timestamp = System.currentTimeMillis(),
                                        category = category.name,
                                        isIncome = isIncome,
                                        note = note
                                    ),
                                    accountId = defaultAccount.id,
                                    categoryId = category.id
                                )
                            }
                            line = reader.readLine()
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun exportToJson(uri: Uri, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val transactions = transactionRepository.getTransactions().first()
                val accounts = accountRepository.getAccountsIncludingClosed().first()
                val categories = categoryRepository.getCategories().first()
                val recurringItems = recurringRepository.getRecurringItems().first()
                val budgets = planningRepository.getBudgetsWithProgress().first().map { it.budget }
                val goals = planningRepository.getGoals().first()
                val preferences = userPreferencesRepository.userPreferences.first()

                val backupData = FullBackupData(transactions, accounts, categories, recurringItems, budgets, goals, preferences)
                val jsonString = json.encodeToString(backupData)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonString)
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun importFromJson(uri: Uri, context: Context, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val jsonString = reader.readText()
                        val backupData = json.decodeFromString<FullBackupData>(jsonString)

                        database.withTransaction {
                            // 1. Clear everything
                            database.transactionDao().clearAllTransactions()
                            database.accountDao().clearAllAccounts()
                            database.categoryDao().clearAllCategories()
                            database.utilityDao().clearAllOccurrences()
                            database.utilityDao().clearAllRecurringItems() 
                            database.planningDao().clearAllBudgets()
                            database.planningDao().clearAllGoals()

                            // 2. Restore Categories
                            backupData.categories.forEach { 
                                database.categoryDao().insertCategory(it.toEntity())
                            }

                            // 3. Restore Accounts
                            backupData.accounts.forEach {
                                database.accountDao().insertAccount(it.toEntity())
                            }

                            // 4. Restore Transactions
                            backupData.transactions.forEach { trans ->
                                database.transactionDao().insertTransaction(
                                    trans.toEntity(trans.accountId, trans.categoryId)
                                )
                            }
                            
                            // 5. Restore Recurring Items and Occurrences
                            backupData.recurringItems.forEach { item ->
                                val entity = item.toEntity()
                                val itemId = database.utilityDao().insertRecurringItem(entity)
                                
                                // Restore occurrences
                                item.occurrences.forEach { occ ->
                                    database.utilityDao().insertOccurrence(occ.toEntity().copy(id = 0, recurringItemId = itemId))
                                }
                            }

                            // 6. Restore Budgets
                            backupData.budgets.forEach { budget ->
                                database.planningDao().insertBudget(budget.toEntity())
                            }

                            // 7. Restore Goals
                            backupData.goals.forEach { goal ->
                                database.planningDao().insertGoal(goal.toEntity())
                            }

                            // 8. Restore Preferences (Not strictly database, but part of full restore)
                            userPreferencesRepository.updateCurrency(backupData.preferences.currency)
                            userPreferencesRepository.updateIndianNumberFormat(backupData.preferences.indianNumberFormat)
                            // ... add more as needed
                        }
                    }
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error")
            }
        }
    }

    fun backupDatabase(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            backupRepository.backupToLocal(uri).collect { result ->
                when (result) {
                    is BackupResult.Success -> {
                        updateLastBackupTimestamp(System.currentTimeMillis())
                        onSuccess()
                    }
                    is BackupResult.Error -> onError(result.message)
                    is BackupResult.Loading -> { /* Handle loading if needed */ }
                }
            }
        }
    }

    fun restoreDatabase(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            backupRepository.restoreFromLocal(uri).collect { result ->
                when (result) {
                    is BackupResult.Success -> onSuccess()
                    is BackupResult.Error -> onError(result.message)
                    is BackupResult.Loading -> { /* Handle loading if needed */ }
                }
            }
        }
    }

    fun backupToGoogleDrive(accessToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.backupToGoogleDrive(accessToken)
            if (result is BackupResult.Success) {
                updateLastBackupTimestamp(System.currentTimeMillis())
                onSuccess()
            } else if (result is BackupResult.Error) {
                onError(result.message)
            }
        }
    }

    fun restoreFromGoogleDrive(accessToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.restoreFromGoogleDrive(accessToken)
            if (result is BackupResult.Success) {
                onSuccess()
            } else if (result is BackupResult.Error) {
                onError(result.message)
            }
        }
    }

    fun deleteGoogleDriveBackup(accessToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = backupRepository.deleteGoogleDriveBackup(accessToken)
            if (result is BackupResult.Success) {
                _driveBackupTimestamp.value = null
                onSuccess()
            } else if (result is BackupResult.Error) {
                onError(result.message)
            }
        }
    }

    fun resetAppData(onSuccess: () -> Unit) {
        viewModelScope.launch {
            database.withTransaction {
                database.transactionDao().clearAllTransactions()
                database.accountDao().clearAllAccounts()
                database.categoryDao().clearAllCategories()
                database.utilityDao().clearAllOccurrences()
                database.utilityDao().clearAllRecurringItems()
                database.planningDao().clearAllBudgets()
                database.planningDao().clearAllGoals()
            }
            userPreferencesRepository.updateFirstRun(true)
            onSuccess()
        }
    }
}
