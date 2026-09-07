package sayan.apps.rupeeflow.feature.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import dagger.hilt.android.EntryPointAccessors
import sayan.apps.rupeeflow.core.widget.GlanceEntryPoint

import kotlinx.coroutines.flow.firstOrNull

class MarkPaidActionCallback : ActionCallback {

    companion object {
        val PARAM_OCCURRENCE_ID = ActionParameters.Key<Long>("occurrence_id")
        val PARAM_ACCOUNT_ID = ActionParameters.Key<Long>("account_id")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val occurrenceId = parameters[PARAM_OCCURRENCE_ID] ?: return
        
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceEntryPoint::class.java
        )

        val accountRepository = entryPoint.accountRepository()
        val userPreferencesRepository = entryPoint.userPreferencesRepository()
        
        var accountId = parameters[PARAM_ACCOUNT_ID]
        if (accountId == null || accountId <= 0) {
            val userPrefs = runCatching { userPreferencesRepository.userPreferences.firstOrNull() }.getOrNull()
            if (userPrefs != null && userPrefs.defaultAccountId > 0) {
                accountId = userPrefs.defaultAccountId
            } else {
                val accounts = runCatching { accountRepository.getAccounts().firstOrNull() }.getOrNull()
                accountId = accounts?.firstOrNull()?.id ?: 1L
            }
        }

        runCatching {
            entryPoint.recurringRepository().markAsPaid(occurrenceId, accountId)
        }

        // Always refresh widgets to ensure UI stays consistent
        entryPoint.widgetUpdateCoordinator().refreshAll()
    }
}
