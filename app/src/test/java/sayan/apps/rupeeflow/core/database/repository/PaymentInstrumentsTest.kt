package sayan.apps.rupeeflow.core.database.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.PaymentMethodType
import sayan.apps.rupeeflow.domain.model.SavedUpiApp
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.UPIMetadata

class PaymentInstrumentsTest {

    @Test
    fun `debit card domain model holds account relationship and last4`() {
        val debitCard = DebitCard(
            id = 10,
            accountId = 1,
            cardName = "SBI Platinum Debit Card",
            last4Digits = "4821",
            network = "Visa",
            nickname = "Primary Card"
        )

        assertEquals(10L, debitCard.id)
        assertEquals(1L, debitCard.accountId)
        assertEquals("SBI Platinum Debit Card", debitCard.cardName)
        assertEquals("4821", debitCard.last4Digits)
        assertEquals("Visa", debitCard.network)
        assertEquals("Primary Card", debitCard.nickname)
    }

    @Test
    fun `saved upi app distinguishes global defaults vs custom account apps`() {
        val globalApp = SavedUpiApp(
            id = 1,
            accountId = null,
            appName = "Google Pay",
            isCustom = false,
            isDefault = true
        )

        val customApp = SavedUpiApp(
            id = 2,
            accountId = 5,
            appName = "CRED",
            isCustom = true,
            isDefault = false
        )

        assertNull(globalApp.accountId)
        assertEquals("Google Pay", globalApp.appName)
        assertEquals(5L, customApp.accountId)
        assertEquals("CRED", customApp.appName)
    }

    @Test
    fun `transaction with debit card snapshot retains historical details`() {
        val transaction = Transaction(
            id = "100",
            title = "Electronics Store",
            amount = 4500.0,
            timestamp = System.currentTimeMillis(),
            category = "Shopping",
            isIncome = false,
            accountId = 1L,
            paymentMethodType = PaymentMethodType.DEBIT_CARD,
            debitCardId = 10L,
            debitCardNameSnapshot = "SBI Debit Card",
            debitCardLast4Snapshot = "4821"
        )

        assertEquals(PaymentMethodType.DEBIT_CARD, transaction.paymentMethodType)
        assertEquals(10L, transaction.debitCardId)
        assertEquals("SBI Debit Card", transaction.debitCardNameSnapshot)
        assertEquals("4821", transaction.debitCardLast4Snapshot)
    }

    @Test
    fun `transaction with upi app snapshot retains app name and upi transaction id`() {
        val transaction = Transaction(
            id = "101",
            title = "Grocery Store",
            amount = 350.0,
            timestamp = System.currentTimeMillis(),
            category = "Food",
            isIncome = false,
            accountId = 1L,
            paymentMethodType = PaymentMethodType.UPI,
            upiMetadata = UPIMetadata(
                transactionId = "UPI987654321",
                upiAppNameSnapshot = "PhonePe"
            )
        )

        assertEquals(PaymentMethodType.UPI, transaction.paymentMethodType)
        assertEquals("PhonePe", transaction.upiMetadata?.upiAppNameSnapshot)
        assertEquals("UPI987654321", transaction.upiMetadata?.transactionId)
    }
}
