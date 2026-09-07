package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DebitCard(
    val id: Long = 0,
    val accountId: Long,
    val cardName: String,
    val last4Digits: String,
    val network: String? = null,
    val nickname: String? = null
)
