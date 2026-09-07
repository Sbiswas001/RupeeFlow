package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "account_upi_apps",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accountId")
    ]
)
data class AccountUpiAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long? = null,
    val appName: String,
    val appPackage: String? = null,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false
)
