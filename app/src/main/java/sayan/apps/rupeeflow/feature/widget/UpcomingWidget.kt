package sayan.apps.rupeeflow.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import sayan.apps.rupeeflow.core.widget.GlanceEntryPoint
import sayan.apps.rupeeflow.core.widget.WidgetFormattingUtils
import sayan.apps.rupeeflow.domain.model.RecurringOccurrence
import sayan.apps.rupeeflow.domain.model.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UpcomingWidget : GlanceAppWidget() {

    companion object {
        private val TINY = DpSize(100.dp, 100.dp)
        private val COMPACT = DpSize(180.dp, 120.dp)
        private val TALL_WIDE = DpSize(220.dp, 180.dp)
    }

    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(TINY, COMPACT, TALL_WIDE)
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlanceEntryPoint::class.java
        )

        val userPrefs = runCatching {
            entryPoint.userPreferencesRepository().userPreferences.firstOrNull()
        }.getOrNull() ?: UserPreferences()

        val occurrences: List<RecurringOccurrence> = runCatching {
            entryPoint.recurringRepository().getUpcomingOccurrences(2).firstOrNull()
        }.getOrNull() ?: emptyList()

        provideContent {
            GlanceTheme {
                UpcomingWidgetContent(
                    occurrences = occurrences,
                    userPreferences = userPrefs
                )
            }
        }
    }
}

@Composable
private fun UpcomingWidgetContent(
    occurrences: List<RecurringOccurrence>,
    userPreferences: UserPreferences
) {
    val context = LocalContext.current
    val size = LocalSize.current

    val textPrimary = GlanceTheme.colors.onSurface
    val textSecondary = GlanceTheme.colors.outline
    val accentColor = GlanceTheme.colors.primary

    val isTiny = size.width < 140.dp || size.height < 110.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(WidgetFormattingUtils.createDeepLinkIntent(context, "Recurring")))
    ) {
        if (isTiny) {
            val firstOcc = occurrences.firstOrNull()
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (firstOcc != null) {
                    val title = firstOcc.recurringItemNameSnapshot ?: "Bill"
                    val amount = firstOcc.amount ?: 0.0
                    Text(
                        text = title,
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = WidgetFormattingUtils.formatWidgetAmount(amount, userPreferences, false),
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                } else {
                    Text(
                        text = "Upcoming",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = "None",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )
                }
            }
        } else {
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upcoming",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (occurrences.isEmpty()) {
                    Text(
                        text = "No upcoming payments",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )
                } else {
                    occurrences.forEachIndexed { index, occurrence ->
                        val title = occurrence.recurringItemNameSnapshot ?: "Bill"
                        val amount = occurrence.amount ?: 0.0
                        val formattedAmount = WidgetFormattingUtils.formatWidgetAmount(amount, userPreferences, false)
                        val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(occurrence.scheduledDate))

                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = title,
                                    style = TextStyle(
                                        color = textPrimary,
                                        fontSize = if (index == 0) 17.sp else 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = GlanceModifier.defaultWeight())
                                Text(
                                    text = formattedAmount,
                                    style = TextStyle(
                                        color = textPrimary,
                                        fontSize = if (index == 0) 17.sp else 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Row(
                                modifier = GlanceModifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateStr,
                                    style = TextStyle(
                                        color = textSecondary,
                                        fontSize = 14.sp
                                    )
                                )

                                // Only the nearest (first) item gets the "✓ Mark paid" action button
                                if (index == 0) {
                                    Spacer(modifier = GlanceModifier.defaultWeight())
                                    Text(
                                        text = "✓ Mark paid",
                                        style = TextStyle(
                                            color = accentColor,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = GlanceModifier
                                            .clickable(
                                                actionRunCallback<MarkPaidActionCallback>(
                                                    actionParametersOf(
                                                        MarkPaidActionCallback.PARAM_OCCURRENCE_ID to occurrence.id,
                                                        MarkPaidActionCallback.PARAM_ACCOUNT_ID to (occurrence.accountId ?: 0L)
                                                    )
                                                )
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (index < occurrences.size - 1) {
                            Spacer(modifier = GlanceModifier.height(4.dp))
                        }
                    }
                }
            }
        }
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}
