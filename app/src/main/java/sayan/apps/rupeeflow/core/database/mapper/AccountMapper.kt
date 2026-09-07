package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.AccountEntity
import sayan.apps.rupeeflow.core.database.entity.AccountSubType
import sayan.apps.rupeeflow.core.database.entity.AccountUpiAppEntity
import sayan.apps.rupeeflow.core.database.entity.DebitCardEntity
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.SavedUpiApp

fun AccountEntity.toDomainModel(): Account {
    return Account(
        id = id,
        name = name,
        category = category.name,
        subType = subType.name,
        balance = balance,
        institutionName = institutionName,
        accountNumberLast4 = accountNumberLast4,
        creditLimit = creditLimit,
        interestRate = interestRate,
        maturityDate = maturityDate,
        principalAmount = principalAmount,
        tenureMonths = tenureMonths,
        upiId = upiId,
        colorHex = colorHex,
        lastReconciledAt = lastReconciledAt,
        lastReconciledBalance = lastReconciledBalance,
        isClosed = isClosed,
        lastUpdated = lastUpdated
    )
}

fun Account.toEntity(): AccountEntity {
    return AccountEntity(
        id = id,
        name = name,
        category = AccountCategory.valueOf(category),
        subType = AccountSubType.valueOf(subType),
        balance = balance,
        institutionName = institutionName,
        accountNumberLast4 = accountNumberLast4,
        creditLimit = creditLimit,
        interestRate = interestRate,
        maturityDate = maturityDate,
        principalAmount = principalAmount,
        tenureMonths = tenureMonths,
        upiId = upiId,
        colorHex = colorHex,
        lastReconciledAt = lastReconciledAt,
        lastReconciledBalance = lastReconciledBalance,
        isClosed = isClosed,
        lastUpdated = lastUpdated
    )
}

fun DebitCardEntity.toDomainModel(): DebitCard {
    return DebitCard(
        id = id,
        accountId = accountId,
        cardName = cardName,
        last4Digits = last4Digits,
        network = network,
        nickname = nickname
    )
}

fun DebitCard.toEntity(): DebitCardEntity {
    return DebitCardEntity(
        id = id,
        accountId = accountId,
        cardName = cardName,
        last4Digits = last4Digits,
        network = network,
        nickname = nickname
    )
}

fun AccountUpiAppEntity.toDomainModel(): SavedUpiApp {
    return SavedUpiApp(
        id = id,
        accountId = accountId,
        appName = appName,
        appPackage = appPackage,
        isCustom = isCustom,
        isDefault = isDefault
    )
}

fun SavedUpiApp.toEntity(): AccountUpiAppEntity {
    return AccountUpiAppEntity(
        id = id,
        accountId = accountId,
        appName = appName,
        appPackage = appPackage,
        isCustom = isCustom,
        isDefault = isDefault
    )
}
