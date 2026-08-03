package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.AttachmentEntity
import sayan.apps.rupeeflow.domain.model.Attachment

fun AttachmentEntity.toDomainModel(): Attachment {
    return Attachment(
        id = id,
        transactionId = transactionId,
        filePath = filePath,
        fileType = fileType
    )
}

fun Attachment.toEntity(): AttachmentEntity {
    return AttachmentEntity(
        id = id,
        transactionId = transactionId,
        filePath = filePath,
        fileType = fileType
    )
}
