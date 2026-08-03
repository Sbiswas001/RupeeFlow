package sayan.apps.rupeeflow.domain.model

data class Attachment(
    val id: Long = 0,
    val transactionId: Long,
    val filePath: String,
    val fileType: String
)
