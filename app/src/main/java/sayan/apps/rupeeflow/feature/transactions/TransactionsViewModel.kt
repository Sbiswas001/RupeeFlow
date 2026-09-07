package sayan.apps.rupeeflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Attachment
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val accountRepository: sayan.apps.rupeeflow.domain.repository.AccountRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val accounts = accountRepository.getAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val transactions: StateFlow<Map<String, List<Transaction>>> = repository.getTransactions()
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list else list.filter { 
                it.title.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true) 
            }
        }
        .map { list ->
            list.groupBy { formatGroupHeader(it.timestamp) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun getAttachments(transactionId: Long) = repository.getAttachments(transactionId)

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Double, note: String?, timestamp: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            accountRepository.updateTransfer(transferId, fromAccountId, toAccountId, amount, note, timestamp)
        }
    }

    private fun formatGroupHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val time = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        return when {
            isSameDay(now, time) -> "TODAY"
            isYesterday(now, time) -> "YESTERDAY"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp)).uppercase()
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(now: Calendar, then: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { 
            timeInMillis = now.timeInMillis
            add(Calendar.DAY_OF_YEAR, -1) 
        }
        return isSameDay(yesterday, then)
    }
}
