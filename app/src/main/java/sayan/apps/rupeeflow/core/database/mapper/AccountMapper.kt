package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.AccountCategory
import sayan.apps.rupeeflow.core.database.entity.AccountEntity
import sayan.apps.rupeeflow.core.database.entity.AccountSubType
import sayan.apps.rupeeflow.domain.model.Account

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
        lastUpdated = lastUpdated
    )
}
