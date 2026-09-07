package sayan.apps.rupeeflow.core.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import sayan.apps.rupeeflow.feature.widget.BalanceWidget
import sayan.apps.rupeeflow.feature.widget.SpendingWidget
import sayan.apps.rupeeflow.feature.widget.UpcomingWidget
import javax.inject.Inject
import javax.inject.Singleton

interface WidgetUpdateCoordinator {
    suspend fun refreshBalance()
    suspend fun refreshSpending()
    suspend fun refreshUpcoming()
    suspend fun refreshAll()
}

@Singleton
class WidgetUpdateCoordinatorImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetUpdateCoordinator {

    override suspend fun refreshBalance() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(BalanceWidget::class.java)
            glanceIds.forEach { glanceId ->
                BalanceWidget().update(context, glanceId)
            }
        }
    }

    override suspend fun refreshSpending() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(SpendingWidget::class.java)
            glanceIds.forEach { glanceId ->
                SpendingWidget().update(context, glanceId)
            }
        }
    }

    override suspend fun refreshUpcoming() {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(UpcomingWidget::class.java)
            glanceIds.forEach { glanceId ->
                UpcomingWidget().update(context, glanceId)
            }
        }
    }

    override suspend fun refreshAll() {
        refreshBalance()
        refreshSpending()
        refreshUpcoming()
    }
}
