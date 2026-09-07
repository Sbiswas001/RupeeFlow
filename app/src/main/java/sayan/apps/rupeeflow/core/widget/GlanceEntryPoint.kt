package sayan.apps.rupeeflow.core.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.PlanningRepository
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlanceEntryPoint {
    fun accountRepository(): AccountRepository
    fun transactionRepository(): TransactionRepository
    fun planningRepository(): PlanningRepository
    fun recurringRepository(): RecurringRepository
    fun userPreferencesRepository(): UserPreferencesRepository
    fun widgetUpdateCoordinator(): WidgetUpdateCoordinator
}
