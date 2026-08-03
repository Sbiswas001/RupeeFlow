package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import sayan.apps.rupeeflow.domain.model.TransactionType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val budget: Double? = null,
    val isArchived: Boolean = false,
    val parentCategoryId: Long? = null
)
