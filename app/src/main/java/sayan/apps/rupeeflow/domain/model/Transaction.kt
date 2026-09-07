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
    val type: TransactionType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
    val accountId: Long? = null,
    val note: String? = null,
    val upiMetadata: UPIMetadata? = null,
    val previousBalance: Double? = null,
    val actualBalance: Double? = null,
    val reconciliationReason: String? = null,
    val accountNameSnapshot: String? = null,
    val accountCategorySnapshot: String? = null,
    val transferId: String? = null,
    val transferAccountId: Long? = null,
    val transferAccountNameSnapshot: String? = null,
    val isIncoming: Boolean = false,
    val paymentMethodType: PaymentMethodType? = null,
    val debitCardId: Long? = null,
    val debitCardNameSnapshot: String? = null,
    val debitCardLast4Snapshot: String? = null
)
