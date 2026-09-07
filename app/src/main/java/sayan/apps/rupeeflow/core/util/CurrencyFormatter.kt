package sayan.apps.rupeeflow.core.util

import sayan.apps.rupeeflow.domain.model.UserPreferences
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*

object CurrencyFormatter {
    
    fun format(
        amount: Double,
        preferences: UserPreferences = UserPreferences(),
        includeSymbol: Boolean = true,
        overrideHideBalances: Boolean = false
    ): String {
        if (preferences.hideBalances && !overrideHideBalances) {
            return "••••••"
        }
        val locale = if (preferences.indianNumberFormat) {
            Locale("en", "IN")
        } else {
            Locale.US
        }
        
        val currency = try {
            Currency.getInstance(preferences.currency)
        } catch (e: Exception) {
            Currency.getInstance("INR")
        }

        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = currency
        
        val cleanAmount = if (amount == 0.0) 0.0 else amount
        var formatted = formatter.format(cleanAmount)
        
        // Java's Indian locale sometimes uses "INR" or "Rs." instead of "₹" depending on Android version/provider
        if (preferences.currency == "INR") {
            formatted = formatted.replace("INR", "₹").replace("Rs.", "₹")
        }
        
        return if (includeSymbol) {
            formatted
        } else {
            val symbol = currency.getSymbol(locale)
            formatted.replace(symbol, "").trim()
        }
    }

    fun getSymbol(preferences: UserPreferences): String {
        val locale = if (preferences.indianNumberFormat) Locale("en", "IN") else Locale.US
        val currency = try {
            Currency.getInstance(preferences.currency)
        } catch (e: Exception) {
            Currency.getInstance("INR")
        }
        val symbol = currency.getSymbol(locale)
        return if (preferences.currency == "INR") "₹" else symbol
    }

    /**
     * Formats amount with suffix for large values (e.g., 1.5L, 2Cr)
     */
    fun formatCompact(
        amount: Double,
        preferences: UserPreferences = UserPreferences(),
        overrideHideBalances: Boolean = false
    ): String {
        if (preferences.hideBalances && !overrideHideBalances) {
            return "••••••"
        }
        if (!preferences.indianNumberFormat) {
            return format(amount, preferences, overrideHideBalances = overrideHideBalances) // Standard compact format could be added later
        }
        
        val locale = Locale("en", "IN")
        return when {
            amount >= 10_000_000 -> String.format(locale, "%.2f Cr", amount / 10_000_000)
            amount >= 100_000 -> String.format(locale, "%.2f L", amount / 100_000)
            else -> format(amount, preferences, overrideHideBalances = overrideHideBalances)
        }
    }
}
