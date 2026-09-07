package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountCategory {
    CASH_WALLETS, BANKING, CREDIT, DEPOSITS, INVESTMENTS, ASSETS, LIABILITIES, OTHERS
}

enum class AccountSubType {
    CASH, SAVINGS, CURRENT, WALLET, UPI, CREDIT_CARD, FD, RD, MUTUAL_FUND, STOCKS, GOLD, PPF, EPF, NPS, BONDS, LOAN, ASSET, OTHERS
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: AccountCategory,
    val subType: AccountSubType,
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
