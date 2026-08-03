package sayan.apps.rupeeflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _amount = MutableStateFlow("")
    val amount = _amount.asStateFlow()

    private val _title = MutableStateFlow("")
    val title = _title.asStateFlow()

    private val _note = MutableStateFlow("")
    val note = _note.asStateFlow()

    private val _attachments = MutableStateFlow<List<String>>(emptyList())
    val attachments = _attachments.asStateFlow()

    private val _isIncome = MutableStateFlow(false)
    val isIncome = _isIncome.asStateFlow()

    private val _upiTransactionId = MutableStateFlow("")
    val upiTransactionId = _upiTransactionId.asStateFlow()

    private val _upiApp = MutableStateFlow<sayan.apps.rupeeflow.domain.model.UPIApp?>(null)
    val upiApp = _upiApp.asStateFlow()

    private val _selectedAccountId = MutableStateFlow<Long?>(null)
    val selectedAccountId = _selectedAccountId.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _selectedTimestamp = MutableStateFlow(System.currentTimeMillis())
    val selectedTimestamp = _selectedTimestamp.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode = _isEditMode.asStateFlow()

    private var editingTransactionId: String? = null

    val accounts: StateFlow<List<Account>> = accountRepository.getAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categories: StateFlow<List<Category>> = combine(
        categoryRepository.getCategories(),
        _isIncome
    ) { allCategories, isIncome ->
        val targetType = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        allCategories.filter { it.type == targetType && !it.isArchived }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Default to first account and first category if available
        accounts.onEach { list ->
            if (_selectedAccountId.value == null && list.isNotEmpty()) {
                _selectedAccountId.value = list.first().id
            }
        }.launchIn(viewModelScope)

        categories.onEach { list ->
            // If current category is not in the filtered list, pick the first one
            if (list.isNotEmpty() && ( _selectedCategoryId.value == null || list.none { it.id == _selectedCategoryId.value })) {
                _selectedCategoryId.value = list.first().id
            }
        }.launchIn(viewModelScope)
    }

    fun onAmountChange(newAmount: String) {
        if (newAmount.isEmpty() || newAmount.toDoubleOrNull() != null) {
            _amount.value = newAmount
        }
    }

    fun onTitleChange(newTitle: String) {
        _title.value = newTitle
    }

    fun onNoteChange(newNote: String) {
        _note.value = newNote
    }

    fun onAddAttachment(uri: String) {
        _attachments.value = _attachments.value + uri
    }

    fun onRemoveAttachment(uri: String) {
        _attachments.value = _attachments.value - uri
    }

    fun onToggleIncome(isIncome: Boolean) {
        _isIncome.value = isIncome
    }

    fun onCategoryChange(categoryId: Long) {
        _selectedCategoryId.value = categoryId
    }

    fun onAccountChange(accountId: Long) {
        _selectedAccountId.value = accountId
    }

    fun onUPITransactionIdChange(id: String) {
        _upiTransactionId.value = id
    }

    fun onTimestampChange(timestamp: Long) {
        _selectedTimestamp.value = timestamp
    }

    fun onUPIAppChange(app: sayan.apps.rupeeflow.domain.model.UPIApp?) {
        _upiApp.value = app
    }

    fun resetFields() {
        _amount.value = ""
        _title.value = ""
        _note.value = ""
        _attachments.value = emptyList()
        _isIncome.value = false
        _upiTransactionId.value = ""
        _upiApp.value = null
        _selectedTimestamp.value = System.currentTimeMillis()
        _isEditMode.value = false
        editingTransactionId = null
    }

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            val transaction = transactionRepository.getTransactionById(id) ?: return@launch
            editingTransactionId = transaction.id
            _isEditMode.value = true
            
            _amount.value = transaction.amount.toString()
            _title.value = transaction.title
            _isIncome.value = transaction.isIncome
            _note.value = transaction.note ?: ""
            _selectedTimestamp.value = transaction.timestamp
            _upiTransactionId.value = transaction.upiMetadata?.transactionId ?: ""
            _upiApp.value = transaction.upiMetadata?.app
            _selectedAccountId.value = transaction.accountId
            _selectedCategoryId.value = transaction.categoryId
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val amountValue = _amount.value.toDoubleOrNull() ?: return
        val titleValue = _title.value.ifEmpty { if (_isIncome.value) "Income" else "Expense" }
        val accountId = _selectedAccountId.value ?: return
        val categoryId = _selectedCategoryId.value
        
        viewModelScope.launch {
            val transaction = Transaction(
                id = editingTransactionId ?: "",
                title = titleValue,
                amount = amountValue,
                timestamp = _selectedTimestamp.value,
                category = categories.value.find { it.id == categoryId }?.name ?: "General",
                isIncome = _isIncome.value,
                note = _note.value.ifEmpty { null },
                upiMetadata = _upiApp.value?.let { 
                    sayan.apps.rupeeflow.domain.model.UPIMetadata(
                        transactionId = _upiTransactionId.value.ifEmpty { null },
                        app = it,
                        linkedBank = null
                    )
                }
            )
            
            if (_isEditMode.value) {
                transactionRepository.updateTransaction(transaction, accountId, categoryId)
                // TODO: Update attachments for edit mode
            } else {
                val newId = transactionRepository.addTransaction(transaction, accountId, categoryId)
                _attachments.value.forEach { uri ->
                    transactionRepository.addAttachment(
                        sayan.apps.rupeeflow.domain.model.Attachment(
                            transactionId = newId,
                            filePath = uri,
                            fileType = "image/*" // Basic assumption
                        )
                    )
                }
            }
            onSuccess()
        }
    }
}
