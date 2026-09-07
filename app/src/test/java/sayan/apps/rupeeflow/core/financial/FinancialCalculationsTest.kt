package sayan.apps.rupeeflow.core.financial

import org.junit.Assert.*
import org.junit.Test
import sayan.apps.rupeeflow.domain.model.Budget
import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategorySpending

class FinancialCalculationsTest {

    @Test
    fun forecastUsesOnlyRemainingSpendingAndBudgetedCategoriesForPacing() {
        val result = SpendingForecastCalculator.calculateSpendingForecast(
            currentBalance = 1_000.0,
            expectedIncome = 0.0,
            upcomingBills = 100.0,
            currentMonthSpending = 500.0,
            daysPassed = 10,
            totalDaysInMonth = 30,
            budgetLimit = 900.0,
            budgetedCurrentSpending = 100.0
        )

        // The balance already reflects the first ten days' spending; only the
        // remaining 20 days are deducted from it.
        assertEquals(-100.0, result.estimatedMonthEnd, 0.01)
        assertFalse(result.isPacingOverBudget)
        assertTrue(result.isProjectedDeficit)
    }

    @Test
    fun test1_netCashFlowWithLowIncomeAndHighExpense() {
        val transactions = listOf(
            Transaction(
                id = "1",
                title = "Income",
                amount = 73.0,
                timestamp = System.currentTimeMillis(),
                category = "Salary",
                isIncome = true,
                type = TransactionType.INCOME
            ),
            Transaction(
                id = "2",
                title = "Expense",
                amount = 9712.51,
                timestamp = System.currentTimeMillis(),
                category = "Rent",
                isIncome = false,
                type = TransactionType.EXPENSE
            )
        )

        val result = FinancialCalculations.calculateNetCashFlow(transactions)

        assertEquals(73.0, result.income, 0.01)
        assertEquals(9712.51, result.spending, 0.01)
        assertEquals(-9639.51, result.netCashFlow, 0.01)
        assertTrue(result.textSummary.contains("You spent ₹9,639.51 more than you earned."))
    }

    @Test
    fun test2_percentChangeWithZeroPreviousBaseline() {
        val change = FinancialCalculations.calculatePercentChange(0.0, 500.0)
        assertNull(change)

        val formattedText = FinancialCalculations.formatPercentChangeText(change)
        assertEquals("New", formattedText)
    }

    @Test
    fun test3_categoryPercentageFormattingForSmallValues() {
        val total = 9712.51
        val categorySpending = listOf(
            CategorySpending("Rent", "#3B82F6", 8500.0),
            CategorySpending("Miscellaneous", "#10B981", 89.0)
        )

        val shares = FinancialCalculations.calculateCategoryShare(categorySpending, total)
        val miscShare = shares.first { it.categoryName == "Miscellaneous" }

        assertEquals("0.9%", miscShare.formattedPercentage)
        assertNotEquals("0%", miscShare.formattedPercentage)
    }

    @Test
    fun test4_categoryShareReconciliationWithOtherCategory() {
        val total = 1000.0
        val categorySpending = listOf(
            CategorySpending("Cat1", "#1", 400.0),
            CategorySpending("Cat2", "#2", 300.0),
            CategorySpending("Cat3", "#3", 100.0),
            CategorySpending("Cat4", "#4", 100.0),
            CategorySpending("Cat5", "#5", 50.0),
            CategorySpending("Cat6", "#6", 30.0),
            CategorySpending("Cat7", "#7", 20.0)
        )

        val shares = FinancialCalculations.calculateCategoryShare(categorySpending, total, maxItems = 5)

        assertEquals(6, shares.size) // 5 top + 1 Other
        val other = shares.last()
        assertTrue(other.isOther)
        assertEquals("Other", other.categoryName)
        assertEquals(50.0, other.amount, 0.01)
        assertEquals(5.0, other.percentage, 0.01)
    }

    @Test
    fun test5_internalTransfersExcludedFromNetCashFlow() {
        val transactions = listOf(
            Transaction(
                id = "1",
                title = "Salary",
                amount = 10000.0,
                timestamp = System.currentTimeMillis(),
                category = "Income",
                isIncome = true,
                type = TransactionType.INCOME
            ),
            Transaction(
                id = "2",
                title = "Shopping",
                amount = 2000.0,
                timestamp = System.currentTimeMillis(),
                category = "Shopping",
                isIncome = false,
                type = TransactionType.EXPENSE
            ),
            Transaction(
                id = "3",
                title = "Transfer to Savings",
                amount = 500.0,
                timestamp = System.currentTimeMillis(),
                category = "Transfer",
                isIncome = false,
                type = TransactionType.TRANSFER,
                transferId = "transfer_101"
            )
        )

        val result = FinancialCalculations.calculateNetCashFlow(transactions)

        assertEquals(10000.0, result.income, 0.01)
        assertEquals(2000.0, result.spending, 0.01)
        assertEquals(8000.0, result.netCashFlow, 0.01)
    }

    @Test
    fun test6_safeToSpendCalculationWithBudgetAndBills() {
        val budget = Budget(
            id = 1,
            categoryId = 1,
            categoryName = "Overall",
            limitAmount = 15000.0,
            spentAmount = 500.0,
            period = "MONTHLY",
            startDate = System.currentTimeMillis()
        )
        val budgetWithProgress = BudgetWithProgress(budget, 500f / 15000f, 14500.0)

        val result = SpendingForecastCalculator.calculateSafeToSpend(
            budgets = listOf(budgetWithProgress),
            upcomingBills = 2000.0,
            plannedSavings = 1500.0,
            daysRemaining = 28,
            upcomingBillsAlreadyInBudget = false
        )

        assertTrue(result.hasConfiguredBudget)
        assertEquals(11000.0, result.availableToSpend, 0.01)
        assertEquals(392.86, result.safeToSpendToday, 0.01)
    }

    @Test
    fun test7_safeToSpendWhenNoBudgetConfigured() {
        val result = SpendingForecastCalculator.calculateSafeToSpend(
            budgets = emptyList(),
            daysRemaining = 28
        )

        assertFalse(result.hasConfiguredBudget)
        assertEquals(0.0, result.safeToSpendToday, 0.01)
        assertEquals("Set a monthly budget to calculate your safe spending limit.", result.statusMessage)
    }

    @Test
    fun test8_previousMonthZeroIncomeComparison() {
        val change = FinancialCalculations.calculatePercentChange(0.0, 1000.0)
        assertNull(change)
        assertEquals("New", FinancialCalculations.formatPercentChangeText(change))
    }

    @Test
    fun test9_financialHealthWithNoHistoricalData() {
        val result = FinancialHealthCalculator.calculateFinancialHealth(
            income = 0.0,
            expense = 0.0,
            budgets = emptyList(),
            recurringTotal = 0.0,
            transactionCount = 0
        )

        assertFalse(result.isDataSufficient)
        assertNull(result.overallScore)
        assertEquals("Not enough data yet", result.statusLabel)
    }

    @Test
    fun test10_anomalyDetectionRequiresSufficientSampleSize() {
        val currentTx = listOf(
            Transaction("1", "Large Spike", 8500.0, System.currentTimeMillis(), "Rent", isIncome = false, type = TransactionType.EXPENSE)
        )
        val insufficientHistory = listOf(
            Transaction("2", "Food", 200.0, System.currentTimeMillis(), "Food", isIncome = false, type = TransactionType.EXPENSE),
            Transaction("3", "Cab", 300.0, System.currentTimeMillis(), "Travel", isIncome = false, type = TransactionType.EXPENSE)
        )

        val anomalies = FinancialCalculations.detectSpendingAnomalies(currentTx, insufficientHistory)
        assertTrue(anomalies.isEmpty())
    }

    @Test
    fun test11_financialHealthEvaluationWithLowIncomeThreshold() {
        val result = FinancialHealthCalculator.calculateFinancialHealth(
            income = 73.0, // < 100 threshold
            expense = 9712.51,
            budgets = listOf(
                BudgetWithProgress(Budget(1, 1, "Rent", limitAmount = 10000.0, spentAmount = 8500.0, period = "MONTHLY", startDate = System.currentTimeMillis()), 0.85f, 1500.0)
            ),
            recurringTotal = 0.0,
            transactionCount = 11
        )

        assertTrue(result.isDataSufficient)
        assertNotNull(result.overallScore)
        // Savings component should be marked as unavailable due to low income threshold
        val savingsSub = result.subScores.first { it.name == "Savings" }
        assertFalse(savingsSub.isAvailable)
        assertTrue(savingsSub.explanation.contains("Savings rate not evaluated"))
    }

    @Test
    fun test_savingsRate21Percent_producesHighSavingsScoreNot21() {
        val score = FinancialHealthCalculator.calculateSavingsRateScore(0.21)
        assertEquals(77.0, score, 0.5)
    }

    @Test
    fun test_zeroRecurringCommitments_scores100() {
        val subScore = FinancialHealthCalculator.calculateCommitmentLoad(
            recurringTotal = 0.0,
            income = 50000.0
        )
        assertEquals(100, subScore.score)
        assertTrue(subScore.isAvailable)
    }

    @Test
    fun test_smallRecurringCommitment_doesNotImproveScoreVersusZero() {
        val zeroScore = FinancialHealthCalculator.calculateCommitmentLoad(0.0, 50000.0).score
        val smallScore = FinancialHealthCalculator.calculateCommitmentLoad(5000.0, 50000.0).score // 10% load
        assertEquals(100, zeroScore)
        assertEquals(100, smallScore)
        assertTrue(smallScore <= zeroScore)
    }

    @Test
    fun test_budgetOverspending250Percent_muchWorseThan101Percent() {
        val score101 = FinancialHealthCalculator.calculateBudgetScore(1.01)
        val score250 = FinancialHealthCalculator.calculateBudgetScore(2.50)
        assertTrue(score101 > 80.0)
        assertEquals(0.0, score250, 0.01)
    }

    @Test
    fun test_largeBudgetOverspending_influencesSpendingControlMoreThanTinyBudget() {
        val largeOverspent = BudgetWithProgress(
            Budget(1, 1, "Rent", limitAmount = 100000.0, spentAmount = 150000.0, period = "MONTHLY", startDate = System.currentTimeMillis()),
            1.5f, -50000.0
        )
        val smallUnderSpent = BudgetWithProgress(
            Budget(2, 2, "Coffee", limitAmount = 1000.0, spentAmount = 500.0, period = "MONTHLY", startDate = System.currentTimeMillis()),
            0.5f, 500.0
        )

        val result = FinancialHealthCalculator.calculateSpendingControl(listOf(largeOverspent, smallUnderSpent))
        assertTrue("Large budget overspending should drag score down close to 10 rather than unweighted 55", result.score <= 12)
    }

    @Test
    fun test_positiveCashFlow_increasesCashFlowScore() {
        val positive = FinancialHealthCalculator.calculateCashFlow(10000.0, 5000.0, emptyList())
        val negative = FinancialHealthCalculator.calculateCashFlow(5000.0, 10000.0, emptyList())
        assertTrue(positive.score > negative.score)
    }

    @Test
    fun test_negativeCashFlow_decreasesCashFlowScore() {
        val negative = FinancialHealthCalculator.calculateCashFlow(5000.0, 10000.0, emptyList())
        assertTrue(negative.score < 40)
    }

    @Test
    fun test_threePositiveMonths_producesHigherConsistencyThanOneMonth() {
        val threePositive = listOf(
            MonthlyCashFlow("M1", 10000.0, 5000.0),
            MonthlyCashFlow("M2", 10000.0, 6000.0),
            MonthlyCashFlow("M3", 10000.0, 7000.0)
        )
        val onePositive = listOf(
            MonthlyCashFlow("M1", 10000.0, 5000.0)
        )

        val scoreThree = FinancialHealthCalculator.calculateCashFlowConsistency(5000.0, threePositive)
        val scoreOne = FinancialHealthCalculator.calculateCashFlowConsistency(5000.0, onePositive)
        assertTrue(scoreThree > scoreOne)
        assertEquals(100.0, scoreThree, 0.01)
        assertEquals(70.0, scoreOne, 0.01)
    }

    @Test
    fun test_zeroIncome_neverCausesDivisionByZero() {
        val result = FinancialHealthCalculator.calculateFinancialHealth(
            income = 0.0,
            expense = 1000.0,
            budgets = emptyList(),
            recurringTotal = 0.0,
            transactionCount = 5
        )
        assertTrue(result.isDataSufficient)
        val score = result.overallScore
        assertNotNull(score)
        assertFalse(score!!.toDouble().isNaN())
        assertFalse(score.toDouble().isInfinite())
    }

    @Test
    fun test_noBudgets_handledGracefully() {
        val subScore = FinancialHealthCalculator.calculateSpendingControl(emptyList())
        assertFalse(subScore.isAvailable)
        assertEquals(80, subScore.score)
    }

    @Test
    fun test_noRecurringCommitments_handledCorrectly() {
        val subScore = FinancialHealthCalculator.calculateCommitmentLoad(0.0, 10000.0)
        assertTrue(subScore.isAvailable)
        assertEquals(100, subScore.score)
    }

    @Test
    fun test_criticalConditionCaps_work() {
        val rawScore = 80
        val subScoresWithSevereCashFlow = listOf(
            FinancialHealthSubScore("Cash Flow", score = 10, isAvailable = true, explanation = ""),
            FinancialHealthSubScore("Savings", score = 90, isAvailable = true, explanation = ""),
            FinancialHealthSubScore("Spending Control", score = 90, isAvailable = true, explanation = ""),
            FinancialHealthSubScore("Commitment Load", score = 90, isAvailable = true, explanation = "")
        )

        val cappedScore = FinancialHealthCalculator.applyCriticalCaps(rawScore, subScoresWithSevereCashFlow)
        assertEquals(49, cappedScore)
    }

    @Test
    fun test_scores_alwaysClamped0To100() {
        val extremeCashFlow = FinancialHealthCalculator.calculateCashFlowMarginScore(-5.0)
        assertEquals(0.0, extremeCashFlow, 0.01)

        val extremeSavings = FinancialHealthCalculator.calculateSavingsRateScore(2.0)
        assertEquals(100.0, extremeSavings, 0.01)
    }

    @Test
    fun test_overallScore_equalsWeightedCalculationPlusCap() {
        val result = FinancialHealthCalculator.calculateFinancialHealth(
            income = 10000.0,
            expense = 5000.0,
            budgets = listOf(
                BudgetWithProgress(
                    Budget(
                        id = 1,
                        categoryId = 1,
                        categoryName = "Overall",
                        limitAmount = 8000.0,
                        spentAmount = 5000.0,
                        period = "MONTHLY",
                        startDate = System.currentTimeMillis()
                    ),
                    5000f / 8000f,
                    3000.0
                )
            ),
            recurringTotal = 1000.0,
            transactionCount = 10
        )
        assertNotNull(result.overallScore)
        assertTrue(result.overallScore!! in 0..100)
    }

    @Test
    fun test12_endToEndTransferExclusionAcrossCalculations() {
        val transferTx = Transaction(
            id = "t1",
            title = "Account A -> Account B",
            amount = 5000.0,
            timestamp = System.currentTimeMillis(),
            category = "Transfer",
            isIncome = false,
            type = TransactionType.TRANSFER,
            transferId = "tr_999"
        )
        val regularExpense = Transaction(
            id = "e1",
            title = "Groceries",
            amount = 1200.0,
            timestamp = System.currentTimeMillis(),
            category = "Groceries",
            isIncome = false,
            type = TransactionType.EXPENSE
        )

        val txList = listOf(transferTx, regularExpense)
        val filtered = FinancialCalculations.filterNonTransferTransactions(txList)

        assertEquals(1, filtered.size)
        assertEquals("Groceries", filtered.first().title)

        val cashFlow = FinancialCalculations.calculateNetCashFlow(txList)
        assertEquals(0.0, cashFlow.income, 0.01)
        assertEquals(1200.0, cashFlow.spending, 0.01)
    }

    @Test
    fun test13_safeToSpendWhenBudgetIsExceeded() {
        val budget = Budget(
            id = 1, categoryId = 1, categoryName = "Shopping",
            limitAmount = 10000.0, spentAmount = 12000.0, period = "MONTHLY", startDate = System.currentTimeMillis()
        )
        val budgetWithProgress = BudgetWithProgress(budget, 1.2f, -2000.0)

        val result = SpendingForecastCalculator.calculateSafeToSpend(
            budgets = listOf(budgetWithProgress),
            daysRemaining = 15,
            totalDaysInMonth = 30
        )

        assertEquals(0.0, result.safeToSpendToday, 0.01)
        assertTrue(result.statusMessage.contains("Budget exceeded by"))
        assertTrue(result.statusMessage.contains("2,000"))
    }

    @Test
    fun test14_safeToSpendWhenSpendingPaceIsAboveTarget() {
        // Initial budget 30,000 for 30 days -> daily target = 1,000
        // Spent 20,000 in 10 days -> 10,000 remaining for 20 days -> safeToday = 500
        val budget = Budget(
            id = 1, categoryId = 1, categoryName = "Overall",
            limitAmount = 30000.0, spentAmount = 20000.0, period = "MONTHLY", startDate = System.currentTimeMillis()
        )
        val budgetWithProgress = BudgetWithProgress(budget, 20000f / 30000f, 10000.0)

        val result = SpendingForecastCalculator.calculateSafeToSpend(
            budgets = listOf(budgetWithProgress),
            daysRemaining = 20,
            totalDaysInMonth = 30
        )

        assertEquals(500.0, result.safeToSpendToday, 0.01)
        assertEquals(1000.0, result.dailyTarget, 0.01)
        assertTrue(result.statusMessage.contains("over budget"))
        assertTrue(result.statusMessage.contains("500"))
    }

    @Test
    fun test15_forecastWhenCurrentSpendingAlreadyExceedsBudget() {
        val result = SpendingForecastCalculator.calculateSpendingForecast(
            currentBalance = 5000.0,
            expectedIncome = 0.0,
            upcomingBills = 0.0,
            currentMonthSpending = 12000.0,
            daysPassed = 15,
            totalDaysInMonth = 30,
            budgetLimit = 10000.0,
            budgetedCurrentSpending = 12000.0
        )

        assertTrue(result.isPacingOverBudget)
        assertEquals(15, result.projectedOverrunDay)
        assertTrue(result.statusMessage.contains("Budget limit exceeded by"))
        assertTrue(result.statusMessage.contains("2,000"))
    }
}
