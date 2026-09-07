package sayan.apps.rupeeflow.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import sayan.apps.rupeeflow.core.database.RupeeFlowDatabase
import sayan.apps.rupeeflow.core.database.RupeeFlowMigrations
import sayan.apps.rupeeflow.core.database.dao.*
import sayan.apps.rupeeflow.core.util.Constants
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RupeeFlowDatabase {
        return Room.databaseBuilder(
            context,
            RupeeFlowDatabase::class.java,
            Constants.DATABASE_NAME
        )
        .addMigrations(*RupeeFlowMigrations.ALL_MIGRATIONS)
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO categories (id, name, icon, colorHex, type) VALUES (1, 'Others', '📦', '#6B7280', 'EXPENSE')")
                db.execSQL("INSERT INTO categories (id, name, icon, colorHex, type) VALUES (2, 'Salary', '💼', '#10B981', 'INCOME')")
                db.execSQL("INSERT INTO accounts (id, name, category, subType, balance, lastUpdated) VALUES (1, 'Cash', 'CASH_WALLETS', 'CASH', 0.0, ${System.currentTimeMillis()})")
                db.execSQL("INSERT OR IGNORE INTO account_upi_apps (appName, isCustom, isDefault) VALUES ('Google Pay', 0, 1)")
                db.execSQL("INSERT OR IGNORE INTO account_upi_apps (appName, isCustom, isDefault) VALUES ('PhonePe', 0, 1)")
                db.execSQL("INSERT OR IGNORE INTO account_upi_apps (appName, isCustom, isDefault) VALUES ('Paytm', 0, 1)")
                db.execSQL("INSERT OR IGNORE INTO account_upi_apps (appName, isCustom, isDefault) VALUES ('Amazon Pay', 0, 1)")
                db.execSQL("INSERT OR IGNORE INTO account_upi_apps (appName, isCustom, isDefault) VALUES ('BHIM', 0, 1)")
            }
        })
        .build()
    }

    @Provides
    fun provideTransactionDao(database: RupeeFlowDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideAccountDao(database: RupeeFlowDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: RupeeFlowDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun providePlanningDao(database: RupeeFlowDatabase): PlanningDao = database.planningDao()

    @Provides
    fun provideUtilityDao(database: RupeeFlowDatabase): UtilityDao = database.utilityDao()

    @Provides
    fun provideRecentSearchDao(database: RupeeFlowDatabase): RecentSearchDao = database.recentSearchDao()

    @Provides
    fun provideDebitCardDao(database: RupeeFlowDatabase): DebitCardDao = database.debitCardDao()

    @Provides
    fun provideAccountUpiAppDao(database: RupeeFlowDatabase): AccountUpiAppDao = database.accountUpiAppDao()
}
