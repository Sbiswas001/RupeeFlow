package sayan.apps.rupeeflow.domain.model

import java.util.Calendar

enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val currency: String = "INR",
    val firstDayOfWeek: Int = Calendar.MONDAY,
    val useFinancialYear: Boolean = true,
    val indianNumberFormat: Boolean = true,
    val defaultAccountId: Long = -1,
    val defaultCategoryId: Long = -1,
    val hapticFeedbackEnabled: Boolean = true,
    val confirmBeforeDelete: Boolean = true,
    val autoSaveDrafts: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val amoledBlack: Boolean = false,
    val dynamicColor: Boolean = true,
    val billReminders: Boolean = true,
    val budgetAlerts: Boolean = true,
    val goalReminders: Boolean = true,
    val appLock: Boolean = false,
    val fingerprintUnlock: Boolean = false,
    val hideBalances: Boolean = false,
    val screenshotProtection: Boolean = false,
    val lastBackupTimestamp: Long = -1L,
    val developerModeEnabled: Boolean = false
)
