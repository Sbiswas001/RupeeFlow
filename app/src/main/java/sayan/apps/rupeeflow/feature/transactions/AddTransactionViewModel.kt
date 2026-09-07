package sayan.apps.rupeeflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.core.widget.WidgetUpdateCoordinator
import sayan.apps.rupeeflow.domain.model.Account
import sayan.apps.rupeeflow.domain.model.Attachment
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.DebitCard
import sayan.apps.rupeeflow.domain.model.PaymentMethodType
import sayan.apps.rupeeflow.domain.model.SavedUpiApp
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.model.UPIApp
import sayan.apps.rupeeflow.domain.model.UPIMetadata
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val widgetUpdateCoordinator: WidgetUpdateCoordinator
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

    private val _showPaymentDetails = MutableStateFlow(false)
    val showPaymentDetails = _showPaymentDetails.asStateFlow()

    private val _paymentMethodType = MutableStateFlow(PaymentMethodType.UPI)
    val paymentMethodType = _paymentMethodType.asStateFlow()

    private val _upiTransactionId = MutableStateFlow("")
    val upiTransactionId = _upiTransactionId.asStateFlow()

    private val _selectedUpiAppName = MutableStateFlow<String?>(null)
    val selectedUpiAppName = _selectedUpiAppName.asStateFlow()

    private val _selectedDebitCard = MutableStateFlow<DebitCard?>(null)
    val selectedDebitCard = _selectedDebitCard.asStateFlow()

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
        allCategories.filter { it.type == targetType }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upiApps: StateFlow<List<SavedUpiApp>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.getUpiAppsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val debitCards: StateFlow<List<DebitCard>> = _selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId != null) {
                accountRepository.getDebitCardsForAccount(accountId)
            } else {
                flowOf(emptyList())
            }
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
            if (list.isNotEmpty() && (_selectedCategoryId.value == null || list.none { it.id == _selectedCategoryId.value })) {
                _selectedCategoryId.value = list.first().id
            }
        }.launchIn(viewModelScope)

        // Select first debit card when debit cards list changes and current selection is null
        debitCards.onEach { cards ->
            if (_selectedDebitCard.value == null && cards.isNotEmpty()) {
                _selectedDebitCard.value = cards.first()
            }
        }.launchIn(viewModelScope)

        // Select first UPI app when UPI apps list changes and current selection is null
        upiApps.onEach { apps ->
            if (_selectedUpiAppName.value == null && apps.isNotEmpty()) {
                _selectedUpiAppName.value = apps.first().appName
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

    fun addCategory(category: Category, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = categoryRepository.addCategory(category)
            _selectedCategoryId.value = newId
            onCreated?.invoke(newId)
        }
    }

    fun onAccountChange(accountId: Long) {
        _selectedAccountId.value = accountId
        _selectedDebitCard.value = null
    }

    fun onTogglePaymentDetails(show: Boolean) {
        _showPaymentDetails.value = show
        if (!show) {
            _upiTransactionId.value = ""
            _selectedUpiAppName.value = null
            _selectedDebitCard.value = null
        } else {
            if (_selectedUpiAppName.value == null && upiApps.value.isNotEmpty()) {
                _selectedUpiAppName.value = upiApps.value.first().appName
            }
            if (_selectedDebitCard.value == null && debitCards.value.isNotEmpty()) {
                _selectedDebitCard.value = debitCards.value.first()
            }
        }
    }

    fun onPaymentMethodTypeChange(type: PaymentMethodType) {
        _paymentMethodType.value = type
    }

    fun onUpiAppNameChange(appName: String?) {
        _selectedUpiAppName.value = appName
    }

    fun onDebitCardChange(card: DebitCard?) {
        _selectedDebitCard.value = card
    }

    fun onUPITransactionIdChange(id: String) {
        _upiTransactionId.value = id
    }

    fun onTimestampChange(timestamp: Long) {
        _selectedTimestamp.value = timestamp
    }

    fun addCustomUpiAppInline(appName: String, appPackage: String?) {
        val accountId = _selectedAccountId.value ?: return
        viewModelScope.launch {
            val app = SavedUpiApp(
                accountId = accountId,
                appName = appName.trim(),
                appPackage = appPackage?.ifBlank { null },
                isCustom = true,
                isDefault = false
            )
            accountRepository.addUpiApp(app)
            _selectedUpiAppName.value = appName.trim()
        }
    }

    fun addDebitCardInline(cardName: String, last4Digits: String, network: String?, nickname: String?) {
        val accountId = _selectedAccountId.value ?: return
        viewModelScope.launch {
            val card = DebitCard(
                accountId = accountId,
                cardName = cardName.trim(),
                last4Digits = last4Digits.trim(),
                network = network?.ifBlank { null },
                nickname = nickname?.ifBlank { null }
            )
            val newId = accountRepository.addDebitCard(card)
            val insertedCard = card.copy(id = newId)
            _selectedDebitCard.value = insertedCard
        }
    }

    fun resetFields() {
        _amount.value = ""
        _title.value = ""
        _note.value = ""
        _attachments.value = emptyList()
        _isIncome.value = false
        _showPaymentDetails.value = false
        _paymentMethodType.value = PaymentMethodType.UPI
        _upiTransactionId.value = ""
        _selectedUpiAppName.value = null
        _selectedDebitCard.value = null
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
            _selectedAccountId.value = transaction.accountId
            _selectedCategoryId.value = transaction.categoryId

            if (transaction.paymentMethodType != null) {
                _showPaymentDetails.value = true
                _paymentMethodType.value = transaction.paymentMethodType
                if (transaction.paymentMethodType == PaymentMethodType.UPI) {
                    _upiTransactionId.value = transaction.upiMetadata?.transactionId ?: ""
                    _selectedUpiAppName.value = transaction.upiMetadata?.upiAppNameSnapshot 
                        ?: transaction.upiMetadata?.app?.name?.replace("_", " ")
                } else if (transaction.paymentMethodType == PaymentMethodType.DEBIT_CARD) {
                    transaction.debitCardId?.let { cardId ->
                        _selectedDebitCard.value = accountRepository.getDebitCardById(cardId)
                    }
                }
            } else if (transaction.upiMetadata != null) {
                // Fallback for legacy UPI transactions
                _showPaymentDetails.value = true
                _paymentMethodType.value = PaymentMethodType.UPI
                _upiTransactionId.value = transaction.upiMetadata.transactionId ?: ""
                _selectedUpiAppName.value = transaction.upiMetadata.upiAppNameSnapshot 
                    ?: transaction.upiMetadata.app?.name?.replace("_", " ")
            } else {
                _showPaymentDetails.value = false
            }

            transactionRepository.getAttachments(id).firstOrNull()?.let { list ->
                _attachments.value = list.map { it.filePath }
            }
        }
    }

    fun saveTransaction(onSuccess: () -> Unit) {
        val amountValue = _amount.value.toDoubleOrNull() ?: return
        val titleValue = _title.value.ifEmpty { if (_isIncome.value) "Income" else "Expense" }
        val accountId = _selectedAccountId.value ?: return
        val categoryId = _selectedCategoryId.value
        
        viewModelScope.launch {
            val isPaymentEnabled = _showPaymentDetails.value
            val currentPaymentType = if (isPaymentEnabled) _paymentMethodType.value else null

            val upiMeta = if (isPaymentEnabled && currentPaymentType == PaymentMethodType.UPI) {
                val appEnum = try {
                    _selectedUpiAppName.value?.replace(" ", "_")?.uppercase()?.let { UPIApp.valueOf(it) }
                } catch (e: Exception) {
                    UPIApp.OTHER
                }
                UPIMetadata(
                    transactionId = _upiTransactionId.value.ifEmpty { null },
                    app = appEnum,
                    linkedBank = null,
                    upiAppNameSnapshot = _selectedUpiAppName.value
                )
            } else null

            val debitCard = if (isPaymentEnabled && currentPaymentType == PaymentMethodType.DEBIT_CARD) {
                _selectedDebitCard.value
            } else null

            val transaction = Transaction(
                id = editingTransactionId ?: "",
                title = titleValue,
                amount = amountValue,
                timestamp = _selectedTimestamp.value,
                category = categories.value.find { it.id == categoryId }?.name ?: "General",
                isIncome = _isIncome.value,
                note = _note.value.ifEmpty { null },
                upiMetadata = upiMeta,
                paymentMethodType = currentPaymentType,
                debitCardId = debitCard?.id,
                debitCardNameSnapshot = debitCard?.cardName,
                debitCardLast4Snapshot = debitCard?.last4Digits
            )
            
            if (_isEditMode.value) {
                transactionRepository.updateTransaction(transaction, accountId, categoryId)
                val transId = editingTransactionId?.toLongOrNull()
                if (transId != null) {
                    transactionRepository.deleteAttachmentsForTransaction(transId)
                    _attachments.value.forEach { uri ->
                        transactionRepository.addAttachment(
                            Attachment(
                                transactionId = transId,
                                filePath = uri,
                                fileType = "image/*"
                            )
                        )
                    }
                }
            } else {
                val newId = transactionRepository.addTransaction(transaction, accountId, categoryId)
                _attachments.value.forEach { uri ->
                    transactionRepository.addAttachment(
                        Attachment(
                            transactionId = newId,
                            filePath = uri,
                            fileType = "image/*"
                        )
                    )
                }
            }
            widgetUpdateCoordinator.refreshBalance()
            widgetUpdateCoordinator.refreshSpending()
            onSuccess()
        }
    }
}
