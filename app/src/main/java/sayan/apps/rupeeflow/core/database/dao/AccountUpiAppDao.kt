package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.AccountUpiAppEntity

@Dao
interface AccountUpiAppDao {
    @Query("SELECT * FROM account_upi_apps WHERE accountId IS NULL OR accountId = :accountId")
    fun getUpiAppsForAccount(accountId: Long): Flow<List<AccountUpiAppEntity>>

    @Query("SELECT * FROM account_upi_apps WHERE accountId IS NULL")
    fun getDefaultUpiApps(): Flow<List<AccountUpiAppEntity>>

    @Query("SELECT * FROM account_upi_apps WHERE id = :id")
    suspend fun getUpiAppById(id: Long): AccountUpiAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUpiApp(upiApp: AccountUpiAppEntity): Long

    @Update
    suspend fun updateUpiApp(upiApp: AccountUpiAppEntity)

    @Delete
    suspend fun deleteUpiApp(upiApp: AccountUpiAppEntity)
}
