package sayan.apps.rupeeflow.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import sayan.apps.rupeeflow.core.database.dao.UtilityDao
import sayan.apps.rupeeflow.core.database.mapper.toDomainModel
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.core.util.NotificationHelper
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

@HiltWorker
class RecurringTransactionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val recurringRepository: RecurringRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val utilityDao: UtilityDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val dueItems = utilityDao.getDueRecurringItemsSync(currentTime)

        dueItems.forEach { entity ->
            val item = entity.toDomainModel()
            val preferences = userPreferencesRepository.userPreferences.first()
            val formattedAmount = CurrencyFormatter.format(item.amount, preferences)
            
            if (item.isAutoPay) {
                // For simplicity, assume 'Main Wallet' (id=1) for AutoPay
                recurringRepository.markAsPaid(item, accountId = 1L)
                NotificationHelper.showPaymentNotification(
                    applicationContext,
                    "AutoPay Successful",
                    "$formattedAmount paid for ${item.title}"
                )
            } else {
                NotificationHelper.showPaymentNotification(
                    applicationContext,
                    "Payment Due",
                    "$formattedAmount due for ${item.title}"
                )
            }
        }

        return Result.success()
    }
}
