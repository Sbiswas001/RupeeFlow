package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val logoUrl: String? = null,
    val defaultCategoryId: Long? = null
)
