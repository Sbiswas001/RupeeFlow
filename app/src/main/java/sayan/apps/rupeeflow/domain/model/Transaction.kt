package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val category: String,
    val categoryId: Long? = null,
    val categoryIcon: String? = null,
    val categoryColor: String? = null,
    val isIncome: Boolean,
    val accountId: Long = 0,
    val note: String? = null,
    val upiMetadata: UPIMetadata? = null
)
