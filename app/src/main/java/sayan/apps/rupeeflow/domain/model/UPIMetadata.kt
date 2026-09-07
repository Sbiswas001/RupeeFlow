package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class UPIApp {
    GOOGLE_PAY,
    PHONEPE,
    PAYTM,
    AMAZON_PAY,
    BHIM,
    OTHER
}

@Serializable
data class UPIMetadata(
    val transactionId: String? = null,
    val app: UPIApp? = null,
    val linkedBank: String? = null,
    val upiAppNameSnapshot: String? = null
)
