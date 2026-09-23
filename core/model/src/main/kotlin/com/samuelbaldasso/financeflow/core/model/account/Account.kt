package com.samuelbaldasso.financeflow.core.model.account

import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.CurrencedMoney
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

enum class AccountType {
    CHECKING,
    SAVINGS,
    CASH,
    INVESTMENT,
    CREDIT_CARD
}

data class Account(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: AccountType,
    val currency: CurrencyCode,
    val initialBalance: Money = Money.ZERO,
    val isArchived: Boolean = false,
    val colorHex: String? = null,
    val iconKey: String? = null,
    val creditLimit: Money? = null,
    val closingDay: Int? = null,
    val dueDay: Int? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Account name cannot be blank" }
        if (type == AccountType.CREDIT_CARD) {
            requireNotNull(closingDay) { "Credit card must have a closing day" }
            requireNotNull(dueDay) { "Credit card must have a due day" }
            require(closingDay in 1..31) { "Closing day must be between 1 and 31" }
            require(dueDay in 1..31) { "Due day must be between 1 and 31" }
        }
    }

    val initialCurrencedBalance: CurrencedMoney
        get() = CurrencedMoney(initialBalance, currency)
}
