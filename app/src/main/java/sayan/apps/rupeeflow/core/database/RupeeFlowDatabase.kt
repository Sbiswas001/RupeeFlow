package sayan.apps.rupeeflow.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import sayan.apps.rupeeflow.core.database.converters.DatabaseConverters
import sayan.apps.rupeeflow.core.database.dao.*
import sayan.apps.rupeeflow.core.database.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        CategoryEntity::class,
        RecurringEntity::class,
        RecurringOccurrenceEntity::class,
        GoalEntity::class,
        GoalContributionEntity::class,
        AttachmentEntity::class,
        RecentSearchEntity::class,
        DebitCardEntity::class,
        AccountUpiAppEntity::class
    ],
    version = 25,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class RupeeFlowDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun planningDao(): PlanningDao
    abstract fun utilityDao(): UtilityDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun debitCardDao(): DebitCardDao
    abstract fun accountUpiAppDao(): AccountUpiAppDao
}
