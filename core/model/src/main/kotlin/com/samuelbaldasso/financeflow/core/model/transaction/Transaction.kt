package com.samuelbaldasso.financeflow.core.model.transaction

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    ADJUSTMENT
}

enum class TransactionStatus {
    PENDING,
    CLEARED,
    RECONCILED
}

data class Transaction(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val type: TransactionType,
    val amount: Money,
    val competenceDate: Instant,
    val effectiveDate: Instant,
    val categoryId: UUID? = null,
    val description: String,
    val tags: List<String> = emptyList(),
    val attachmentPath: String? = null,
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val transferId: UUID? = null,
    val destinationAccountId: UUID? = null,
    val installmentGroupId: UUID? = null,
    val installmentNumber: Int? = null,
    val totalInstallments: Int? = null,
    val goalId: UUID? = null,
    val recurrenceRuleId: UUID? = null,
    val invoiceId: UUID? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        if (amount.amountMinor <= 0L) {
            throw DomainException.ZeroAmountException("Transaction amount must be strictly positive: ${amount.amountMinor}")
        }
        if (type == TransactionType.TRANSFER) {
            requireNotNull(transferId) { "Transfer transaction must have a transferId" }
        }
        if (installmentGroupId != null) {
            requireNotNull(installmentNumber) { "Installment transaction requires installmentNumber" }
            requireNotNull(totalInstallments) { "Installment transaction requires totalInstallments" }
            require(installmentNumber in 1..totalInstallments) {
                "Installment number $installmentNumber must be between 1 and $totalInstallments"
            }
        }
    }

    fun validateFutureDate(now: Instant) {
        if (effectiveDate.isAfter(now) && status != TransactionStatus.PENDING) {
            throw DomainException.InvalidFutureDateException(
                "Future dated transaction must have PENDING status. Date: $effectiveDate, Status: $status"
            )
        }
    }

    fun validateModificationAllowed() {
        if (status == TransactionStatus.RECONCILED) {
            throw DomainException.ReconciledTransactionLockedException(
                "Cannot modify reconciled transaction $id directly. Unreconcile with audit trail first."
            )
        }
    }

    val isTransfer: Boolean get() = type == TransactionType.TRANSFER
    val isInstallment: Boolean get() = installmentGroupId != null
    val isContribution: Boolean get() = goalId != null
}
