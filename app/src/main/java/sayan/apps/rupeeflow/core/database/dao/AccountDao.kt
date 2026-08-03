package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.AccountEntity

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountByIdSync(id: Long): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT SUM(balance) FROM accounts")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT * FROM accounts WHERE name LIKE '%' || :query || '%' OR institutionName LIKE '%' || :query || '%' OR upiId LIKE '%' || :query || '%'")
    fun searchAccounts(query: String): Flow<List<AccountEntity>>
}
