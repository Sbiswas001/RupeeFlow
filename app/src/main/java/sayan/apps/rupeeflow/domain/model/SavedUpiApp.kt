package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SavedUpiApp(
    val id: Long = 0,
    val accountId: Long? = null,
    val appName: String,
    val appPackage: String? = null,
    val isCustom: Boolean = false,
    val isDefault: Boolean = false
)
