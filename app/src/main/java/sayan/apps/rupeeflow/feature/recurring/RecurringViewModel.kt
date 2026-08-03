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
    private val repository: RecurringRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

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

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryChange(category: String) {
        _selectedCategory.value = category
    }

    fun markAsPaid(item: RecurringItem) {
        viewModelScope.launch {
            // Assume Account ID 1 for now
            repository.markAsPaid(item, accountId = 1L)
        }
    }

    fun deleteRecurringItem(item: RecurringItem) {
        viewModelScope.launch {
            repository.deleteRecurringItem(item)
        }
    }

    fun addRecurringItem(
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        isAutoPay: Boolean,
        dueDate: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.addRecurringItem(
                RecurringItem(
                    title = title,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    isAutoPay = isAutoPay,
                    dueDate = dueDate,
                    status = "PENDING"
                )
            )
        }
    }

    fun addSampleRecurringItems() {
        viewModelScope.launch {
            val samples = listOf(
                RecurringItem(
                    title = "Netflix",
                    amount = 499.0,
                    dueDate = System.currentTimeMillis() + 86400000 * 2,
                    isAutoPay = true,
                    status = "PENDING",
                    frequency = "MONTHLY",
                    category = "Subscriptions",
                    paymentMethod = "Credit Card"
                ),
                RecurringItem(
                    title = "Electricity Bill",
                    amount = 1580.0,
                    dueDate = System.currentTimeMillis() + 86400000 * 5,
                    isAutoPay = false,
                    status = "PENDING",
                    frequency = "MONTHLY",
                    category = "Bills",
                    paymentMethod = "UPI"
                ),
                RecurringItem(
                    title = "Laptop EMI",
                    amount = 4250.0,
                    dueDate = System.currentTimeMillis() + 86400000 * 8,
                    isAutoPay = true,
                    status = "PENDING",
                    frequency = "MONTHLY",
                    category = "EMIs",
                    recurrenceCount = 12,
                    totalRecurrence = 24
                )
            )
            samples.forEach { repository.addRecurringItem(it) }
        }
    }
}
