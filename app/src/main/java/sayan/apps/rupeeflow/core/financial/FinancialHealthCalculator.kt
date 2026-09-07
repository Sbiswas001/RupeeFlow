package sayan.apps.rupeeflow.core.financial

import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

object FinancialHealthCalculator {

    const val MIN_INCOME_FOR_SAVINGS_EVALUATION = 100.0

    const val WEIGHT_SPENDING_CONTROL = 0.25
    const val WEIGHT_CASH_FLOW = 0.25
    const val WEIGHT_SAVINGS = 0.30
    const val WEIGHT_COMMITMENT_LOAD = 0.20

    /**
     * Calculates an explainable 0-100 Financial Health score based on 4 distinct pillars:
     * 1. Spending Control (25%)
     * 2. Cash Flow (25%)
     * 3. Savings (30%)
     * 4. Commitment Load (20%)
     */
    fun calculateFinancialHealth(
        income: Double,
        expense: Double,
        budgets: List<BudgetWithProgress> = emptyList(),
        recurringTotal: Double = 0.0,
        goalProgress: Double = 0.0,
        transactionCount: Int = 0,
        allTransactions: List<Transaction> = emptyList(),
        monthlyHistory: List<MonthlyCashFlow> = emptyList(),
        accountAgeDays: Int? = null,
        currentTimeMs: Long = System.currentTimeMillis()
    ): FinancialHealthResult {
        val confidence = determineDataConfidence(
            transactionCount = transactionCount,
            income = income,
            expense = expense,
            allTransactions = allTransactions,
            accountAgeDays = accountAgeDays,
            currentTimeMs = currentTimeMs
        )

        val confidenceLabel = getConfidenceLabel(confidence)

        if (confidence == FinancialHealthConfidence.INSUFFICIENT_DATA && transactionCount < 3 && income == 0.0 && expense == 0.0) {
            return FinancialHealthResult(
                overallScore = null,
                statusLabel = "Not enough data yet",
                subScores = emptyList(),
                isDataSufficient = false,
                advicePills = listOf("Add transactions to generate your financial health score."),
                confidence = FinancialHealthConfidence.INSUFFICIENT_DATA,
                confidenceLabel = confidenceLabel,
                keyInsight = "Add transactions to unlock your Financial Health insights."
            )
        }

        val resolvedHistory = if (monthlyHistory.isNotEmpty()) {
            monthlyHistory
        } else if (allTransactions.isNotEmpty()) {
            extractMonthlyCashFlows(allTransactions, currentTimeMs)
        } else {
            emptyList()
        }

        val subScores = mutableListOf<FinancialHealthSubScore>()
        val advicePills = mutableListOf<String>()

        // 1. Spending Control (25%)
        val spendingControl = calculateSpendingControl(budgets)
        subScores.add(spendingControl)
        if (spendingControl.isAvailable) {
            if (spendingControl.score < 70) {
                advicePills.add("Budget overspending detected in some categories.")
            } else {
                advicePills.add("Good spending control across budget categories.")
            }
        }

        // 2. Cash Flow (25%)
        val cashFlow = calculateCashFlow(income, expense, resolvedHistory)
        subScores.add(cashFlow)
        if (cashFlow.isAvailable) {
            val net = income - expense
            if (net < 0) {
                advicePills.add("Spending exceeds income this period.")
            } else {
                advicePills.add("Positive cash flow generated.")
            }
        }

        // 3. Savings Rate (30%)
        val savings = calculateSavings(income, expense)
        subScores.add(savings)
        if (savings.isAvailable) {
            val savingsRate = if (income > 0) (income - expense) / income else 0.0
            if (savingsRate >= 0.20) {
                advicePills.add("Healthy savings rate (${(savingsRate * 100).toInt()}%).")
            } else if (savingsRate < 0.05) {
                advicePills.add("Savings rate is low (${(savingsRate * 100).toInt()}%).")
            }
        }

        // 4. Commitment Load (20%)
        val commitmentLoad = calculateCommitmentLoad(recurringTotal, income)
        subScores.add(commitmentLoad)
        if (commitmentLoad.isAvailable) {
            if (commitmentLoad.score >= 90) {
                advicePills.add("Low recurring commitment load.")
            } else if (commitmentLoad.score < 55) {
                advicePills.add("High recurring commitment load relative to income.")
            }
        }

        val availableScores = subScores.filter { it.isAvailable }
        if (availableScores.isEmpty()) {
            return FinancialHealthResult(
                overallScore = null,
                statusLabel = "Not enough data yet",
                subScores = subScores,
                isDataSufficient = false,
                advicePills = listOf("Not enough financial history to compute health score."),
                confidence = FinancialHealthConfidence.INSUFFICIENT_DATA,
                confidenceLabel = confidenceLabel,
                keyInsight = "Insufficient financial data available."
            )
        }

        val rawOverall = calculateOverallScore(subScores)
        val finalOverall = applyCriticalCaps(rawOverall, subScores)

        val statusLabel = getStatusLabel(finalOverall)
        val keyInsight = generateKeyInsight(subScores, income, expense)

        return FinancialHealthResult(
            overallScore = finalOverall,
            statusLabel = statusLabel,
            subScores = subScores,
            isDataSufficient = true,
            advicePills = advicePills.distinct(),
            confidence = confidence,
            confidenceLabel = confidenceLabel,
            keyInsight = keyInsight,
            spendingControlScore = spendingControl.score,
            cashFlowScore = cashFlow.score,
            savingsScore = savings.score,
            commitmentLoadScore = commitmentLoad.score
        )
    }

    internal fun calculateSpendingControl(budgets: List<BudgetWithProgress>): FinancialHealthSubScore {
        if (budgets.isEmpty()) {
            return FinancialHealthSubScore(
                name = "Spending Control",
                score = 80,
                isAvailable = false,
                explanation = "No active budgets set yet.",
                weight = WEIGHT_SPENDING_CONTROL
            )
        }

        val validBudgets = budgets.filter { it.budget.limitAmount > 0 || it.budget.spentAmount > 0 }
        if (validBudgets.isEmpty()) {
            return FinancialHealthSubScore(
                name = "Spending Control",
                score = 80,
                isAvailable = false,
                explanation = "No valid budget limits defined.",
                weight = WEIGHT_SPENDING_CONTROL
            )
        }

        val totalLimit = validBudgets.sumOf { maxOf(0.0, it.budget.limitAmount) }

        val weightedScoreSum = validBudgets.sumOf { b ->
            val limit = b.budget.limitAmount
            val spent = b.budget.spentAmount
            val utilization = if (limit > 0) spent / limit else if (spent > 0) Double.POSITIVE_INFINITY else 0.0
            val budgetScore = calculateBudgetScore(utilization)
            val weightFactor = if (totalLimit > 0 && limit > 0) limit else 1.0
            budgetScore * weightFactor
        }

        val divisor = if (totalLimit > 0) totalLimit else validBudgets.size.toDouble()
        val finalScore = (weightedScoreSum / divisor).roundToInt().coerceIn(0, 100)

        val adheredCount = validBudgets.count { it.budget.spentAmount <= it.budget.limitAmount }
        val explanation = "$finalScore/100 ($adheredCount of ${validBudgets.size} budgets within target)"

        return FinancialHealthSubScore(
            name = "Spending Control",
            score = finalScore,
            isAvailable = true,
            explanation = explanation,
            weight = WEIGHT_SPENDING_CONTROL,
            supportingValueText = "$adheredCount/${validBudgets.size} within target"
        )
    }

    internal fun calculateBudgetScore(utilization: Double): Double {
        return when {
            utilization <= 0.80 -> 100.0
            utilization <= 1.00 -> 100.0 - ((utilization - 0.80) / 0.20) * 15.0
            utilization <= 1.10 -> 85.0 - ((utilization - 1.00) / 0.10) * 20.0
            utilization <= 1.25 -> 65.0 - ((utilization - 1.10) / 0.15) * 30.0
            utilization <= 1.50 -> 35.0 - ((utilization - 1.25) / 0.25) * 25.0
            else -> 0.0
        }
    }

    internal fun calculateCashFlow(
        income: Double,
        expense: Double,
        history: List<MonthlyCashFlow>
    ): FinancialHealthSubScore {
        if (income <= 0 && expense <= 0) {
            return FinancialHealthSubScore(
                name = "Cash Flow",
                score = 50,
                isAvailable = false,
                explanation = "No cash flow data available.",
                weight = WEIGHT_CASH_FLOW
            )
        }

        val currentMargin = if (income > 0) {
            (income - expense) / income
        } else {
            -1.0
        }

        val marginScore = calculateCashFlowMarginScore(currentMargin)
        val consistencyScore = calculateCashFlowConsistency(income - expense, history)

        val combinedScore = (0.70 * marginScore + 0.30 * consistencyScore).roundToInt().coerceIn(0, 100)
        val net = income - expense
        val netFormatted = if (net >= 0) "+₹${net.toInt()}" else "-₹${abs(net.toInt())}"
        val explanation = "$combinedScore/100 (${if (net >= 0) "$netFormatted monthly surplus" else "$netFormatted monthly deficit"})"

        return FinancialHealthSubScore(
            name = "Cash Flow",
            score = combinedScore,
            isAvailable = true,
            explanation = explanation,
            weight = WEIGHT_CASH_FLOW,
            supportingValueText = netFormatted
        )
    }

    internal fun calculateCashFlowMarginScore(margin: Double): Double {
        return when {
            margin <= -0.20 -> 0.0
            margin <= 0.00 -> ((margin - (-0.20)) / 0.20) * 40.0
            margin <= 0.10 -> 40.0 + (margin / 0.10) * 25.0
            margin <= 0.20 -> 65.0 + ((margin - 0.10) / 0.10) * 15.0
            margin <= 0.30 -> 80.0 + ((margin - 0.20) / 0.10) * 10.0
            else -> 100.0
        }
    }

    internal fun calculateCashFlowConsistency(currentNetCashFlow: Double, history: List<MonthlyCashFlow>): Double {
        if (history.isEmpty()) {
            return if (currentNetCashFlow > 0) 70.0 else 10.0
        }

        val completedMonths = history.take(3)
        val positiveMonthsCount = completedMonths.count { it.netCashFlow > 0 }

        return when (completedMonths.size) {
            3 -> when (positiveMonthsCount) {
                3 -> 100.0
                2 -> 70.0
                1 -> 40.0
                else -> 10.0
            }
            2 -> when (positiveMonthsCount) {
                2 -> 85.0
                1 -> 50.0
                else -> 10.0
            }
            1 -> when (positiveMonthsCount) {
                1 -> 70.0
                else -> 10.0
            }
            else -> if (currentNetCashFlow > 0) 70.0 else 10.0
        }
    }

    internal fun calculateSavings(income: Double, expense: Double): FinancialHealthSubScore {
        if (income < MIN_INCOME_FOR_SAVINGS_EVALUATION) {
            return FinancialHealthSubScore(
                name = "Savings",
                score = 0,
                isAvailable = false,
                explanation = "Income < ₹${MIN_INCOME_FOR_SAVINGS_EVALUATION.toInt()} (Savings rate not evaluated)",
                weight = WEIGHT_SAVINGS
            )
        }

        val net = income - expense
        val savingsRate = if (net > 0) net / income else net / income
        val scoreDouble = calculateSavingsRateScore(savingsRate)
        val score = scoreDouble.roundToInt().coerceIn(0, 100)
        val savingsPct = (maxOf(0.0, savingsRate) * 100).roundToInt()

        val explanation = "$score/100 (Retaining $savingsPct% of monthly income)"

        return FinancialHealthSubScore(
            name = "Savings",
            score = score,
            isAvailable = true,
            explanation = explanation,
            weight = WEIGHT_SAVINGS,
            supportingValueText = "$savingsPct%"
        )
    }

    internal fun calculateSavingsRateScore(rate: Double): Double {
        return when {
            rate < 0.00 -> 0.0
            rate <= 0.05 -> 15.0 + (rate / 0.05) * 5.0
            rate <= 0.10 -> 20.0 + ((rate - 0.05) / 0.05) * 20.0
            rate <= 0.15 -> 40.0 + ((rate - 0.10) / 0.05) * 20.0
            rate <= 0.20 -> 60.0 + ((rate - 0.15) / 0.05) * 15.0
            rate <= 0.25 -> 75.0 + ((rate - 0.20) / 0.05) * 10.0
            rate <= 0.30 -> 85.0 + ((rate - 0.25) / 0.05) * 8.0
            else -> 100.0
        }
    }

    internal fun calculateCommitmentLoad(recurringTotal: Double, income: Double): FinancialHealthSubScore {
        if (recurringTotal == 0.0) {
            return FinancialHealthSubScore(
                name = "Commitment Load",
                score = 100,
                isAvailable = true,
                explanation = "100/100 (Low recurring commitment load)",
                weight = WEIGHT_COMMITMENT_LOAD,
                supportingValueText = "0%"
            )
        }

        if (income >= MIN_INCOME_FOR_SAVINGS_EVALUATION) {
            val recurringRatio = recurringTotal / income
            val scoreDouble = when {
                recurringRatio <= 0.20 -> 100.0
                recurringRatio <= 0.30 -> 100.0 - ((recurringRatio - 0.20) / 0.10) * 10.0
                recurringRatio <= 0.40 -> 90.0 - ((recurringRatio - 0.30) / 0.10) * 15.0
                recurringRatio <= 0.50 -> 75.0 - ((recurringRatio - 0.40) / 0.10) * 20.0
                recurringRatio <= 0.60 -> 55.0 - ((recurringRatio - 0.50) / 0.10) * 25.0
                else -> 10.0
            }
            val score = scoreDouble.roundToInt().coerceIn(0, 100)
            val loadPct = (recurringRatio * 100).roundToInt()
            val explanation = "$score/100 (Recurring bills are $loadPct% of income)"

            return FinancialHealthSubScore(
                name = "Commitment Load",
                score = score,
                isAvailable = true,
                explanation = explanation,
                weight = WEIGHT_COMMITMENT_LOAD,
                supportingValueText = "$loadPct%"
            )
        }

        return FinancialHealthSubScore(
            name = "Commitment Load",
            score = 50,
            isAvailable = false,
            explanation = "Income data needed for recurring load.",
            weight = WEIGHT_COMMITMENT_LOAD
        )
    }

    internal fun calculateOverallScore(subScores: List<FinancialHealthSubScore>): Int {
        val available = subScores.filter { it.isAvailable }
        if (available.isEmpty()) return 0

        val weightedSum = available.sumOf { it.score * it.weight }
        val totalWeight = available.sumOf { it.weight }

        return (weightedSum / totalWeight).roundToInt().coerceIn(0, 100)
    }

    internal fun applyCriticalCaps(rawScore: Int, subScores: List<FinancialHealthSubScore>): Int {
        var score = rawScore

        val cashFlow = subScores.find { it.name == "Cash Flow" }
        if (cashFlow != null && cashFlow.isAvailable && cashFlow.score < 20) {
            score = minOf(score, 49)
        }

        val savings = subScores.find { it.name == "Savings" }
        if (savings != null && savings.isAvailable && savings.score < 10) {
            score = minOf(score, 59)
        }

        val commitment = subScores.find { it.name == "Commitment Load" }
        if (commitment != null && commitment.isAvailable && commitment.score < 15) {
            score = minOf(score, 59)
        }

        return score
    }

    internal fun determineDataConfidence(
        transactionCount: Int,
        income: Double,
        expense: Double,
        allTransactions: List<Transaction>,
        accountAgeDays: Int?,
        currentTimeMs: Long
    ): FinancialHealthConfidence {
        if (transactionCount < 3 && income == 0.0 && expense == 0.0) {
            return FinancialHealthConfidence.INSUFFICIENT_DATA
        }

        val days = accountAgeDays ?: if (allTransactions.isNotEmpty()) {
            val minTimestamp = allTransactions.minOf { it.timestamp }
            ((currentTimeMs - minTimestamp) / (24 * 60 * 60 * 1000L)).toInt().coerceAtLeast(0)
        } else {
            0
        }

        return when {
            days <= 7 -> FinancialHealthConfidence.PRELIMINARY
            days <= 30 -> FinancialHealthConfidence.PRELIMINARY
            days <= 90 -> FinancialHealthConfidence.MODERATE
            else -> FinancialHealthConfidence.HIGH
        }
    }

    private fun getConfidenceLabel(confidence: FinancialHealthConfidence): String {
        return when (confidence) {
            FinancialHealthConfidence.INSUFFICIENT_DATA -> "Insufficient data"
            FinancialHealthConfidence.PRELIMINARY -> "Preliminary · < 30 days activity"
            FinancialHealthConfidence.MODERATE -> "Moderate confidence · 1–3 months history"
            FinancialHealthConfidence.HIGH -> "High confidence · 3+ months history"
        }
    }

    private fun getStatusLabel(score: Int): String {
        return when {
            score >= 90 -> "Excellent"
            score >= 75 -> "Good"
            score >= 60 -> "Fair"
            score >= 40 -> "Needs Attention"
            else -> "At Risk"
        }
    }

    private fun generateKeyInsight(
        subScores: List<FinancialHealthSubScore>,
        income: Double,
        expense: Double
    ): String {
        val available = subScores.filter { it.isAvailable }
        if (available.isEmpty()) return "Insufficient financial data."

        val lowest = available.minByOrNull { it.score }
        val highest = available.maxByOrNull { it.score }

        val savingsPillar = available.find { it.name == "Savings" }
        val savingsRateText = if (income > 0) "${(((income - expense) / income).coerceAtLeast(0.0) * 100).toInt()}%" else "0%"

        return when {
            lowest != null && lowest.score < 60 -> {
                val focusName = when (lowest.name) {
                    "Spending Control" -> "keeping your budgets under control"
                    "Cash Flow" -> "generating positive monthly cash flow"
                    "Savings" -> "increasing your savings rate"
                    "Commitment Load" -> "reducing fixed recurring commitments"
                    else -> lowest.name.lowercase()
                }
                if (highest != null && highest != lowest && highest.score >= 75) {
                    "Strong performance in ${highest.name}. Your biggest opportunity is $focusName."
                } else {
                    "Your primary focus should be $focusName to improve your financial health."
                }
            }
            savingsPillar != null && savingsPillar.score >= 75 -> {
                "You're saving $savingsRateText of your income with solid financial metrics overall."
            }
            else -> {
                "Your financial health is stable. Keep monitoring spending and maintaining your savings habits."
            }
        }
    }

    internal fun extractMonthlyCashFlows(
        allTransactions: List<Transaction>,
        currentTimeMs: Long
    ): List<MonthlyCashFlow> {
        val nonTransfers = FinancialCalculations.filterNonTransferTransactions(allTransactions)
        if (nonTransfers.isEmpty()) return emptyList()

        val result = mutableListOf<MonthlyCashFlow>()

        for (i in 1..3) {
            val targetCal = Calendar.getInstance().apply {
                timeInMillis = currentTimeMs
                add(Calendar.MONTH, -i)
            }
            val targetYear = targetCal.get(Calendar.YEAR)
            val targetMonth = targetCal.get(Calendar.MONTH)

            val monthTxs = nonTransfers.filter { tx ->
                val txCal = Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                txCal.get(Calendar.YEAR) == targetYear && txCal.get(Calendar.MONTH) == targetMonth
            }

            if (monthTxs.isNotEmpty()) {
                val net = FinancialCalculations.calculateNetCashFlow(monthTxs)
                result.add(
                    MonthlyCashFlow(
                        monthLabel = "${targetCal.get(Calendar.MONTH) + 1}/$targetYear",
                        income = net.income,
                        expense = net.spending,
                        netCashFlow = net.netCashFlow
                    )
                )
            }
        }

        return result
    }
}
