package sayan.apps.rupeeflow.core.widget

import android.content.Context
import android.content.Intent
import sayan.apps.rupeeflow.MainActivity
import sayan.apps.rupeeflow.core.util.CurrencyFormatter
import sayan.apps.rupeeflow.domain.model.UserPreferences

object WidgetFormattingUtils {

    fun evaluateShouldMask(globalHideBalances: Boolean, widgetInstancePrivacyLocked: Boolean): Boolean {
        return globalHideBalances || widgetInstancePrivacyLocked
    }

    fun formatWidgetAmount(amount: Double, userPreferences: UserPreferences, shouldMask: Boolean): String {
        return if (shouldMask) {
            "••••••"
        } else {
            CurrencyFormatter.format(amount, userPreferences)
        }
    }

    fun createDeepLinkIntent(context: Context, routeName: String): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("EXTRA_TARGET_ROUTE", routeName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }
}
