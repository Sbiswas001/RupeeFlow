package sayan.apps.rupeeflow.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.Category
import sayan.apps.rupeeflow.domain.model.Transaction
import sayan.apps.rupeeflow.domain.model.TransactionType
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.TransactionRepository
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val searchQuery: String = "",
    val filterType: CategoryFilter = CategoryFilter.ALL,
    val showArchived: Boolean = false,
    val isLoading: Boolean = false
)

data class CategoryStats(
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val totalCount: Int = 0,
    val archivedCount: Int = 0
)

enum class CategoryFilter {
    ALL, EXPENSE, INCOME, BUDGETED, UNUSED, ARCHIVED
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _filterType = MutableStateFlow(CategoryFilter.ALL)
    val filterType = _filterType.asStateFlow()

    private val _showArchived = MutableStateFlow(false)

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getCategories(),
        _searchQuery,
        _filterType,
        _showArchived
    ) { categories, query, filter, showArchived ->
        val filtered = categories.filter { category ->
            val matchesQuery = category.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                CategoryFilter.ALL -> !category.isArchived || showArchived
                CategoryFilter.EXPENSE -> category.type == TransactionType.EXPENSE && (!category.isArchived || showArchived)
                CategoryFilter.INCOME -> category.type == TransactionType.INCOME && (!category.isArchived || showArchived)
                CategoryFilter.ARCHIVED -> category.isArchived
                else -> !category.isArchived
            }
            matchesQuery && matchesFilter
        }
        CategoryUiState(
            categories = filtered,
            searchQuery = query,
            filterType = filter,
            showArchived = showArchived
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    val stats: StateFlow<CategoryStats> = categoryRepository.getCategories().map { categories ->
        CategoryStats(
            expenseCount = categories.count { it.type == TransactionType.EXPENSE && !it.isArchived },
            incomeCount = categories.count { it.type == TransactionType.INCOME && !it.isArchived },
            totalCount = categories.count { !it.isArchived },
            archivedCount = categories.count { it.isArchived }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryStats())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: CategoryFilter) {
        _filterType.value = filter
        if (filter == CategoryFilter.ARCHIVED) {
            _showArchived.value = true
        }
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun archiveCategory(id: Long) {
        viewModelScope.launch {
            categoryRepository.setArchived(id, true)
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun addCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.addCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }
    
    fun getCategoryStatsFlow(categoryId: Long) = combine(
        transactionRepository.getTransactionCountForCategory(categoryId),
        transactionRepository.getTotalAmountForCategory(categoryId)
    ) { count, total ->
        Pair(count, total ?: 0.0)
    }

    fun getTransactionsForCategory(categoryId: Long): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsForCategory(categoryId)
    }

    suspend fun getCategoryById(id: Long): Category? {
        return categoryRepository.getCategoryById(id)
    }
}
