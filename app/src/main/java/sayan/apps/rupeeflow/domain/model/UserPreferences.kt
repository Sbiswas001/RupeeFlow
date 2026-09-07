package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable
import java.util.Calendar

@Serializable
enum class LockTimeout(val description: String, val durationMillis: Long) {
    IMMEDIATE("Immediately", 0L),
    ONE_MINUTE("1 Minute", 60_000L),
    FIVE_MINUTES("5 Minutes", 300_000L),
    FIFTEEN_MINUTES("15 Minutes", 900_000L),
    ONE_HOUR("1 Hour", 3_600_000L)
}

@Serializable
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
    val amoledBlack: Boolean = false,
    val dynamicColor: Boolean = false,
    val billReminders: Boolean = true,
    val budgetAlerts: Boolean = true,
    val goalReminders: Boolean = true,
    val appLock: Boolean = false,
    val fingerprintUnlock: Boolean = false,
    val lockTimeout: LockTimeout = LockTimeout.ONE_MINUTE,
    val encryptedPinMaterial: String? = null,
    val failedAttempts: Int = 0,
    val cooldownEndTimeMillis: Long = 0,
    val hideBalances: Boolean = false,
    val screenshotProtection: Boolean = false,
    val lastBackupTimestamp: Long = -1L,
    val isFirstRun: Boolean = true,
    val developerModeEnabled: Boolean = false,
    val automaticBackupEnabled: Boolean = false,
    val aiModelVerified: Boolean = false,
    val verifiedAiModelId: String = "",
    val verifiedAiModelSha256: String = "",
    val verifiedAiModelSizeBytes: Long = 0L
)
