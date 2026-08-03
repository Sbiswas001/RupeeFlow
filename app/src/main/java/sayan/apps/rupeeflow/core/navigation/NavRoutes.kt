package sayan.apps.rupeeflow.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey

@Serializable
data object Dashboard : NavRoute

@Serializable
data object Activity : NavRoute

@Serializable
data object AddTransaction : NavRoute

@Serializable
data object Accounts : NavRoute

@Serializable
data object Insights : NavRoute

@Serializable
data object Recurring : NavRoute

@Serializable
data object AddRecurring : NavRoute

@Serializable
data object NetWorth : NavRoute

@Serializable
data object Search : NavRoute

@Serializable
data object Categories : NavRoute

@Serializable
data object Merchants : NavRoute

@Serializable
data object Tags : NavRoute

@Serializable
data object Archived : NavRoute

@Serializable
data object BackupRestore : NavRoute

@Serializable
data object ImportExport : NavRoute

@Serializable
data object Notifications : NavRoute

@Serializable
data object Settings : NavRoute

@Serializable
data object AiChat : NavRoute

@Serializable
data object AiDeveloper : NavRoute

@Serializable
data object About : NavRoute

@Serializable
data class RecurringDetail(val id: Long) : NavRoute

@Serializable
data class CategoryDetail(val id: Long) : NavRoute

@Serializable
data class TransactionDetail(val id: Long) : NavRoute

@Serializable
data class EditTransaction(val id: Long) : NavRoute
