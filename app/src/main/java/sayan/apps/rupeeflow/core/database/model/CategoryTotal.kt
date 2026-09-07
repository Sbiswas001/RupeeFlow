package sayan.apps.rupeeflow.core.database.model

data class CategoryTotal(
    val categoryId: Long? = null,
    val categoryName: String?,
    val colorHex: String?,
    val totalAmount: Double
)
