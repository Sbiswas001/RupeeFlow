package sayan.apps.rupeeflow

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.worker.BudgetGoalWorker
import sayan.apps.rupeeflow.core.worker.RecurringTransactionWorker
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import javax.inject.Inject

@HiltAndroidApp
class RupeeFlowApplication : Application(), Configuration.Provider {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var accountRepository: AccountRepository

    @Inject
    lateinit var categoryRepository: CategoryRepository

    override fun onCreate() {
        super.onCreate()
        
        // Pre-populate with default account and category if none exist
        applicationScope.launch {
            if (categoryRepository.getCategories().first().isEmpty()) {
                val defaultCategories = listOf(
                    Category(name = "Others", icon = "📦", colorHex = "#6B7280", type = TransactionType.EXPENSE),
                    Category(name = "Salary", icon = "💼", colorHex = "#10B981", type = TransactionType.INCOME)
                )
                defaultCategories.forEach { categoryRepository.addCategory(it) }
            }
            if (accountRepository.getAccounts().first().isEmpty()) {
                accountRepository.addAccount(
                    Account(
                        name = "Cash",
                        category = "CASH_WALLETS",
                        subType = "CASH",
                        balance = 0.0
                    )
                )
            }
        }

        setupRecurringWorker()
        BudgetGoalWorker.schedule(this)
    }

    private fun setupRecurringWorker() {
        // Use KEEP policy so we don't re-enqueue redundant work on every cold launch
        val workRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "RecurringTransactionWorker_startup",
            ExistingWorkPolicy.KEEP,
            workRequest
        )
        RecurringTransactionWorker.schedulePeriodic(this)
    }
}
