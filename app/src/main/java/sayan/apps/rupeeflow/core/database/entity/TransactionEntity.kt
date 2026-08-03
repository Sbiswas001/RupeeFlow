package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
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
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = MerchantEntity::class,
            parentColumns = ["id"],
            childColumns = ["merchantId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("merchantId"),
        Index("timestamp")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long?,
    val accountId: Long,
    val merchantId: Long?,
    val timestamp: Long,
    val note: String? = null,
    val isRecurring: Boolean = false,
    val upiTransactionId: String? = null,
    val upiApp: UPIApp? = null,
    val upiLinkedBank: String? = null
)
