package com.samuelbaldasso.financeflow.domain.usecase.transaction

import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import java.util.UUID

class ReconcileTransactionUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: UUID) {
        transactionRepository.reconcileTransaction(id)
    }
}
