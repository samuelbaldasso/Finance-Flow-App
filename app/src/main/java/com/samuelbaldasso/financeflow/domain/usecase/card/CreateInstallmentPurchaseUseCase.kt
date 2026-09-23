package com.samuelbaldasso.financeflow.domain.usecase.card

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

class CreateInstallmentPurchaseUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        cardAccount: Account,
        totalAmount: Money,
        installmentsCount: Int,
        firstPurchaseDate: LocalDate,
        description: String,
        categoryId: UUID? = null
    ): List<Transaction> {
        require(cardAccount.type == AccountType.CREDIT_CARD) { "Account must be a credit card" }
        require(installmentsCount in 1..48) { "Installments must be between 1 and 48" }
        require(totalAmount.amountMinor > 0L) { "Amount must be strictly positive" }

        val totalMinor = totalAmount.amountMinor
        val baseInstallment = totalMinor / installmentsCount
        val remainder = totalMinor % installmentsCount

        val installmentGroupId = UUID.randomUUID()
        val createdTransactions = mutableListOf<Transaction>()

        val closingDay = cardAccount.closingDay ?: 1
        val dueDay = cardAccount.dueDay ?: 10

        var currentCycleDate = firstPurchaseDate

        for (i in 1..installmentsCount) {
            val installmentAmountMinor = if (i <= remainder) baseInstallment + 1 else baseInstallment
            val cycle = CreditCardBillingCycle.calculateCycle(currentCycleDate, closingDay, dueDay)
            val effectiveInstant = cycle.closingDate.atStartOfDay(ZoneOffset.UTC).toInstant()

            val tx = Transaction(
                id = UUID.randomUUID(),
                accountId = cardAccount.id,
                type = TransactionType.EXPENSE,
                amount = Money(installmentAmountMinor),
                competenceDate = currentCycleDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
                effectiveDate = effectiveInstant,
                categoryId = categoryId,
                description = "$description ($i/$installmentsCount)",
                status = if (effectiveInstant.isAfter(Instant.now())) TransactionStatus.PENDING else TransactionStatus.CLEARED,
                installmentGroupId = installmentGroupId,
                installmentNumber = i,
                totalInstallments = installmentsCount
            )

            transactionRepository.createTransaction(tx)
            createdTransactions.add(tx)

            // Step to next month's purchase date
            currentCycleDate = currentCycleDate.plusMonths(1)
        }

        return createdTransactions
    }
}
