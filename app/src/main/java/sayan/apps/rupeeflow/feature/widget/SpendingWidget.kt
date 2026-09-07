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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
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
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.UserPreferences
import java.util.Calendar
import java.util.Locale

class SpendingWidget : GlanceAppWidget() {

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

        val budgets: List<BudgetWithProgress> = runCatching {
            entryPoint.planningRepository().getBudgetsWithProgress().firstOrNull()
        }.getOrNull() ?: emptyList()

        val totalLimit = budgets.sumOf { it.budget.limitAmount }
        val totalSpent = budgets.sumOf { it.budget.spentAmount }

        provideContent {
            GlanceTheme {
                SpendingWidgetContent(
                    totalLimit = totalLimit,
                    totalSpent = totalSpent,
                    userPreferences = userPrefs
                )
            }
        }
    }
}

@Composable
private fun SpendingWidgetContent(
    totalLimit: Double,
    totalSpent: Double,
    userPreferences: UserPreferences
) {
    val context = LocalContext.current
    val size = LocalSize.current

    val monthName = Calendar.getInstance().getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("en", "IN")) ?: "This Month"
    val progress = if (totalLimit > 0) (totalSpent / totalLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val remaining = (totalLimit - totalSpent).coerceAtLeast(0.0)
    val percentage = (progress * 100).toInt()

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
            .clickable(actionStartActivity(WidgetFormattingUtils.createDeepLinkIntent(context, "Analytics")))
    ) {
        if (isTiny) {
            // Tiny Responsive View: Key number + "Spent" label
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = WidgetFormattingUtils.formatWidgetAmount(totalSpent, userPreferences, false),
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Spent",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            // Compact / Full Responsive View: Header with attached '+', Amounts, Progress Bar
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header Row with attached '+' quick action
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = monthName,
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    
                    Text(
                        text = "＋",
                        style = TextStyle(
                            color = accentColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = GlanceModifier
                            .clickable(actionStartActivity(WidgetFormattingUtils.createDeepLinkIntent(context, "AddTransaction")))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (totalLimit <= 0.0) {
                    Text(
                        text = "No budget set",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Tap to create a budget",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )
                } else {
                    // Spending Amount & Remaining
                    Text(
                        text = "${WidgetFormattingUtils.formatWidgetAmount(totalSpent, userPreferences, false)} spent",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = "${WidgetFormattingUtils.formatWidgetAmount(remaining, userPreferences, false)} remaining",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(8.dp))

                    // Progress Bar + Percentage Text Row
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier.defaultWeight().height(8.dp),
                            color = accentColor
                        )

                        Spacer(modifier = GlanceModifier.padding(start = 8.dp))

                        Text(
                            text = "$percentage%",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

class SpendingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SpendingWidget()
}
