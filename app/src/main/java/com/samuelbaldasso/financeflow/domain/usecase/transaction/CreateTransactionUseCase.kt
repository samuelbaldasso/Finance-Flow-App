package com.samuelbaldasso.financeflow.domain.usecase.transaction

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import java.util.UUID

class CreateTransactionUseCase(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(transaction: Transaction): Transaction {
        // Validate account is not archived
        val account = accountRepository.getAccountById(transaction.accountId)
            ?: throw IllegalArgumentException("Account ${transaction.accountId} not found")
        if (account.isArchived) {
            throw DomainException.AccountArchivedException("Cannot add transaction to archived account ${account.name}")
        }

        if (transaction.type == TransactionType.TRANSFER) {
            val destId = transaction.destinationAccountId
                ?: throw IllegalArgumentException("Transfer requires destinationAccountId")
            val destAccount = accountRepository.getAccountById(destId)
                ?: throw IllegalArgumentException("Destination account $destId not found")
            if (destAccount.isArchived) {
                throw DomainException.AccountArchivedException("Cannot transfer to archived account ${destAccount.name}")
            }

            val transferId = transaction.transferId ?: UUID.randomUUID()
            val debitLeg = transaction.copy(transferId = transferId, destinationAccountId = destId)
            val creditLeg = transaction.copy(
                id = UUID.randomUUID(),
                accountId = destId,
                destinationAccountId = null,
                transferId = transferId,
                description = "Transferência recebida: ${transaction.description}"
            )

            transactionRepository.createTransfer(debitLeg, creditLeg)
            return debitLeg
        } else {
            return transactionRepository.createTransaction(transaction)
        }
    }
}
