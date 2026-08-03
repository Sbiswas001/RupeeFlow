package sayan.apps.rupeeflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import sayan.apps.rupeeflow.domain.model.AppTheme
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import java.io.IOException
import java.util.Calendar
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val CURRENCY = stringPreferencesKey("currency")
        val FIRST_DAY_OF_WEEK = intPreferencesKey("first_day_of_week")
        val USE_FINANCIAL_YEAR = booleanPreferencesKey("use_financial_year")
        val INDIAN_NUMBER_FORMAT = booleanPreferencesKey("indian_number_format")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val DEFAULT_CATEGORY_ID = longPreferencesKey("default_category_id")
        val HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
        val CONFIRM_BEFORE_DELETE = booleanPreferencesKey("confirm_before_delete")
        val AUTO_SAVE_DRAFTS = booleanPreferencesKey("auto_save_drafts")
        val THEME = stringPreferencesKey("theme")
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val GOAL_REMINDERS = booleanPreferencesKey("goal_reminders")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val FINGERPRINT_UNLOCK = booleanPreferencesKey("fingerprint_unlock")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
    }

    override val userPreferences: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                currency = preferences[PreferencesKeys.CURRENCY] ?: "INR",
                firstDayOfWeek = preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] ?: Calendar.MONDAY,
                useFinancialYear = preferences[PreferencesKeys.USE_FINANCIAL_YEAR] ?: true,
                indianNumberFormat = preferences[PreferencesKeys.INDIAN_NUMBER_FORMAT] ?: true,
                defaultAccountId = preferences[PreferencesKeys.DEFAULT_ACCOUNT_ID] ?: -1L,
                defaultCategoryId = preferences[PreferencesKeys.DEFAULT_CATEGORY_ID] ?: -1L,
                hapticFeedbackEnabled = preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] ?: true,
                confirmBeforeDelete = preferences[PreferencesKeys.CONFIRM_BEFORE_DELETE] ?: true,
                autoSaveDrafts = preferences[PreferencesKeys.AUTO_SAVE_DRAFTS] ?: true,
                theme = AppTheme.valueOf(preferences[PreferencesKeys.THEME] ?: AppTheme.SYSTEM.name),
                amoledBlack = preferences[PreferencesKeys.AMOLED_BLACK] ?: false,
                dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: true,
                billReminders = preferences[PreferencesKeys.BILL_REMINDERS] ?: true,
                budgetAlerts = preferences[PreferencesKeys.BUDGET_ALERTS] ?: true,
                goalReminders = preferences[PreferencesKeys.GOAL_REMINDERS] ?: true,
                appLock = preferences[PreferencesKeys.APP_LOCK] ?: false,
                fingerprintUnlock = preferences[PreferencesKeys.FINGERPRINT_UNLOCK] ?: false,
                hideBalances = preferences[PreferencesKeys.HIDE_BALANCES] ?: false,
                screenshotProtection = preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false,
                lastBackupTimestamp = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: -1L,
                developerModeEnabled = preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] ?: false
            )
        }

    override suspend fun updateCurrency(currency: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY] = currency
        }
    }

    override suspend fun updateFirstDayOfWeek(day: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_DAY_OF_WEEK] = day
        }
    }

    override suspend fun updateUseFinancialYear(use: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_FINANCIAL_YEAR] = use
        }
    }

    override suspend fun updateIndianNumberFormat(use: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.INDIAN_NUMBER_FORMAT] = use
        }
    }

    override suspend fun updateDefaultAccountId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ACCOUNT_ID] = id
        }
    }

    override suspend fun updateDefaultCategoryId(id: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_CATEGORY_ID] = id
        }
    }

    override suspend fun updateHapticFeedbackEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAPTIC_FEEDBACK_ENABLED] = enabled
        }
    }

    override suspend fun updateConfirmBeforeDelete(confirm: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONFIRM_BEFORE_DELETE] = confirm
        }
    }

    override suspend fun updateAutoSaveDrafts(autoSave: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_SAVE_DRAFTS] = autoSave
        }
    }

    override suspend fun updateTheme(theme: AppTheme) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME] = theme.name
        }
    }

    override suspend fun updateAmoledBlack(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AMOLED_BLACK] = enabled
        }
    }

    override suspend fun updateDynamicColor(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR] = enabled
        }
    }

    override suspend fun updateBillReminders(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BILL_REMINDERS] = enabled
        }
    }

    override suspend fun updateBudgetAlerts(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUDGET_ALERTS] = enabled
        }
    }

    override suspend fun updateGoalReminders(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.GOAL_REMINDERS] = enabled
        }
    }

    override suspend fun updateAppLock(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LOCK] = enabled
        }
    }

    override suspend fun updateFingerprintUnlock(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FINGERPRINT_UNLOCK] = enabled
        }
    }

    override suspend fun updateHideBalances(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_BALANCES] = enabled
        }
    }

    override suspend fun updateScreenshotProtection(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SCREENSHOT_PROTECTION] = enabled
        }
    }

    override suspend fun updateLastBackupTimestamp(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    override suspend fun updateDeveloperModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] = enabled
        }
    }
}
