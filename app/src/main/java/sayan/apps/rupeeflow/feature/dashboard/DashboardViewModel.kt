package sayan.apps.rupeeflow.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject

data class DashboardState(
    val totalBalance: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardState> = combine(
        accountRepository.getAccounts(),
        transactionRepository.getTransactions()
    ) { accounts, transactions ->
        DashboardState(
            totalBalance = accounts.sumOf { it.balance },
            accounts = accounts,
            recentTransactions = transactions.take(5)
        )
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )
}
