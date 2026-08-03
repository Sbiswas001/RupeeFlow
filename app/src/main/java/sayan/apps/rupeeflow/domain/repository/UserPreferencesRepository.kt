package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.AppTheme
import sayan.apps.rupeeflow.domain.model.UserPreferences

interface UserPreferencesRepository {
    val userPreferences: Flow<UserPreferences>
    suspend fun updateCurrency(currency: String)
    suspend fun updateFirstDayOfWeek(day: Int)
    suspend fun updateUseFinancialYear(use: Boolean)
    suspend fun updateIndianNumberFormat(use: Boolean)
    suspend fun updateDefaultAccountId(id: Long)
    suspend fun updateDefaultCategoryId(id: Long)
    suspend fun updateHapticFeedbackEnabled(enabled: Boolean)
    suspend fun updateConfirmBeforeDelete(confirm: Boolean)
    suspend fun updateAutoSaveDrafts(autoSave: Boolean)
    suspend fun updateTheme(theme: AppTheme)
    suspend fun updateAmoledBlack(enabled: Boolean)
    suspend fun updateDynamicColor(enabled: Boolean)
    suspend fun updateBillReminders(enabled: Boolean)
    suspend fun updateBudgetAlerts(enabled: Boolean)
    suspend fun updateGoalReminders(enabled: Boolean)
    suspend fun updateAppLock(enabled: Boolean)
    suspend fun updateFingerprintUnlock(enabled: Boolean)
    suspend fun updateHideBalances(enabled: Boolean)
    suspend fun updateScreenshotProtection(enabled: Boolean)
    suspend fun updateLastBackupTimestamp(timestamp: Long)
    suspend fun updateDeveloperModeEnabled(enabled: Boolean)
}
