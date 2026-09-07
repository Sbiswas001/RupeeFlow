package sayan.apps.rupeeflow.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import sayan.apps.rupeeflow.domain.model.UserPreferences

class CurrencyFormatterTest {

    @Test
    fun `format returns formatted amount when hideBalances is false`() {
        val prefs = UserPreferences(currency = "INR", hideBalances = false)
        val result = CurrencyFormatter.format(1234.56, prefs)
        assertTrue(result.contains("1,234.56") || result.contains("1,234.56"))
    }

    @Test
    fun `format returns dots when hideBalances is true`() {
        val prefs = UserPreferences(currency = "INR", hideBalances = true)
        val result = CurrencyFormatter.format(1234.56, prefs)
        assertEquals("••••••", result)
    }

    @Test
    fun `format returns formatted amount when hideBalances is true but overrideHideBalances is true`() {
        val prefs = UserPreferences(currency = "INR", hideBalances = true)
        val result = CurrencyFormatter.format(1234.56, prefs, overrideHideBalances = true)
        assertTrue(result.contains("1,234.56") || result.contains("1,234.56"))
    }

    @Test
    fun `formatCompact returns dots when hideBalances is true`() {
        val prefs = UserPreferences(currency = "INR", hideBalances = true)
        val result = CurrencyFormatter.formatCompact(150000.0, prefs)
        assertEquals("••••••", result)
    }

    @Test
    fun `formatCompact returns formatted amount when overrideHideBalances is true`() {
        val prefs = UserPreferences(currency = "INR", hideBalances = true, indianNumberFormat = true)
        val result = CurrencyFormatter.formatCompact(150000.0, prefs, overrideHideBalances = true)
        assertEquals("1.50 L", result)
    }
}
