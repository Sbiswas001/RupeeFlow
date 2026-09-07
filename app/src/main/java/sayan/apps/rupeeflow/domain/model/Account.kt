package sayan.apps.rupeeflow.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: Long = 0,
    val name: String,
    val category: String, // CASH_WALLETS, BANKING, CREDIT, DEPOSITS, INVESTMENTS, ASSETS, LIABILITIES, OTHERS
    val subType: String, // CASH, SAVINGS, CURRENT, WALLET, UPI, CREDIT_CARD, FD, RD, MUTUAL_FUND, STOCKS, GOLD, PPF, EPF, NPS, BONDS, LOAN, ASSET, OTHERS
    val balance: Double,
    val institutionName: String? = null,
    val accountNumberLast4: String? = null,
    val creditLimit: Double? = null,
    val interestRate: Double? = null,
    val maturityDate: Long? = null,
    val principalAmount: Double? = null,
    val tenureMonths: Int? = null,
    val upiId: String? = null,
    val colorHex: String? = null,
    val lastReconciledAt: Long? = null,
    val lastReconciledBalance: Double? = null,
    val isClosed: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)
