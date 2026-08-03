package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.entity.RecurrenceFrequency
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.domain.model.RecurringItem

fun RecurringEntity.toDomainModel(): RecurringItem {
    return RecurringItem(
        id = id,
        title = title,
        amount = amount,
        dueDate = dueDate,
        isAutoPay = isAutoPay,
        status = status.name,
        frequency = frequency.name,
        category = category,
        subCategory = subCategory,
        iconRes = iconRes,
        reminderDaysBefore = reminderDaysBefore,
        paymentMethod = paymentMethod,
        accountId = accountId,
        notes = notes,
        recurrenceCount = recurrenceCount,
        totalRecurrence = totalRecurrence,
        outstandingAmount = outstandingAmount,
        minimumDue = minimumDue,
        lastPaidDate = lastPaidDate
    )
}

fun RecurringItem.toEntity(): RecurringEntity {
    return RecurringEntity(
        id = id,
        title = title,
        amount = amount,
        dueDate = dueDate,
        isAutoPay = isAutoPay,
        status = RecurringStatus.valueOf(status),
        frequency = RecurrenceFrequency.valueOf(frequency),
        category = category,
        subCategory = subCategory,
        iconRes = iconRes,
        reminderDaysBefore = reminderDaysBefore,
        paymentMethod = paymentMethod,
        accountId = accountId,
        notes = notes,
        recurrenceCount = recurrenceCount,
        totalRecurrence = totalRecurrence,
        outstandingAmount = outstandingAmount,
        minimumDue = minimumDue,
        lastPaidDate = lastPaidDate
    )
}
