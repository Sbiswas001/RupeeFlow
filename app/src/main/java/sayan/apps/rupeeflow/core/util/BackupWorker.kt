package sayan.apps.rupeeflow.core.util

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import sayan.apps.rupeeflow.domain.repository.BackupRepository
import sayan.apps.rupeeflow.domain.repository.BackupResult
import sayan.apps.rupeeflow.domain.repository.UserPreferencesRepository
import java.util.concurrent.TimeUnit

@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authHelper: CredentialManagerHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = userPreferencesRepository.userPreferences.first()
        if (!preferences.automaticBackupEnabled) return Result.success()

        // We need an access token. For background workers, we typically use silent sign-in.
        // In this implementation, since we are using Credential Manager, we'll try to get an authorized account.
        val credential = authHelper.signInWithGoogle(applicationContext, filterByAuthorized = true)
        
        // Note: Credential Manager might require user interaction (prompt) even for authorized accounts 
        // if not configured for auto-select. This background worker might fail if user interaction is needed.
        // In a production app, you'd use OAuth2 refresh tokens for true background backup.
        
        return if (credential != null) {
            // We need a real access token from the ID Token or via AuthorizationClient.
            // This is a simplified placeholder as background OAuth token refresh requires more setup.
            Result.success()
        } else {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "daily_backup_work"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
        
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
