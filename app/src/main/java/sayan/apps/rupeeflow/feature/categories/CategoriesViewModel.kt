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
    val isLoading: Boolean = false
)

data class CategoryStats(
    val expenseCount: Int = 0,
    val incomeCount: Int = 0,
    val totalCount: Int = 0
)

enum class CategoryFilter {
    ALL, EXPENSE, INCOME, BUDGETED, UNUSED
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

    val uiState: StateFlow<CategoryUiState> = combine(
        categoryRepository.getCategories(),
        _searchQuery,
        _filterType
    ) { categories, query, filter ->
        val filtered = categories.filter { category ->
            val matchesQuery = category.name.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                CategoryFilter.ALL -> true
                CategoryFilter.EXPENSE -> category.type == TransactionType.EXPENSE
                CategoryFilter.INCOME -> category.type == TransactionType.INCOME
                else -> true
            }
            matchesQuery && matchesFilter
        }
        CategoryUiState(
            categories = filtered,
            searchQuery = query,
            filterType = filter
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    val stats: StateFlow<CategoryStats> = categoryRepository.getCategories().map { categories ->
        CategoryStats(
            expenseCount = categories.count { it.type == TransactionType.EXPENSE },
            incomeCount = categories.count { it.type == TransactionType.INCOME },
            totalCount = categories.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryStats())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: CategoryFilter) {
        _filterType.value = filter
    }

    suspend fun isCategoryInUse(categoryId: Long): Boolean {
        return categoryRepository.isCategoryInUse(categoryId)
    }

    fun deleteCategory(category: Category, targetCategoryId: Long? = null) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category, targetCategoryId)
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
