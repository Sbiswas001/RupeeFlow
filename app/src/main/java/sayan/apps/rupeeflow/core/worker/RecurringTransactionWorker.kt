package sayan.apps.rupeeflow.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinator
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recurringRepository: RecurringRepository,
    private val widgetUpdateCoordinator: WidgetUpdateCoordinator
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        recurringRepository.processDueRecurringTransactions()
        widgetUpdateCoordinator.refreshAll()
        return Result.success()
    }

    companion object {
        fun enqueueOneTime(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "RecurringTransactionsOneTime",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun schedulePeriodic(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<RecurringTransactionWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "RecurringTransactions",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
