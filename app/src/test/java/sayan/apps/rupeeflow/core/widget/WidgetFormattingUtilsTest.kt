package sayan.apps.rupeeflow.core.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import sayan.apps.rupeeflow.domain.model.UserPreferences

class WidgetFormattingUtilsTest {

    @Test
    fun evaluateShouldMask_globalHideBalancesTrue_returnsTrue() {
        val result = WidgetFormattingUtils.evaluateShouldMask(globalHideBalances = true, widgetInstancePrivacyLocked = false)
        assertTrue(result)
    }

    @Test
    fun evaluateShouldMask_instancePrivacyLockedTrue_returnsTrue() {
        val result = WidgetFormattingUtils.evaluateShouldMask(globalHideBalances = false, widgetInstancePrivacyLocked = true)
        assertTrue(result)
    }

    @Test
    fun evaluateShouldMask_bothFalse_returnsFalse() {
        val result = WidgetFormattingUtils.evaluateShouldMask(globalHideBalances = false, widgetInstancePrivacyLocked = false)
        assertFalse(result)
    }

    @Test
    fun formatWidgetAmount_masked_returnsDots() {
        val prefs = UserPreferences(currency = "INR", indianNumberFormat = true)
        val formatted = WidgetFormattingUtils.formatWidgetAmount(42580.0, prefs, shouldMask = true)
        assertEquals("••••••", formatted)
    }

    @Test
    fun formatWidgetAmount_unmasked_formatsCurrency() {
        val prefs = UserPreferences(currency = "INR", indianNumberFormat = true)
        val formatted = WidgetFormattingUtils.formatWidgetAmount(42580.0, prefs, shouldMask = false)
        assertTrue(formatted.contains("42,580"))
    }
}
