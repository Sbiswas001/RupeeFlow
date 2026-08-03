package sayan.apps.rupeeflow.domain.repository

import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.domain.model.SearchFilters
import sayan.apps.rupeeflow.domain.model.SearchResult

interface SearchRepository {
    fun globalSearch(filters: SearchFilters): Flow<List<SearchResult>>
    fun getRecentSearches(): Flow<List<String>>
    suspend fun saveSearch(query: String)
    suspend fun deleteSearch(query: String)
    suspend fun clearRecentSearches()
}
