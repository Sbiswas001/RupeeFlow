package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurringStatus {
    PENDING, PAID, OVERDUE, CANCELLED, PAUSED
}

enum class RecurrenceFrequency {
    NONE, DAILY, WEEKLY, MONTHLY, QUARTERLY, HALF_YEARLY, YEARLY, CUSTOM
}

@Entity(tableName = "recurring_items")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val dueDate: Long,
    val isAutoPay: Boolean = false,
    val status: RecurringStatus = RecurringStatus.PENDING,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
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
