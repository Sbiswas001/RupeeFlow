package sayan.apps.rupeeflow.core.database.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import sayan.apps.rupeeflow.core.util.deduplicateTransfers
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType

class TransferDeduplicationTest {

    @Test
    fun `historical duplicate transfers with isIncoming false on both sides are deduplicated to exactly 1 transaction`() {
        val t1 = Transaction(
            id = "1",
            title = "Transfer Out",
            amount = 500.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_A",
            isIncoming = false
        )
        val t2 = Transaction(
            id = "2",
            title = "Transfer Out",
            amount = 500.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_A",
            isIncoming = false
        )

        val list = listOf(t1, t2)
        val deduplicated = list.deduplicateTransfers()

        assertEquals(1, deduplicated.size)
        assertEquals("TRANSFER_A", deduplicated.first().transferId)
    }

    @Test
    fun `normal transfer pair with incoming and outgoing is deduplicated to 1 transaction`() {
        val outgoing = Transaction(
            id = "10",
            title = "Transfer Out",
            amount = 500.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_B",
            isIncoming = false
        )
        val incoming = Transaction(
            id = "11",
            title = "Transfer In",
            amount = 500.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_B",
            isIncoming = true
        )

        val list = listOf(outgoing, incoming)
        val deduplicated = list.deduplicateTransfers()

        assertEquals(1, deduplicated.size)
        assertEquals("10", deduplicated.first().id)
        assertEquals(false, deduplicated.first().isIncoming)
    }

    @Test
    fun `multiple transfers and normal transactions are deduplicated correctly`() {
        val t1 = Transaction(
            id = "1",
            title = "Transfer Out",
            amount = 100.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_1",
            isIncoming = false
        )
        val t2 = Transaction(
            id = "2",
            title = "Transfer In",
            amount = 100.0,
            timestamp = 1000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_1",
            isIncoming = true
        )
        val t3 = Transaction(
            id = "3",
            title = "Transfer Out",
            amount = 200.0,
            timestamp = 2000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_2",
            isIncoming = false
        )
        val t4 = Transaction(
            id = "4",
            title = "Transfer In",
            amount = 200.0,
            timestamp = 2000L,
            category = "General",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "TRANSFER_2",
            isIncoming = true
        )
        val normal = Transaction(
            id = "5",
            title = "Groceries",
            amount = 50.0,
            timestamp = 3000L,
            category = "Food",
            isIncome = false,
            type = TransactionType.EXPENSE,
            transferId = null
        )

        val list = listOf(t1, t2, t3, t4, normal)
        val deduplicated = list.deduplicateTransfers()

        assertEquals(3, deduplicated.size)
    }

    @Test
    fun `normal transactions with null transferId remain intact and are not grouped together`() {
        val t1 = Transaction(
            id = "1", title = "Lunch", amount = 15.0, timestamp = 1000L,
            category = "Food", isIncome = false, type = TransactionType.EXPENSE, transferId = null
        )
        val t2 = Transaction(
            id = "2", title = "Dinner", amount = 25.0, timestamp = 2000L,
            category = "Food", isIncome = false, type = TransactionType.EXPENSE, transferId = null
        )
        val t3 = Transaction(
            id = "3", title = "Salary", amount = 1000.0, timestamp = 3000L,
            category = "Income", isIncome = true, type = TransactionType.INCOME, transferId = null
        )

        val list = listOf(t1, t2, t3)
        val deduplicated = list.deduplicateTransfers()

        assertEquals(3, deduplicated.size)
    }

    @Test
    fun `analytics filtering considers only INCOME and EXPENSE, ignoring TRANSFER transactions`() {
        val expense = Transaction(
            id = "1", title = "Shopping", amount = 100.0, timestamp = 1000L,
            category = "Shopping", isIncome = false, type = TransactionType.EXPENSE
        )
        val income = Transaction(
            id = "2", title = "Salary", amount = 500.0, timestamp = 1000L,
            category = "Salary", isIncome = true, type = TransactionType.INCOME
        )
        val transfer1 = Transaction(
            id = "3", title = "Transfer Out", amount = 300.0, timestamp = 1000L,
            category = "General", isIncome = false, type = TransactionType.TRANSFER, transferId = "TR_1"
        )
        val transfer2 = Transaction(
            id = "4", title = "Transfer In", amount = 300.0, timestamp = 1000L,
            category = "General", isIncome = false, type = TransactionType.TRANSFER, transferId = "TR_1", isIncoming = true
        )

        val list = listOf(expense, income, transfer1, transfer2)

        val expensesSum = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val incomeSum = list.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        assertEquals(100.0, expensesSum, 0.001)
        assertEquals(500.0, incomeSum, 0.001)
    }
}
