package sayan.apps.rupeeflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Attachment
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.getTransactions()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun getAttachments(transactionId: Long) = repository.getAttachments(transactionId)

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }
}
