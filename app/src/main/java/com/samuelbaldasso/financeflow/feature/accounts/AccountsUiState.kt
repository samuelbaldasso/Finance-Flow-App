package com.samuelbaldasso.financeflow.feature.accounts

import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.AccountWithBalance
import java.util.UUID

data class AccountsUiState(
    val accounts: List<AccountWithBalance> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = false,
    val totalBrlBalance: Money = Money.ZERO,
    val totalUsdBalance: Money = Money.ZERO,
    val showCreateDialog: Boolean = false
)

sealed interface AccountsUiEvent {
    data object ToggleShowArchived : AccountsUiEvent
    data object OpenCreateDialog : AccountsUiEvent
    data object DismissCreateDialog : AccountsUiEvent
    data class ArchiveAccount(val id: UUID) : AccountsUiEvent
    data class UnarchiveAccount(val id: UUID) : AccountsUiEvent
    data class CreateAccount(
        val name: String,
        val type: AccountType,
        val currency: CurrencyCode,
        val initialBalanceMinor: Long,
        val creditLimitMinor: Long? = null,
        val closingDay: Int? = null,
        val dueDay: Int? = null
    ) : AccountsUiEvent
}
