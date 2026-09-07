package sayan.apps.rupeeflow.feature.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
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
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.firstOrNull
import sayan.apps.rupeeflow.R
import sayan.apps.rupeeflow.core.widget.GlanceEntryPoint
import sayan.apps.rupeeflow.core.widget.ToggleWidgetPrivacyCallback
import sayan.apps.rupeeflow.core.widget.WidgetFormattingUtils
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.UserPreferences

class BalanceWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

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

        val accounts: List<Account> = runCatching {
            entryPoint.accountRepository().getAccounts().firstOrNull()
        }.getOrNull() ?: emptyList()

        val activeAccounts = accounts.filter { !it.isClosed }
        val totalBalance = activeAccounts.sumOf { it.balance }

        provideContent {
            GlanceTheme {
                val prefs = currentState<Preferences>()
                val instancePrivacyLocked = prefs[ToggleWidgetPrivacyCallback.KEY_PRIVACY_LOCKED] ?: false
                val shouldMask = WidgetFormattingUtils.evaluateShouldMask(userPrefs.hideBalances, instancePrivacyLocked)

                BalanceWidgetContent(
                    totalBalance = totalBalance,
                    accountCount = activeAccounts.size,
                    userPreferences = userPrefs,
                    shouldMask = shouldMask
                )
            }
        }
    }
}

@Composable
private fun BalanceWidgetContent(
    totalBalance: Double,
    accountCount: Int,
    userPreferences: UserPreferences,
    shouldMask: Boolean
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val formattedAmount = WidgetFormattingUtils.formatWidgetAmount(totalBalance, userPreferences, shouldMask)

    val surfaceColor = GlanceTheme.colors.widgetBackground
    val textPrimary = GlanceTheme.colors.onSurface
    val textSecondary = GlanceTheme.colors.outline
    val accentColor = GlanceTheme.colors.primary

    val isTiny = size.width < 140.dp || size.height < 110.dp

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(surfaceColor)
            .cornerRadius(16.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(WidgetFormattingUtils.createDeepLinkIntent(context, "Accounts")))
    ) {
        if (isTiny) {
            // Tiny Responsive View: High-prominence key number + simple label
            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedAmount,
                    style = TextStyle(
                        color = textPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "Balance",
                    style = TextStyle(
                        color = textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        } else {
            // Compact / Full Responsive View: Header, Large amount, Footer with attached '+' button
            Column(
                modifier = GlanceModifier.fillMaxSize()
            ) {
                // Header Row
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RupeeFlow",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.defaultWeight())
                    
                    // Privacy Toggle Button (Image provider for clean vector icon)
                    Image(
                        provider = ImageProvider(
                            if (shouldMask) R.drawable.ic_visibility else R.drawable.ic_visibility_off
                        ),
                        contentDescription = "Toggle Privacy",
                        modifier = GlanceModifier
                            .clickable(actionRunCallback<ToggleWidgetPrivacyCallback>())
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = GlanceModifier.height(8.dp))

                if (accountCount == 0) {
                    Text(
                        text = "No accounts yet",
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = "Tap to add an account",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )
                } else {
                    // Primary financial amount (~28sp prominent)
                    Text(
                        text = formattedAmount,
                        style = TextStyle(
                            color = textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(2.dp))

                    Text(
                        text = "Total balance",
                        style = TextStyle(
                            color = textSecondary,
                            fontSize = 15.sp
                        )
                    )

                    Spacer(modifier = GlanceModifier.height(10.dp))

                    // Footer Row with attached '+' quick action
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Across $accountCount account${if (accountCount > 1) "s" else ""}",
                            style = TextStyle(
                                color = textSecondary,
                                fontSize = 15.sp
                            )
                        )
                        Spacer(modifier = GlanceModifier.defaultWeight())
                        Text(
                            text = "＋",
                            style = TextStyle(
                                color = accentColor,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = GlanceModifier
                                .clickable(actionStartActivity(WidgetFormattingUtils.createDeepLinkIntent(context, "AddTransaction")))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

class BalanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BalanceWidget()
}
