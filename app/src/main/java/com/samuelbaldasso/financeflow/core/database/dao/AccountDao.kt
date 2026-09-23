package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.AccountEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(account: AccountEntity)

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getById(id: UUID): AccountEntity?

    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE is_archived = 0 ORDER BY name ASC")
    fun getActiveAccountsFlow(): Flow<List<AccountEntity>>

    @Query("UPDATE accounts SET is_archived = 1, updated_at = :now WHERE id = :id")
    suspend fun softDelete(id: UUID, now: Long): Int

    @Query("UPDATE accounts SET is_archived = 0, updated_at = :now WHERE id = :id")
    suspend fun unarchive(id: UUID, now: Long): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE account_id = :accountId")
    suspend fun getTransactionCount(accountId: UUID): Int
}
