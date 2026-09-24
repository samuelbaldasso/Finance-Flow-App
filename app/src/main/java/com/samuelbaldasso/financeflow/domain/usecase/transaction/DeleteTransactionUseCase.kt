package com.samuelbaldasso.financeflow.domain.usecase.transaction

import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import java.util.UUID
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(id: UUID) {
        transactionRepository.deleteTransaction(id)
    }
}
