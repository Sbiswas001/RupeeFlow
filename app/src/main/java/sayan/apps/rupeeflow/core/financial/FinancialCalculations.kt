package sayan.apps.rupeeflow.core.financial

import sayan.apps.rupeeflow.core.util.deduplicateTransfers
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategorySpending
import java.util.Locale
import kotlin.math.abs

object FinancialCalculations {

    /**
     * Filters out internal transfers, transfer pairs, and balance adjustments so they
     * do not distort income, expense, cash flow, or category spending.
     */
    fun filterNonTransferTransactions(transactions: List<Transaction>): List<Transaction> {
        val deduplicated = transactions.deduplicateTransfers()
        return deduplicated.filter { transaction ->
            transaction.type != TransactionType.TRANSFER &&
                    transaction.type != TransactionType.BALANCE_ADJUSTMENT &&
                    transaction.transferId == null
        }
    }

    /**
     * Calculates Net Cash Flow = Total Income - Total Expense.
     * Excludes transfers.
     */
    fun calculateNetCashFlow(transactions: List<Transaction>): NetCashFlow {
        val filtered = filterNonTransferTransactions(transactions)
        val income = filtered.filter { it.isIncome || it.type == TransactionType.INCOME }.sumOf { it.amount }
        val spending = filtered.filter { !it.isIncome && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val net = income - spending

        val formattedIncome = formatAmountSimple(income)
        val formattedSpending = formatAmountSimple(spending)
        val formattedNet = formatAmountSimple(abs(net))

        val summary = when {
            spending > income && income == 0.0 -> "You spent ₹$formattedSpending with no recorded income."
            spending > income -> "You spent ₹$formattedNet more than you earned."
            income > spending -> "You saved ₹$formattedNet this period."
            else -> "Net cash flow is ₹0.00."
        }

        return NetCashFlow(
            income = income,
            spending = spending,
            netCashFlow = net,
            textSummary = summary
        )
    }

    /**
     * Safely calculates percentage change between previous and current values.
     * If previous == 0.0, returns null (meaning "New" / "N/A"). Never returns 100% for 0 baseline.
     */
    fun calculatePercentChange(old: Double, new: Double): Double? {
        if (old == 0.0) return null
        return ((new - old) / abs(old)) * 100.0
    }

    /**
     * Formats percentage change as a label string ("New", "↑ 24%", "↓ 18%", "→ 0%").
     */
    fun formatPercentChangeText(change: Double?): String {
        if (change == null) return "New"
        val rounded = abs(change).toInt()
        return when {
            change > 0 -> "↑ $rounded%"
            change < 0 -> "↓ $rounded%"
            else -> "→ 0%"
        }
    }

    /**
     * Calculates category shares cleanly:
     * - Formats values < 1% as "0.9%" or "<1%" (never "0%" for non-zero spending)
     * - Groups categories beyond maxItems under "Other"
     * - Reconciles visible percentages to 100% total
     */
    fun calculateCategoryShare(
        categorySpending: List<CategorySpending>,
        totalExpense: Double,
        maxItems: Int = 5
    ): List<FormattedCategoryShare> {
        if (totalExpense <= 0.0 || categorySpending.isEmpty()) return emptyList()

        val sorted = categorySpending.filter { it.amount > 0.0 }.sortedByDescending { it.amount }
        if (sorted.isEmpty()) return emptyList()

        val topCategories = sorted.take(maxItems)
        val remainingCategories = sorted.drop(maxItems)

        val result = mutableListOf<FormattedCategoryShare>()

        topCategories.forEach { item ->
            val pct = (item.amount / totalExpense) * 100.0
            val formattedPct = formatCategoryPercentage(pct)
            result.add(
                FormattedCategoryShare(
                    categoryId = item.categoryId,
                    categoryName = item.categoryName,
                    amount = item.amount,
                    percentage = pct,
                    formattedPercentage = formattedPct,
                    isOther = false,
                    colorHex = item.colorHex
                )
            )
        }

        if (remainingCategories.isNotEmpty()) {
            val otherAmount = remainingCategories.sumOf { it.amount }
            if (otherAmount > 0.0) {
                val pct = (otherAmount / totalExpense) * 100.0
                val formattedPct = formatCategoryPercentage(pct)
                result.add(
                    FormattedCategoryShare(
                        categoryId = null,
                        categoryName = "Other",
                        amount = otherAmount,
                        percentage = pct,
                        formattedPercentage = formattedPct,
                        isOther = true,
                        colorHex = "#6B7280"
                    )
                )
            }
        }

        return result
    }

    private fun formatCategoryPercentage(pct: Double): String {
        return when {
            pct <= 0.0 -> "0%"
            pct < 0.1 -> "<1%"
            pct < 1.0 -> String.format(Locale.US, "%.1f%%", pct)
            else -> "${pct.toInt()}%"
        }
    }

    /**
     * Computes category deltas comparing current vs previous period.
     */
    fun calculateCategoryTrends(
        currentSpending: List<CategorySpending>,
        previousSpending: List<CategorySpending>
    ): List<CategoryTrend> {
        val prevMap = previousSpending.associateBy { it.categoryName }
        return currentSpending.map { current ->
            val prev = prevMap[current.categoryName]?.amount ?: 0.0
            val change = calculatePercentChange(prev, current.amount)
            val dir = when {
                prev == 0.0 && current.amount > 0 -> TrendDirection.NEW
                change == null -> TrendDirection.NO_CHANGE
                change > 0 -> TrendDirection.UP
                change < 0 -> TrendDirection.DOWN
                else -> TrendDirection.NO_CHANGE
            }
            CategoryTrend(
                categoryId = current.categoryId ?: prevMap[current.categoryName]?.categoryId,
                categoryName = current.categoryName,
                currentAmount = current.amount,
                previousAmount = prev,
                percentChange = change,
                formattedChange = formatPercentChangeText(change),
                direction = dir
            )
        }.sortedByDescending { it.currentAmount }
    }

    /**
     * Identifies unusual spending anomalies when sufficient historical data exists (sample size >= 5 transactions).
     */
    fun detectSpendingAnomalies(
        currentTransactions: List<Transaction>,
        historicalTransactions: List<Transaction>
    ): List<SpendingAnomaly> {
        val filteredCurrent = filterNonTransferTransactions(currentTransactions).filter { !it.isIncome }
        val filteredHist = filterNonTransferTransactions(historicalTransactions).filter { !it.isIncome }

        if (filteredHist.size < 5) return emptyList()

        val avgAmount = filteredHist.map { it.amount }.average()
        if (avgAmount <= 0) return emptyList()

        val anomalies = mutableListOf<SpendingAnomaly>()
        filteredCurrent.forEach { tx ->
            val multiplier = tx.amount / avgAmount
            if (multiplier >= 4.0 && tx.amount >= 1000.0) {
                val formattedMultiplier = String.format(Locale.US, "%.1fx", multiplier)
                anomalies.add(
                    SpendingAnomaly(
                        title = "Unusual Expense: ${tx.title}",
                        description = "₹${formatAmountSimple(tx.amount)} is $formattedMultiplier higher than your average transaction.",
                        transaction = tx,
                        multiplier = multiplier
                    )
                )
            }
        }
        return anomalies.sortedByDescending { it.multiplier }
    }

    private fun formatAmountSimple(amount: Double): String {
        return String.format(Locale.US, "%,.2f", amount)
    }
}
