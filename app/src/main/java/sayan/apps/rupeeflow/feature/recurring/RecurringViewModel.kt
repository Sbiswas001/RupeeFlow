package sayan.apps.rupeeflow.feature.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.RecurringItem
import sayan.apps.rupeeflow.domain.repository.RecurringRepository
import javax.inject.Inject

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val repository: RecurringRepository,
    private val categoryRepository: sayan.apps.rupeeflow.domain.repository.CategoryRepository,
    private val accountRepository: sayan.apps.rupeeflow.domain.repository.AccountRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    val categories = categoryRepository.getCategories()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val accounts = accountRepository.getAccounts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringItems: StateFlow<List<RecurringItem>> = repository.getRecurringItems()
        .combine(_searchQuery) { items, query ->
            if (query.isBlank()) items else items.filter { it.title.contains(query, ignoreCase = true) }
        }
        .combine(_selectedCategory) { items, category ->
            if (category == "All") items else items.filter { it.category == category }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val badgeCount: StateFlow<Int> = repository.getRecurringItems()
        .map { items ->
            items.sumOf { item -> 
                item.occurrences.count { it.status == "PENDING" && it.scheduledDate <= System.currentTimeMillis() }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
    }

    fun markAsPaid(occurrenceId: Long, accountId: Long) {
        viewModelScope.launch {
            repository.markAsPaid(occurrenceId, accountId)
        }
    }

    fun skipOccurrence(occurrenceId: Long) {
        viewModelScope.launch {
            repository.skipOccurrence(occurrenceId)
        }
    }

    fun undoPayment(occurrenceId: Long) {
        viewModelScope.launch {
            repository.undoPayment(occurrenceId)
        }
    }

    fun toggleRecurringItemStatus(itemId: Long) {
        viewModelScope.launch {
            repository.toggleRecurringItemStatus(itemId)
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
        }
    }

    fun updateRecurringItem(
        id: Long,
        title: String,
        amount: Double,
        category: String,
        categoryId: Long?,
        frequency: String,
        frequencyInterval: Int,
        frequencyUnit: String,
        isAutoPay: Boolean,
        dueDate: Long,
        accountId: Long?,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val existingItem = repository.getRecurringItemById(id) ?: return@launch
            repository.updateRecurringItem(
                existingItem.copy(
                    title = title,
                    amount = amount,
                    category = category,
                    categoryId = categoryId,
                    frequency = frequency,
                    frequencyInterval = frequencyInterval,
                    frequencyUnit = frequencyUnit,
                    isAutoPay = isAutoPay,
                    dueDate = dueDate,
                    accountId = accountId
                )
            )
            onComplete()
        }
    }

    fun addRecurringItem(
        title: String,
        amount: Double,
        category: String,
        categoryId: Long?,
        frequency: String,
        frequencyInterval: Int,
        frequencyUnit: String,
        isAutoPay: Boolean,
        dueDate: Long,
        accountId: Long? = null,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            repository.addRecurringItem(
                RecurringItem(
                    title = title,
                    amount = amount,
                    category = category,
                    categoryId = categoryId,
                    frequency = frequency,
                    frequencyInterval = frequencyInterval,
                    frequencyUnit = frequencyUnit,
                    isAutoPay = isAutoPay,
                    dueDate = dueDate,
                    status = "ACTIVE",
                    accountId = accountId
                )
            )
            onComplete()
        }
    }

    fun calculateCostPreview(amount: Double, interval: Int, unit: String): Pair<Double, Double> {
        if (amount <= 0 || interval <= 0) return Pair(0.0, 0.0)
        
        val annualAmount: Double = when (unit.uppercase()) {
            "DAYS" -> (amount / interval) * 365.25
            "WEEKS" -> (amount / interval) * 52.17
            "MONTHS" -> (amount / interval) * 12.0
            "YEARS" -> amount / interval
            else -> 0.0
        }
        
        val monthlyAmount = annualAmount / 12.0
        
        return Pair(monthlyAmount, annualAmount)
    }

    fun getOccurrences(itemId: Long): Flow<List<sayan.apps.rupeeflow.domain.model.RecurringOccurrence>> {
        return repository.getOccurrences(itemId)
    }
}
