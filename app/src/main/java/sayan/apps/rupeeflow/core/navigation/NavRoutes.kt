package sayan.apps.rupeeflow.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey

@Serializable
data object Dashboard : NavRoute

@Serializable
data class Activity(val initialTab: Int = 0) : NavRoute

@Serializable
data object AddTransaction : NavRoute

@Serializable
data object Accounts : NavRoute

@Serializable
data class Insights(val initialTab: Int = 0) : NavRoute

@Serializable
data object Recurring : NavRoute

@Serializable
data object AddRecurring : NavRoute

@Serializable
data object NetWorth : NavRoute

@Serializable
data object Search : NavRoute

@Serializable
data object AllCategoryBreakdown : NavRoute

@Serializable
data object Categories : NavRoute

@Serializable
data object BackupRestore : NavRoute

@Serializable
data object Notifications : NavRoute

@Serializable
data object Settings : NavRoute

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

@Serializable
data class EditRecurring(val id: Long) : NavRoute
