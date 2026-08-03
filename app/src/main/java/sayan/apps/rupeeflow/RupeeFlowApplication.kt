package sayan.apps.rupeeflow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import sayan.apps.rupeeflow.core.worker.RecurringTransactionWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltAndroidApp
class RupeeFlowApplication : Application(), Configuration.Provider {

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
        
        // Pre-populate with a default account and category if none exist
        // Note: Using GlobalScope for simplicity in a quick app init, 
        // in production use a better scoped approach or WorkManager.
        GlobalScope.launch {
            if (categoryRepository.getCategories().first().isEmpty()) {
                val defaultCategories = listOf(
                    // Expense
                    Category(name = "Food & Dining", icon = "🍔", colorHex = "#EF4444", type = TransactionType.EXPENSE),
                    Category(name = "Groceries", icon = "🛒", colorHex = "#F59E0B", type = TransactionType.EXPENSE),
                    Category(name = "Transport", icon = "🚕", colorHex = "#3B82F6", type = TransactionType.EXPENSE),
                    Category(name = "Fuel", icon = "⛽", colorHex = "#3B82F6", type = TransactionType.EXPENSE),
                    Category(name = "Rent & Housing", icon = "🏠", colorHex = "#10B981", type = TransactionType.EXPENSE),
                    Category(name = "Utilities", icon = "⚡", colorHex = "#F59E0B", type = TransactionType.EXPENSE),
                    Category(name = "Mobile & Internet", icon = "📱", colorHex = "#3B82F6", type = TransactionType.EXPENSE),
                    Category(name = "Entertainment", icon = "🎬", colorHex = "#8B5CF6", type = TransactionType.EXPENSE),
                    Category(name = "Shopping", icon = "🛍", colorHex = "#EC4899", type = TransactionType.EXPENSE),
                    Category(name = "Healthcare", icon = "🏥", colorHex = "#EF4444", type = TransactionType.EXPENSE),
                    Category(name = "Education", icon = "🎓", colorHex = "#6366F1", type = TransactionType.EXPENSE),
                    Category(name = "Travel", icon = "✈", colorHex = "#3B82F6", type = TransactionType.EXPENSE),
                    Category(name = "Gifts", icon = "🎁", colorHex = "#EC4899", type = TransactionType.EXPENSE),
                    Category(name = "Work", icon = "💼", colorHex = "#6B7280", type = TransactionType.EXPENSE),
                    Category(name = "Taxes", icon = "🧾", colorHex = "#6B7280", type = TransactionType.EXPENSE),
                    Category(name = "Other", icon = "📦", colorHex = "#6B7280", type = TransactionType.EXPENSE),
                    
                    // Income
                    Category(name = "Salary", icon = "💼", colorHex = "#10B981", type = TransactionType.INCOME),
                    Category(name = "Freelance", icon = "💰", colorHex = "#10B981", type = TransactionType.INCOME),
                    Category(name = "Interest", icon = "📈", colorHex = "#3B82F6", type = TransactionType.INCOME),
                    Category(name = "Gifts", icon = "🎁", colorHex = "#EC4899", type = TransactionType.INCOME),
                    Category(name = "Refunds", icon = "🏦", colorHex = "#10B981", type = TransactionType.INCOME),
                    Category(name = "Investments", icon = "💹", colorHex = "#10B981", type = TransactionType.INCOME),
                    Category(name = "Other", icon = "📦", colorHex = "#6B7280", type = TransactionType.INCOME)
                )
                defaultCategories.forEach { categoryRepository.addCategory(it) }
            }
            if (accountRepository.getAccounts().first().isEmpty()) {
                accountRepository.addAccount(
                    Account(
                        name = "Main Wallet",
                        category = "CASH_WALLETS",
                        subType = "WALLET",
                        balance = 0.0
                    )
                )
            }
        }

        setupRecurringWorker()
    }

    private fun setupRecurringWorker() {
        val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "RecurringTransactions",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
