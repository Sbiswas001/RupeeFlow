package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.BudgetEntity
import sayan.apps.rupeeflow.core.database.entity.BudgetPeriod
import sayan.apps.rupeeflow.core.database.entity.GoalEntity
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.Goal

fun BudgetEntity.toDomainModel(categoryName: String = "Category", spentAmount: Double = 0.0): Budget {
    return Budget(
        id = id,
        categoryId = categoryId,
        categoryName = categoryName,
        limitAmount = limitAmount,
        spentAmount = spentAmount,
        period = period.name,
        startDate = startDate
    )
}

fun Budget.toEntity(): BudgetEntity {
    return BudgetEntity(
        id = id,
        categoryId = categoryId,
        limitAmount = limitAmount,
        period = BudgetPeriod.valueOf(period),
        startDate = startDate
    )
}

fun GoalEntity.toDomainModel(): Goal {
    return Goal(
        id = id,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate
    )
}

fun Goal.toEntity(): GoalEntity {
    return GoalEntity(
        id = id,
        title = title,
        targetAmount = targetAmount,
        currentAmount = currentAmount,
        targetDate = targetDate
    )
}
