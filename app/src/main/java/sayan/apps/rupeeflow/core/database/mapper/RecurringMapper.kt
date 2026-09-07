package sayan.apps.rupeeflow.core.database.mapper

import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.entity.OccurrenceStatus
import sayan.apps.rupeeflow.core.database.entity.RecurrenceFrequency
import sayan.apps.rupeeflow.core.database.entity.RecurringEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringOccurrenceEntity
import sayan.apps.rupeeflow.core.database.entity.RecurringStatus
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence

fun RecurringEntity.toDomainModel(occurrences: List<RecurringOccurrenceEntity> = emptyList()): RecurringItem {
    return RecurringItem(
        id = id,
        title = title,
        amount = amount,
        dueDate = dueDate,
        isAutoPay = isAutoPay,
        status = status.name,
        frequency = frequency.name,
        frequencyInterval = frequencyInterval,
        frequencyUnit = frequencyUnit,
        category = category,
        categoryId = categoryId,
        subCategory = subCategory,
        iconRes = iconRes,
        reminderDaysBefore = reminderDaysBefore,
        paymentMethod = paymentMethod,
        accountId = defaultAccountId,
        notes = notes,
        recurrenceCount = recurrenceCount,
        totalRecurrence = totalRecurrence,
        outstandingAmount = outstandingAmount,
        minimumDue = minimumDue,
        lastPaidDate = lastPaidDate,
        occurrences = occurrences.map { it.toDomainModel(this) }
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
        frequencyInterval = frequencyInterval,
        frequencyUnit = frequencyUnit,
        category = category,
        categoryId = categoryId,
        subCategory = subCategory,
        iconRes = iconRes,
        reminderDaysBefore = reminderDaysBefore,
        paymentMethod = paymentMethod,
        defaultAccountId = accountId,
        notes = notes,
        recurrenceCount = recurrenceCount,
        totalRecurrence = totalRecurrence,
        outstandingAmount = outstandingAmount,
        minimumDue = minimumDue,
        lastPaidDate = lastPaidDate
    )
}

fun RecurringOccurrenceEntity.toDomainModel(item: RecurringEntity? = null): RecurringOccurrence {
    return RecurringOccurrence(
        id = id,
        recurringItemId = recurringItemId,
        scheduledDate = scheduledDate,
        status = status.name,
        transactionId = transactionId,
        paymentDate = paymentDate,
        accountId = accountId,
        amount = item?.amount,
        notes = notes,
        recurringItemNameSnapshot = recurringItemNameSnapshot,
        accountNameSnapshot = accountNameSnapshot,
        parentAccountId = item?.defaultAccountId,
        parentAccountName = null // We don't have account name here, will fetch in VM or snapshot
    )
}

fun UtilityDao.OccurrenceWithItem.toDomainModel(): RecurringOccurrence {
    return occurrence.toDomainModel(item)
}

fun RecurringOccurrence.toEntity(): RecurringOccurrenceEntity {
    return RecurringOccurrenceEntity(
        id = id,
        recurringItemId = recurringItemId,
        scheduledDate = scheduledDate,
        status = OccurrenceStatus.valueOf(status),
        transactionId = transactionId,
        paymentDate = paymentDate,
        accountId = accountId,
        notes = notes,
        recurringItemNameSnapshot = recurringItemNameSnapshot,
        accountNameSnapshot = accountNameSnapshot
    )
}
