package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RecurringItem(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isAutoPay: Boolean,
    val status: String,
    val frequency: String,
    val frequencyInterval: Int = 1,
    val frequencyUnit: String = "MONTHS",
    val category: String,
    val categoryId: Long? = null,
    val subCategory: String? = null,
    val iconRes: Int? = null,
    val reminderDaysBefore: Int = 1,
    val paymentMethod: String? = null,
    val accountId: Long? = null,
    val notes: String? = null,
    val recurrenceCount: Int? = null,
    val totalRecurrence: Int? = null,
    val outstandingAmount: Double? = null,
    val minimumDue: Double? = null,
    val lastPaidDate: Long? = null,
    val occurrences: List<RecurringOccurrence> = emptyList(),
    val recurringItemNameSnapshot: String? = null,
    val accountNameSnapshot: String? = null
)

@Serializable
data class RecurringOccurrence(
    val id: Long = 0,
    val recurringItemId: Long?,
    val scheduledDate: Long,
    val status: String,
    val transactionId: Long? = null,
    val paymentDate: Long? = null,
    val accountId: Long? = null,
    val amount: Double? = null,
    val notes: String? = null,
    val recurringItemNameSnapshot: String? = null,
    val accountNameSnapshot: String? = null,
    val parentAccountId: Long? = null,
    val parentAccountName: String? = null
)
