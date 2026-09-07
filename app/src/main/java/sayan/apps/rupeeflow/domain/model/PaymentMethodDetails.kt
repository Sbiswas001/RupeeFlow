package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PaymentMethodType {
    UPI,
    DEBIT_CARD
}

@Serializable
sealed class PaymentMethodDetails {
    @Serializable
    data class UPI(
        val upiAppName: String?,
        val transactionId: String?
    ) : PaymentMethodDetails()

    @Serializable
    data class DebitCard(
        val cardId: Long? = null,
        val cardName: String?,
        val last4Digits: String?,
        val network: String? = null
    ) : PaymentMethodDetails()
}
