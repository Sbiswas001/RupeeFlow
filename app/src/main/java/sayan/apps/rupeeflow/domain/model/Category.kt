package sayan.apps.rupeeflow.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val type: TransactionType = TransactionType.EXPENSE,
    val budget: Double? = null,
    val isArchived: Boolean = false,
    val parentCategoryId: Long? = null
)
