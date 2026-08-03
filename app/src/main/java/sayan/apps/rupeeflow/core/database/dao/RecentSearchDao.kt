package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.RecentSearchEntity

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(search: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE query = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearAll()
}
