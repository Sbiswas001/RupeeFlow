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
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO categories (id, name, icon, colorHex, type, isArchived) VALUES (1, 'General', '📦', '#808080', 'EXPENSE', 0)")
            }
        })
        .fallbackToDestructiveMigration().build()
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
}
