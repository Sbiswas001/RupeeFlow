package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import sayan.apps.rupeeflow.domain.model.PaymentMethodType
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UPIApp

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
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
        Index("categoryId"),
        Index("accountId"),
        Index("debitCardId"),
        Index("timestamp"),
        Index("transferId")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long?,
    val timestamp: Long,
    val note: String? = null,
    val isRecurring: Boolean = false,
    val upiTransactionId: String? = null,
    val upiApp: UPIApp? = null,
    val upiLinkedBank: String? = null,
    val previousBalance: Double? = null,
    val actualBalance: Double? = null,
    val reconciliationReason: String? = null,
    val accountNameSnapshot: String? = null,
    val accountCategorySnapshot: AccountCategory? = null,
    val transferId: String? = null,
    val transferAccountId: Long? = null,
    val transferAccountNameSnapshot: String? = null,
    val isIncoming: Boolean = false,
    val paymentMethodType: PaymentMethodType? = null,
    val debitCardId: Long? = null,
    val debitCardNameSnapshot: String? = null,
    val debitCardLast4Snapshot: String? = null,
    val upiAppNameSnapshot: String? = null
)
