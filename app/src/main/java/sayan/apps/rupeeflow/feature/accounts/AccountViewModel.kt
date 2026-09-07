package sayan.apps.rupeeflow.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.SavedUpiApp
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val repository: AccountRepository
) : ViewModel() {

    val accounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val netWorthHistory: StateFlow<List<Pair<Long, Double>>> = repository.getNetWorthHistory(days = 30)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addAccount(
        name: String,
        category: String,
        subType: String,
        balance: Double,
        institutionName: String? = null,
        accountNumberLast4: String? = null,
        creditLimit: Double? = null,
        interestRate: Double? = null,
        maturityDate: Long? = null,
        principalAmount: Double? = null,
        tenureMonths: Int? = null,
        upiId: String? = null,
        colorHex: String? = null
    ) {
        viewModelScope.launch {
            repository.addAccount(
                Account(
                    name = name,
                    category = category,
                    subType = subType,
                    balance = balance,
                    institutionName = institutionName,
                    accountNumberLast4 = accountNumberLast4,
                    creditLimit = creditLimit,
                    interestRate = interestRate,
                    maturityDate = maturityDate,
                    principalAmount = principalAmount,
                    tenureMonths = tenureMonths,
                    upiId = upiId,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun transferFunds(fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.transferFunds(fromAccountId, toAccountId, amount, note, timestamp)
        }
    }

    fun reconcileAccount(accountId: Long, actualBalance: Double, reason: String?, note: String?) {
        viewModelScope.launch {
            repository.reconcileAccount(accountId, actualBalance, reason, note)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun getDebitCardsForAccount(accountId: Long): Flow<List<DebitCard>> {
        return repository.getDebitCardsForAccount(accountId)
    }

    fun addDebitCard(debitCard: DebitCard) {
        viewModelScope.launch {
            repository.addDebitCard(debitCard)
        }
    }

    fun updateDebitCard(debitCard: DebitCard) {
        viewModelScope.launch {
            repository.updateDebitCard(debitCard)
        }
    }

    fun deleteDebitCard(debitCard: DebitCard) {
        viewModelScope.launch {
            repository.deleteDebitCard(debitCard)
        }
    }

    fun getUpiAppsForAccount(accountId: Long): Flow<List<SavedUpiApp>> {
        return repository.getUpiAppsForAccount(accountId)
    }

    fun addUpiApp(upiApp: SavedUpiApp) {
        viewModelScope.launch {
            repository.addUpiApp(upiApp)
        }
    }

    fun updateUpiApp(upiApp: SavedUpiApp) {
        viewModelScope.launch {
            repository.updateUpiApp(upiApp)
        }
    }

    fun deleteUpiApp(upiApp: SavedUpiApp) {
        viewModelScope.launch {
            repository.deleteUpiApp(upiApp)
        }
    }
}
