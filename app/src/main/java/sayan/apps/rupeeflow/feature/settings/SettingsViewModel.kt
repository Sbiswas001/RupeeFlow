package sayan.apps.rupeeflow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.AppTheme
import sayan.apps.rupeeflow.domain.model.UserPreferences
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val userPreferences = userPreferencesRepository.userPreferences.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
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

    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository.updateTheme(theme)
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

    fun updateAppLock(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppLock(enabled)
        }
    }

    fun updateFingerprintUnlock(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateFingerprintUnlock(enabled)
        }
    }

    fun updateHideBalances(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateHideBalances(enabled)
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

    fun updateDeveloperModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.updateDeveloperModeEnabled(enabled)
        }
    }
}
