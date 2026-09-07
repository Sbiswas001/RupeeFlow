package sayan.apps.rupeeflow.core.database.converters

import androidx.room.TypeConverter
import sayan.apps.rupeeflow.core.database.entity.*
import sayan.apps.rupeeflow.domain.model.PaymentMethodType
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UPIApp

class DatabaseConverters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name
    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromAccountCategory(value: AccountCategory?): String? = value?.name
    @TypeConverter
    fun toAccountCategory(value: String?): AccountCategory? = value?.let { AccountCategory.valueOf(it) }

    @TypeConverter
    fun fromAccountSubType(value: AccountSubType): String = value.name
    @TypeConverter
    fun toAccountSubType(value: String): AccountSubType = AccountSubType.valueOf(value)

    @TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String = value.name
    @TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    @TypeConverter
    fun fromRecurringStatus(value: RecurringStatus): String = value.name
    @TypeConverter
    fun toRecurringStatus(value: String): RecurringStatus = RecurringStatus.valueOf(value)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name
    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)

    @TypeConverter
    fun fromOccurrenceStatus(value: OccurrenceStatus): String = value.name
    @TypeConverter
    fun toOccurrenceStatus(value: String): OccurrenceStatus = OccurrenceStatus.valueOf(value)

    @TypeConverter
    fun fromUPIApp(value: UPIApp?): String? = value?.name
    @TypeConverter
    fun toUPIApp(value: String?): UPIApp? = value?.let { UPIApp.valueOf(it) }

    @TypeConverter
    fun fromPaymentMethodType(value: PaymentMethodType?): String? = value?.name
    @TypeConverter
    fun toPaymentMethodType(value: String?): PaymentMethodType? = value?.let { PaymentMethodType.valueOf(it) }
}
