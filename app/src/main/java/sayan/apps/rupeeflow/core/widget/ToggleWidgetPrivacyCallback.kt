package sayan.apps.rupeeflow.core.widget

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import dagger.hilt.android.EntryPointAccessors

class ToggleWidgetPrivacyCallback : ActionCallback {

    companion object {
        val KEY_PRIVACY_LOCKED = booleanPreferencesKey("widget_privacy_locked")
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val currentLocked = prefs[KEY_PRIVACY_LOCKED] ?: false
            prefs.toMutablePreferences().apply {
                this[KEY_PRIVACY_LOCKED] = !currentLocked
            }
        }
        
        // Refresh specific widget
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceEntryPoint::class.java
        )
        entryPoint.widgetUpdateCoordinator().refreshAll()
    }
}
