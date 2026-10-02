package com.samuelbaldasso.financeflow.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.samuelbaldasso.financeflow.core.database.entity.CreditCardInvoiceEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CreditCardInvoiceDao {

    @Query("SELECT * FROM credit_card_invoices ORDER BY closing_date DESC")
    suspend fun getAll(): List<CreditCardInvoiceEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(invoice: CreditCardInvoiceEntity)

    @Update
    suspend fun update(invoice: CreditCardInvoiceEntity)

    @Query("SELECT * FROM credit_card_invoices WHERE id = :id")
    suspend fun getById(id: UUID): CreditCardInvoiceEntity?

    @Query("SELECT * FROM credit_card_invoices WHERE card_account_id = :cardAccountId AND status = 'OPEN' LIMIT 1")
    suspend fun getOpenInvoiceForCard(cardAccountId: UUID): CreditCardInvoiceEntity?

    @Query("SELECT * FROM credit_card_invoices WHERE card_account_id = :cardAccountId ORDER BY closing_date DESC")
    fun getInvoicesForCardFlow(cardAccountId: UUID): Flow<List<CreditCardInvoiceEntity>>
}
