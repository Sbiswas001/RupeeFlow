package sayan.apps.rupeeflow.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
