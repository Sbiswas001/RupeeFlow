package sayan.apps.rupeeflow.core.database.entity

import androidx.room.*

enum class OccurrenceStatus {
    PENDING, PAID, SKIPPED
}

@Entity(
    tableName = "recurring_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurringEntity::class,
            parentColumns = ["id"],
            childColumns = ["recurringItemId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("recurringItemId"),
        Index("transactionId"),
        Index("accountId"),
        Index(value = ["recurringItemId", "scheduledDate"], unique = true)
    ]
)
data class RecurringOccurrenceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recurringItemId: Long?,
    val scheduledDate: Long,
    val status: OccurrenceStatus = OccurrenceStatus.PENDING,
    val transactionId: Long? = null,
    val paymentDate: Long? = null,
    val accountId: Long? = null,
    val notes: String? = null,
    val recurringItemNameSnapshot: String? = null,
    val accountNameSnapshot: String? = null
)
