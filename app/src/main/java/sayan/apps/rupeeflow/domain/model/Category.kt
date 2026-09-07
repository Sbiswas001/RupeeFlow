package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val budget: Double? = null,
    val parentCategoryId: Long? = null,
    val isDeleted: Boolean = false
)
