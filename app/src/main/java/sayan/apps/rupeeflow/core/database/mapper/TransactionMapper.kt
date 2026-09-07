package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.dao.TransactionWithCategory
import sayan.apps.rupeeflow.core.database.entity.TransactionEntity
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UPIMetadata
import sayan.apps.rupeeflow.core.database.entity.AccountCategory

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
        type = transaction.type,
        accountId = transaction.accountId,
        note = transaction.note,
        upiMetadata = if (transaction.upiApp != null || transaction.upiTransactionId != null || transaction.upiAppNameSnapshot != null) {
            UPIMetadata(
                transactionId = transaction.upiTransactionId,
                app = transaction.upiApp,
                linkedBank = transaction.upiLinkedBank,
                upiAppNameSnapshot = transaction.upiAppNameSnapshot
            )
        } else null,
        previousBalance = transaction.previousBalance,
        actualBalance = transaction.actualBalance,
        reconciliationReason = transaction.reconciliationReason,
        accountNameSnapshot = transaction.accountNameSnapshot,
        accountCategorySnapshot = transaction.accountCategorySnapshot?.name,
        transferId = transaction.transferId,
        transferAccountId = transaction.transferAccountId,
        transferAccountNameSnapshot = transaction.transferAccountNameSnapshot,
        paymentMethodType = transaction.paymentMethodType,
        debitCardId = transaction.debitCardId,
        debitCardNameSnapshot = transaction.debitCardNameSnapshot,
        debitCardLast4Snapshot = transaction.debitCardLast4Snapshot
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
        type = type,
        accountId = accountId,
        note = note,
        upiMetadata = if (upiApp != null || upiTransactionId != null || upiAppNameSnapshot != null) {
            UPIMetadata(
                transactionId = upiTransactionId,
                app = upiApp,
                linkedBank = upiLinkedBank,
                upiAppNameSnapshot = upiAppNameSnapshot
            )
        } else null,
        previousBalance = previousBalance,
        actualBalance = actualBalance,
        reconciliationReason = reconciliationReason,
        accountNameSnapshot = accountNameSnapshot,
        accountCategorySnapshot = accountCategorySnapshot?.name,
        transferId = transferId,
        transferAccountId = transferAccountId,
        transferAccountNameSnapshot = transferAccountNameSnapshot,
        paymentMethodType = paymentMethodType,
        debitCardId = debitCardId,
        debitCardNameSnapshot = debitCardNameSnapshot,
        debitCardLast4Snapshot = debitCardLast4Snapshot
    )
}

fun Transaction.toEntity(accountId: Long?, categoryId: Long?): TransactionEntity {
    return TransactionEntity(
        id = if (id.isEmpty()) 0 else id.toLong(),
        title = title,
        amount = amount,
        type = type,
        categoryId = categoryId,
        accountId = accountId,
        timestamp = timestamp,
        note = note,
        upiTransactionId = upiMetadata?.transactionId,
        upiApp = upiMetadata?.app,
        upiLinkedBank = upiMetadata?.linkedBank,
        previousBalance = previousBalance,
        actualBalance = actualBalance,
        reconciliationReason = reconciliationReason,
        accountNameSnapshot = accountNameSnapshot,
        accountCategorySnapshot = accountCategorySnapshot?.let { AccountCategory.valueOf(it) },
        transferId = transferId,
        transferAccountId = transferAccountId,
        transferAccountNameSnapshot = transferAccountNameSnapshot,
        isIncoming = isIncoming,
        paymentMethodType = paymentMethodType,
        debitCardId = debitCardId,
        debitCardNameSnapshot = debitCardNameSnapshot,
        debitCardLast4Snapshot = debitCardLast4Snapshot,
        upiAppNameSnapshot = upiMetadata?.upiAppNameSnapshot
    )
}
