package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface TransactionRepository {
    fun getAllTransactionsFlow(): Flow<List<Transaction>>
    fun getTransactionsByAccountFlow(accountId: UUID): Flow<List<Transaction>>
    fun getGoalContributionsFlow(goalId: UUID): Flow<List<Transaction>>
    suspend fun getTransactionById(id: UUID): Transaction?
    suspend fun getTransactionsByTransferId(transferId: UUID): List<Transaction>
    suspend fun createTransaction(transaction: Transaction): Transaction
    suspend fun createTransfer(debitTx: Transaction, creditTx: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: UUID)
    suspend fun reconcileTransaction(id: UUID)
    suspend fun unreconcileTransaction(id: UUID)
}
