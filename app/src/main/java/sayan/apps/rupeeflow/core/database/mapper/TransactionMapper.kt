package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.dao.TransactionWithCategory
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UPIMetadata

fun TransactionWithCategory.toDomainModel(): Transaction {
    return Transaction(
        id = transaction.id.toString(),
        title = transaction.title,
        amount = transaction.amount,
        timestamp = transaction.timestamp,
        category = category?.name ?: "General",
        categoryId = transaction.categoryId,
        categoryIcon = category?.icon,
        categoryColor = category?.colorHex,
        isIncome = transaction.type == TransactionType.INCOME,
        accountId = transaction.accountId,
        note = transaction.note,
        upiMetadata = if (transaction.upiApp != null) {
            UPIMetadata(
                transactionId = transaction.upiTransactionId,
                app = transaction.upiApp,
                linkedBank = transaction.upiLinkedBank
            )
        } else null
    )
}

fun TransactionEntity.toDomainModel(): Transaction {
    return Transaction(
        id = id.toString(),
        title = title,
        amount = amount,
        timestamp = timestamp,
        category = "Category", // Needs actual category name from join
        categoryId = categoryId,
        isIncome = type == TransactionType.INCOME,
        accountId = accountId,
        note = note,
        upiMetadata = if (upiApp != null) {
            UPIMetadata(
                transactionId = upiTransactionId,
                app = upiApp,
                linkedBank = upiLinkedBank
            )
        } else null
    )
}

fun Transaction.toEntity(accountId: Long, categoryId: Long?): TransactionEntity {
    return TransactionEntity(
        id = if (id.isEmpty()) 0 else id.toLong(),
        title = title,
        amount = amount,
        type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
        categoryId = categoryId,
        accountId = accountId,
        merchantId = null,
        timestamp = timestamp,
        note = note,
        upiTransactionId = upiMetadata?.transactionId,
        upiApp = upiMetadata?.app,
        upiLinkedBank = upiMetadata?.linkedBank
    )
}
