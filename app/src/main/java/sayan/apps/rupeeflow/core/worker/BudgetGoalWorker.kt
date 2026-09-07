package sayan.apps.rupeeflow.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import sayan.apps.rupeeflow.core.database.dao.CategoryDao
import sayan.apps.rupeeflow.core.database.dao.PlanningDao
import sayan.apps.rupeeflow.core.database.dao.TransactionDao
import sayan.apps.rupeeflow.core.database.entity.BudgetPeriod
import sayan.apps.rupeeflow.core.util.DateUtils
import sayan.apps.rupeeflow.core.util.NotificationHelper
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy

import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinator

@HiltWorker
class BudgetGoalWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val planningDao: PlanningDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val widgetUpdateCoordinator: WidgetUpdateCoordinator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val preferences = userPreferencesRepository.userPreferences.first()
        
        if (preferences.budgetAlerts) {
            checkBudgets()
        }
        
        if (preferences.goalReminders) {
            checkGoals()
        }

        widgetUpdateCoordinator.refreshSpending()

        return Result.success()
    }

    private suspend fun checkBudgets() {
        val budgets = planningDao.getAllBudgetsSync()
        val now = System.currentTimeMillis()

        budgets.forEach { budget ->
            val periodStart = when (budget.period) {
                BudgetPeriod.WEEKLY -> DateUtils.getStartOfWeek()
                BudgetPeriod.MONTHLY -> DateUtils.getStartOfMonth(now)
            }
            
            // Reset flags if new period
            var currentBudget = if (budget.lastNotifiedPeriodStart != periodStart) {
                budget.copy(
                    notified80Percent = false,
                    notified100Percent = false,
                    lastNotifiedPeriodStart = periodStart
                )
            } else {
                budget
            }

            val spent = Math.abs(transactionDao.getCategorySpendingInRange(
                currentBudget.categoryId,
                periodStart,
                Long.MAX_VALUE
            ) ?: 0.0)

            val limit = currentBudget.limitAmount
            if (limit <= 0) return@forEach

            val usageRatio = spent / limit
            val category = categoryDao.getCategoryById(currentBudget.categoryId)
            val categoryName = category?.name ?: "Category"

            if (usageRatio >= 1.0 && !currentBudget.notified100Percent) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Budget Exceeded",
                    "You've spent 100% of your $categoryName budget!"
                )
                currentBudget = currentBudget.copy(notified100Percent = true)
            } else if (usageRatio >= 0.8 && !currentBudget.notified80Percent) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Budget Alert",
                    "You've reached 80% of your $categoryName budget."
                )
                currentBudget = currentBudget.copy(notified80Percent = true)
            }

            if (currentBudget != budget) {
                planningDao.updateBudget(currentBudget)
            }
        }
    }

    private suspend fun checkGoals() {
        val goals = planningDao.getAllGoalsSync()

        goals.forEach { goal ->
            if (goal.targetAmount <= 0) return@forEach
            
            val progressRatio = goal.currentAmount / goal.targetAmount
            var currentGoal = goal

            if (progressRatio >= 1.0 && !goal.notified100Percent) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Goal Reached! 🎉",
                    "Congratulations! You've reached your goal: ${goal.title}"
                )
                currentGoal = currentGoal.copy(notified100Percent = true)
            } else if (progressRatio >= 0.9 && !goal.notified90Percent) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Goal Almost There",
                    "You're at 90% of your ${goal.title} goal. Keep going!"
                )
                currentGoal = currentGoal.copy(notified90Percent = true)
            } else if (progressRatio >= 0.5 && !goal.notified50Percent) {
                NotificationHelper.showNotification(
                    applicationContext,
                    "Goal Milestone",
                    "You've reached 50% of your ${goal.title} goal!"
                )
                currentGoal = currentGoal.copy(notified50Percent = true)
            }

            if (currentGoal != goal) {
                planningDao.updateGoal(currentGoal)
            }
        }
    }

    companion object {
        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<BudgetGoalWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BudgetGoalAlerts",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
