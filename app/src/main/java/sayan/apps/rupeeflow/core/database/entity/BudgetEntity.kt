package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class BudgetPeriod {
    WEEKLY, MONTHLY
}

@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoryId"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val limitAmount: Double,
    val period: BudgetPeriod,
    val startDate: Long,
    val notified80Percent: Boolean = false,
    val notified100Percent: Boolean = false,
    val lastNotifiedPeriodStart: Long = 0L
)
