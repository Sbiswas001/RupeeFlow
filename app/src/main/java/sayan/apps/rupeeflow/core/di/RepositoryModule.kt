package sayan.apps.rupeeflow.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sayan.apps.rupeeflow.core.database.repository.AccountRepositoryImpl
import sayan.apps.rupeeflow.core.database.repository.CategoryRepositoryImpl
import sayan.apps.rupeeflow.core.database.repository.PlanningRepositoryImpl
import sayan.apps.rupeeflow.core.database.repository.RecurringRepositoryImpl
import sayan.apps.rupeeflow.core.database.repository.TransactionRepositoryImpl
import sayan.apps.rupeeflow.core.datastore.UserPreferencesRepositoryImpl
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.core.database.repository.BackupRepositoryImpl
import sayan.apps.rupeeflow.core.database.repository.SearchRepositoryImpl
import sayan.apps.rupeeflow.domain.repository.BackupRepository
import sayan.apps.rupeeflow.domain.repository.SearchRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: TransactionRepositoryImpl
    ): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        accountRepositoryImpl: AccountRepositoryImpl
    ): AccountRepository

    @Binds
    @Singleton
    abstract fun bindPlanningRepository(
        planningRepositoryImpl: PlanningRepositoryImpl
    ): PlanningRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        categoryRepositoryImpl: CategoryRepositoryImpl
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRepository(
        recurringRepositoryImpl: RecurringRepositoryImpl
    ): RecurringRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: UserPreferencesRepositoryImpl
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        backupRepositoryImpl: BackupRepositoryImpl
    ): BackupRepository
}
