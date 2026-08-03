package sayan.apps.rupeeflow.domain.model

data class RecurringItem(
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isAutoPay: Boolean,
    val status: String,
    val frequency: String,
    val category: String,
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
    val lastPaidDate: Long? = null
)
