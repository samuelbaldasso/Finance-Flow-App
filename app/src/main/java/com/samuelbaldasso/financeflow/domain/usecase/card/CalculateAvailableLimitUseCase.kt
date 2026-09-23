package com.samuelbaldasso.financeflow.domain.usecase.card

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

class CalculateAvailableLimitUseCase(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(cardAccount: Account): Money {
        require(cardAccount.type == AccountType.CREDIT_CARD) { "Account must be a credit card" }
        val creditLimit = cardAccount.creditLimit?.amountMinor ?: 0L

        val transactions = transactionRepository.getTransactionsByAccountFlow(cardAccount.id).first()
        val committedMinor = transactions
            .filter { it.type == com.samuelbaldasso.financeflow.core.model.transaction.TransactionType.EXPENSE }
            .sumOf { it.amount.amountMinor }

        val availableMinor = creditLimit - committedMinor
        return Money(availableMinor)
    }
}
