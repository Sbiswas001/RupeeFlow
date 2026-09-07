package sayan.apps.rupeeflow.core.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import sayan.apps.rupeeflow.core.database.entity.DebitCardEntity

@Dao
interface DebitCardDao {
    @Query("SELECT * FROM debit_cards WHERE accountId = :accountId")
    fun getDebitCardsForAccount(accountId: Long): Flow<List<DebitCardEntity>>

    @Query("SELECT * FROM debit_cards WHERE id = :id")
    suspend fun getDebitCardById(id: Long): DebitCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebitCard(debitCard: DebitCardEntity): Long

    @Update
    suspend fun updateDebitCard(debitCard: DebitCardEntity)

    @Delete
    suspend fun deleteDebitCard(debitCard: DebitCardEntity)
}
