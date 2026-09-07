package sayan.apps.rupeeflow.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
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
        val AMOLED_BLACK = booleanPreferencesKey("amoled_black")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val BILL_REMINDERS = booleanPreferencesKey("bill_reminders")
        val BUDGET_ALERTS = booleanPreferencesKey("budget_alerts")
        val GOAL_REMINDERS = booleanPreferencesKey("goal_reminders")
        val APP_LOCK = booleanPreferencesKey("app_lock")
        val FINGERPRINT_UNLOCK = booleanPreferencesKey("fingerprint_unlock")
        val LOCK_TIMEOUT = stringPreferencesKey("lock_timeout")
        val ENCRYPTED_PIN_MATERIAL = stringPreferencesKey("encrypted_pin_material")
        val FAILED_ATTEMPTS = intPreferencesKey("failed_attempts")
        val COOLDOWN_END_TIME_MILLIS = longPreferencesKey("cooldown_end_time_millis")
        val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val IS_FIRST_RUN = booleanPreferencesKey("is_first_run")
        val DEVELOPER_MODE_ENABLED = booleanPreferencesKey("developer_mode_enabled")
        val AUTOMATIC_BACKUP_ENABLED = booleanPreferencesKey("automatic_backup_enabled")
        val AI_MODEL_VERIFIED = booleanPreferencesKey("ai_model_verified")
        val VERIFIED_AI_MODEL_ID = stringPreferencesKey("verified_ai_model_id")
        val VERIFIED_AI_MODEL_SHA256 = stringPreferencesKey("verified_ai_model_sha256")
        val VERIFIED_AI_MODEL_SIZE_BYTES = longPreferencesKey("verified_ai_model_size_bytes")
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
                amoledBlack = preferences[PreferencesKeys.AMOLED_BLACK] ?: false,
                dynamicColor = preferences[PreferencesKeys.DYNAMIC_COLOR] ?: false,
                billReminders = preferences[PreferencesKeys.BILL_REMINDERS] ?: true,
                budgetAlerts = preferences[PreferencesKeys.BUDGET_ALERTS] ?: true,
                goalReminders = preferences[PreferencesKeys.GOAL_REMINDERS] ?: true,
                appLock = preferences[PreferencesKeys.APP_LOCK] ?: false,
                fingerprintUnlock = preferences[PreferencesKeys.FINGERPRINT_UNLOCK] ?: false,
                lockTimeout = sayan.apps.rupeeflow.domain.model.LockTimeout.valueOf(
                    preferences[PreferencesKeys.LOCK_TIMEOUT] ?: sayan.apps.rupeeflow.domain.model.LockTimeout.ONE_MINUTE.name
                ),
                encryptedPinMaterial = preferences[PreferencesKeys.ENCRYPTED_PIN_MATERIAL],
                failedAttempts = preferences[PreferencesKeys.FAILED_ATTEMPTS] ?: 0,
                cooldownEndTimeMillis = preferences[PreferencesKeys.COOLDOWN_END_TIME_MILLIS] ?: 0L,
                hideBalances = preferences[PreferencesKeys.HIDE_BALANCES] ?: false,
                screenshotProtection = preferences[PreferencesKeys.SCREENSHOT_PROTECTION] ?: false,
                lastBackupTimestamp = preferences[PreferencesKeys.LAST_BACKUP_TIMESTAMP] ?: -1L,
                isFirstRun = preferences[PreferencesKeys.IS_FIRST_RUN] ?: true,
                developerModeEnabled = preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] ?: false,
                automaticBackupEnabled = preferences[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] ?: false,
                aiModelVerified = preferences[PreferencesKeys.AI_MODEL_VERIFIED] ?: false,
                verifiedAiModelId = preferences[PreferencesKeys.VERIFIED_AI_MODEL_ID] ?: "",
                verifiedAiModelSha256 = preferences[PreferencesKeys.VERIFIED_AI_MODEL_SHA256] ?: "",
                verifiedAiModelSizeBytes = preferences[PreferencesKeys.VERIFIED_AI_MODEL_SIZE_BYTES] ?: 0L
            )
        }.flowOn(Dispatchers.Default)

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

    override suspend fun updateLockTimeout(timeout: sayan.apps.rupeeflow.domain.model.LockTimeout) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOCK_TIMEOUT] = timeout.name
        }
    }

    override suspend fun updateEncryptedPinMaterial(material: String?) {
        dataStore.edit { preferences ->
            if (material == null) {
                preferences.remove(PreferencesKeys.ENCRYPTED_PIN_MATERIAL)
            } else {
                preferences[PreferencesKeys.ENCRYPTED_PIN_MATERIAL] = material
            }
        }
    }

    override suspend fun updateFailedAttempts(attempts: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FAILED_ATTEMPTS] = attempts
        }
    }

    override suspend fun updateCooldownEndTimeMillis(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.COOLDOWN_END_TIME_MILLIS] = timestamp
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

    override suspend fun updateFirstRun(isFirstRun: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FIRST_RUN] = isFirstRun
        }
    }

    override suspend fun updateDeveloperModeEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEVELOPER_MODE_ENABLED] = enabled
        }
    }

    override suspend fun updateAutomaticBackupEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTOMATIC_BACKUP_ENABLED] = enabled
        }
    }

    override suspend fun updateAiModelVerification(verified: Boolean, modelId: String, sha256: String, sizeBytes: Long) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_MODEL_VERIFIED] = verified
            preferences[PreferencesKeys.VERIFIED_AI_MODEL_ID] = modelId
            preferences[PreferencesKeys.VERIFIED_AI_MODEL_SHA256] = sha256
            preferences[PreferencesKeys.VERIFIED_AI_MODEL_SIZE_BYTES] = sizeBytes
        }
    }
}
