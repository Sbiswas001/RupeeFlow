package sayan.apps.rupeeflow.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sayan.apps.rupeeflow.domain.model.*
import sayan.apps.rupeeflow.domain.repository.AccountRepository
import sayan.apps.rupeeflow.domain.repository.CategoryRepository
import sayan.apps.rupeeflow.domain.repository.SearchRepository
import javax.inject.Inject

enum class SearchCategory {
    ALL, TRANSACTIONS, ACCOUNTS, RECURRING, CATEGORIES, PLANNING, BUDGETS, GOALS
}

data class SearchUiState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val filteredResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val filters: SearchFilters = SearchFilters(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val showFilters: Boolean = false,
    val recentSearches: List<String> = emptyList(),
    val activeCategory: SearchCategory = SearchCategory.ALL
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                categoryRepository.getCategories(),
                accountRepository.getAccounts(),
                searchRepository.getRecentSearches()
            ) { categories, accounts, recent ->
                _uiState.update { it.copy(
                    categories = categories,
                    accounts = accounts,
                    recentSearches = recent
                ) }
            }.collect()
        }

        observeSearch()
        observeFiltering()
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun observeSearch() {
        viewModelScope.launch {
            _uiState
                .map { it.query to it.filters }
                .distinctUntilChanged()
                .debounce(300)
                .onEach { (query, _) -> 
                    if (query.isNotEmpty()) {
                        _uiState.update { it.copy(isSearching = true) }
                    }
                }
                .flatMapLatest { (query, filters) ->
                    if (query.isEmpty() && isFiltersEmpty(filters)) {
                        flowOf(emptyList<SearchResult>())
                    } else {
                        searchRepository.globalSearch(filters.copy(query = query))
                    }
                }
                .collect { results ->
                    _uiState.update { it.copy(results = results, isSearching = false) }
                }
        }
    }

    private fun observeFiltering() {
        viewModelScope.launch {
            _uiState
                .map { it.results to it.activeCategory }
                .distinctUntilChanged()
                .collect { (results, category) ->
                    val filtered = when (category) {
                        SearchCategory.ALL -> results
                        SearchCategory.TRANSACTIONS -> results.filterIsInstance<SearchResult.TransactionResult>()
                        SearchCategory.ACCOUNTS -> results.filterIsInstance<SearchResult.AccountResult>()
                        SearchCategory.RECURRING -> results.filterIsInstance<SearchResult.RecurringResult>()
                        SearchCategory.CATEGORIES -> results.filterIsInstance<SearchResult.CategoryResult>()
                        SearchCategory.PLANNING -> results.filter { it is SearchResult.BudgetResult || it is SearchResult.GoalResult }
                        SearchCategory.BUDGETS -> results.filterIsInstance<SearchResult.BudgetResult>()
                        SearchCategory.GOALS -> results.filterIsInstance<SearchResult.GoalResult>()
                    }
                    _uiState.update { it.copy(filteredResults = filtered) }
                }
        }
    }

    private fun isFiltersEmpty(filters: SearchFilters): Boolean {
        return filters.minAmount == null && filters.maxAmount == null &&
                filters.startDate == null && filters.endDate == null &&
                filters.categoryId == null && filters.accountId == null &&
                filters.type == null && filters.isRecurring == null
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun onSearchAction(query: String) {
        viewModelScope.launch {
            searchRepository.saveSearch(query)
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            searchRepository.deleteSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            searchRepository.clearRecentSearches()
        }
    }

    fun onFilterChange(filters: SearchFilters) {
        _uiState.update { it.copy(filters = filters) }
    }

    fun toggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun setCategory(category: SearchCategory) {
        _uiState.update { it.copy(activeCategory = category) }
    }
}
