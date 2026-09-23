package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class TransactionDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    abstract suspend fun update(transaction: TransactionEntity)

    @Delete
    abstract suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    abstract suspend fun getById(id: UUID): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE account_id = :accountId ORDER BY effective_date DESC")
    abstract fun getByAccountFlow(accountId: UUID): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY effective_date DESC")
    abstract fun getAllFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE transfer_id = :transferId")
    abstract suspend fun getByTransferId(transferId: UUID): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE transfer_id = :transferId")
    abstract suspend fun deleteByTransferId(transferId: UUID): Int

    @Query("SELECT * FROM transactions WHERE installment_group_id = :groupId ORDER BY installment_number ASC")
    abstract suspend fun getByInstallmentGroupId(groupId: UUID): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE installment_group_id = :groupId AND invoice_id IS NULL")
    abstract suspend fun deleteUninvoicedInstallments(groupId: UUID): Int

    @Query("SELECT * FROM transactions WHERE goal_id = :goalId ORDER BY effective_date ASC")
    abstract fun getGoalContributionsFlow(goalId: UUID): Flow<List<TransactionEntity>>

    @Query("""
        SELECT COALESCE(SUM(
            CASE 
                WHEN type = 'INCOME' THEN amount_minor
                WHEN type = 'EXPENSE' THEN -amount_minor
                WHEN type = 'ADJUSTMENT' THEN amount_minor
                WHEN type = 'TRANSFER' AND destination_account_id IS NOT NULL THEN -amount_minor
                WHEN type = 'TRANSFER' AND destination_account_id IS NULL THEN amount_minor
                ELSE 0
            END
        ), 0)
        FROM transactions 
        WHERE account_id = :accountId
    """)
    abstract fun getDerivedBalanceSumMinorFlow(accountId: UUID): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount_minor), 0)
        FROM transactions
        WHERE category_id = :categoryId 
          AND type = 'EXPENSE'
          AND effective_date >= :startEpochMillis 
          AND effective_date <= :endEpochMillis
    """)
    abstract fun getSpentForCategoryInPeriod(
        categoryId: UUID,
        startEpochMillis: Long,
        endEpochMillis: Long
    ): Flow<Long>

    @Query("""
        SELECT COALESCE(SUM(amount_minor), 0)
        FROM transactions
        WHERE goal_id = :goalId
    """)
    abstract fun getGoalContributionsSumMinorFlow(goalId: UUID): Flow<Long>

    @Transaction
    open suspend fun executeAtomicTransfer(debitTx: TransactionEntity, creditTx: TransactionEntity) {
        insert(debitTx)
        insert(creditTx)
    }

    @Transaction
    open suspend fun updateAtomicTransfer(debitTx: TransactionEntity, creditTx: TransactionEntity) {
        update(debitTx)
        update(creditTx)
    }
}
