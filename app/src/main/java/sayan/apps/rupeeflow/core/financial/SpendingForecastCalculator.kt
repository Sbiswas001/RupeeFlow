package sayan.apps.rupeeflow.core.financial

import sayan.apps.rupeeflow.domain.model.BudgetWithProgress
import java.util.Locale
import kotlin.math.max

object SpendingForecastCalculator {

    /**
     * Calculates Safe to Spend Today:
     * Available To Spend = Monthly Budget - Actual Spending - Upcoming Bills - Planned Savings Contributions
     * Safe To Spend Today = max(0, Available To Spend / Days Remaining)
     *
     * If no monthly budget is configured, displays:
     * "Set a monthly budget to calculate your safe spending limit."
     */
    fun calculateSafeToSpend(
        budgets: List<BudgetWithProgress>,
        upcomingBills: Double = 0.0,
        plannedSavings: Double = 0.0,
        daysRemaining: Int,
        upcomingBillsAlreadyInBudget: Boolean = false,
        totalDaysInMonth: Int = daysRemaining,
        currencySymbol: String = "₹"
    ): SafeToSpendResult {
        if (budgets.isEmpty()) {
            return SafeToSpendResult(
                safeToSpendToday = 0.0,
                availableToSpend = 0.0,
                remainingBudget = 0.0,
                daysRemaining = daysRemaining,
                dailyTarget = 0.0,
                hasConfiguredBudget = false,
                statusMessage = "Set a monthly budget to calculate your safe spending limit.",
                explanation = "No monthly budgets set."
            )
        }

        val totalBudget = budgets.sumOf { it.budget.limitAmount }
        val actualSpending = budgets.sumOf { it.budget.spentAmount }
        val remainingBudget = max(0.0, totalBudget - actualSpending)

        // Deduct upcoming bills exactly once if not already accounted for
        val billsDeduction = if (upcomingBillsAlreadyInBudget) 0.0 else upcomingBills

        val unadjustedAvailable = totalBudget - actualSpending - billsDeduction - plannedSavings
        val availableToSpend = max(0.0, unadjustedAvailable)
        val validDays = max(1, daysRemaining)
        val safeToday = max(0.0, availableToSpend / validDays)

        val validTotalDays = max(1, totalDaysInMonth)
        val dailyTarget = totalBudget / validTotalDays

        val statusMsg = when {
            actualSpending > totalBudget -> {
                val over = actualSpending - totalBudget
                "Budget exceeded by $currencySymbol${String.format(Locale.US, "%,.0f", over)}"
            }
            unadjustedAvailable < 0.0 -> {
                val shortage = -unadjustedAvailable
                "Commitments exceed budget by $currencySymbol${String.format(Locale.US, "%,.0f", shortage)}"
            }
            safeToday < dailyTarget -> {
                val diff = dailyTarget - safeToday
                "Spending pace is $currencySymbol${String.format(Locale.US, "%,.0f", diff)}/day over budget"
            }
            else -> {
                "Spending pace is on target"
            }
        }

        val explanation = "Available budget ($currencySymbol${String.format(Locale.US, "%,.0f", availableToSpend)}) / $validDays days = $currencySymbol${String.format(Locale.US, "%,.2f", safeToday)}/day"

        return SafeToSpendResult(
            safeToSpendToday = safeToday,
            availableToSpend = availableToSpend,
            remainingBudget = remainingBudget,
            daysRemaining = daysRemaining,
            dailyTarget = dailyTarget,
            hasConfiguredBudget = true,
            statusMessage = statusMsg,
            explanation = explanation
        )
    }

    /**
     * Calculates Budget Usage % strictly over categories that have budgets configured.
     */
    fun calculateBudgetUsage(budgets: List<BudgetWithProgress>): BudgetUsageResult {
        if (budgets.isEmpty()) {
            return BudgetUsageResult(
                overallUsagePercent = 0.0,
                totalBudgetedLimit = 0.0,
                totalBudgetedSpent = 0.0,
                budgetedCategoriesCount = 0
            )
        }

        val totalLimit = budgets.sumOf { it.budget.limitAmount }
        val totalSpent = budgets.sumOf { it.budget.spentAmount }
        val usagePct = if (totalLimit > 0) (totalSpent / totalLimit) * 100.0 else 0.0

        return BudgetUsageResult(
            overallUsagePercent = usagePct,
            totalBudgetedLimit = totalLimit,
            totalBudgetedSpent = totalSpent,
            budgetedCategoriesCount = budgets.size
        )
    }

    /**
     * Calculates Forecast:
     * Estimated Month End = Current Balance + Expected Income - Upcoming Bills - Projected Remaining Spending
     *
     * `currentMonthSpending` may include categories without a budget, so pacing is
     * calculated separately from `budgetedCurrentSpending`. This keeps the budget
     * alert from comparing unlike values.
     */
    fun calculateSpendingForecast(
        currentBalance: Double,
        expectedIncome: Double,
        upcomingBills: Double,
        currentMonthSpending: Double,
        daysPassed: Int,
        totalDaysInMonth: Int,
        budgetLimit: Double = 0.0,
        budgetedCurrentSpending: Double = currentMonthSpending,
        currencySymbol: String = "₹"
    ): ForecastResult {
        val safeDaysPassed = max(1, daysPassed)
        val daysRemaining = max(0, totalDaysInMonth - daysPassed)
        val dailyRunRate = currentMonthSpending / safeDaysPassed
        val projectedRemainingSpending = dailyRunRate * daysRemaining

        val estimatedMonthEnd = currentBalance + expectedIncome - upcomingBills - projectedRemainingSpending

        val budgetedDailyRunRate = budgetedCurrentSpending / safeDaysPassed
        val totalProjectedBudgetedSpending = budgetedCurrentSpending + (budgetedDailyRunRate * daysRemaining)
        val isPacingOverBudget = budgetLimit > 0.0 && totalProjectedBudgetedSpending > budgetLimit

        var overrunDay: Int? = null
        val status = if (budgetLimit > 0.0 && budgetedCurrentSpending > budgetLimit) {
            val over = budgetedCurrentSpending - budgetLimit
            overrunDay = daysPassed
            "Budget limit exceeded by $currencySymbol${String.format(Locale.US, "%,.0f", over)}"
        } else if (isPacingOverBudget) {
            if (budgetedDailyRunRate > 0.0) {
                val daysToOverrun = ((budgetLimit - budgetedCurrentSpending) / budgetedDailyRunRate).toInt()
                if (daysToOverrun in 0..daysRemaining) {
                    overrunDay = daysPassed + daysToOverrun
                }
            }
            "At current spending pace, you may exceed budget around day ${overrunDay ?: (daysPassed + 1)}"
        } else {
            "On track to finish month within forecast"
        }

        return ForecastResult(
            currentBalance = currentBalance,
            expectedIncome = expectedIncome,
            upcomingBills = upcomingBills,
            projectedRemainingSpending = projectedRemainingSpending,
            estimatedMonthEnd = estimatedMonthEnd,
            isPacingOverBudget = isPacingOverBudget,
            projectedOverrunDay = overrunDay,
            statusMessage = status,
            isProjectedDeficit = estimatedMonthEnd < 0.0
        )
    }
}
