package sayan.apps.rupeeflow.core.ai.model

data class ModelConfig(
    val id: String,
    val displayName: String,
    val version: String,
    val fileName: String,
    val sizeBytes: Long,
    val checksumSha256: String? = null,
    val downloadUrl: String? = null
)
