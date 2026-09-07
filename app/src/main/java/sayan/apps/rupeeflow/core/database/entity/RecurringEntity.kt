package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RecurringStatus {
    ACTIVE, PAUSED, CANCELLED
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
    val dueDate: Long, // Initial start date
    val isAutoPay: Boolean = false,
    val status: RecurringStatus = RecurringStatus.ACTIVE,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val frequencyInterval: Int = 1,
    val frequencyUnit: String = "MONTHS",
    val category: String,
    val categoryId: Long? = null,
    val subCategory: String? = null,
    val iconRes: Int? = null,
    val reminderDaysBefore: Int = 1,
    val paymentMethod: String? = null,
    val defaultAccountId: Long? = null,
    val notes: String? = null,
    val recurrenceCount: Int? = null,
    val totalRecurrence: Int? = null,
    val outstandingAmount: Double? = null,
    val minimumDue: Double? = null,
    val lastPaidDate: Long? = null,
    val lastGeneratedOccurrenceDate: Long? = null
)
