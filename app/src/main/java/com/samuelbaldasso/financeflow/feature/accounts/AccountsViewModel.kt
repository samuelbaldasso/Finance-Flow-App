package com.samuelbaldasso.financeflow.feature.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.usecase.account.ArchiveAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.CreateAccountUseCase
import com.samuelbaldasso.financeflow.domain.usecase.account.GetAccountsWithBalanceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountsViewModel(
    private val getAccountsWithBalanceUseCase: GetAccountsWithBalanceUseCase,
    private val createAccountUseCase: CreateAccountUseCase,
    private val archiveAccountUseCase: ArchiveAccountUseCase
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    private val _showCreateDialog = MutableStateFlow(false)

    val uiState: StateFlow<AccountsUiState> = combine(
        getAccountsWithBalanceUseCase(),
        _showArchived,
        _showCreateDialog
    ) { accounts, showArchived, showDialog ->
        val filtered = if (showArchived) accounts else accounts.filter { !it.account.isArchived }

        val brlTotal = filtered
            .filter { it.account.currency == CurrencyCode.BRL && !it.account.isArchived }
            .sumOf { it.derivedBalance.amountMinor }

        val usdTotal = filtered
            .filter { it.account.currency == CurrencyCode.USD && !it.account.isArchived }
            .sumOf { it.derivedBalance.amountMinor }

        AccountsUiState(
            accounts = filtered,
            showArchived = showArchived,
            isLoading = false,
            totalBrlBalance = Money(brlTotal),
            totalUsdBalance = Money(usdTotal),
            showCreateDialog = showDialog
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsUiState(isLoading = true)
    )

    fun onEvent(event: AccountsUiEvent) {
        when (event) {
            AccountsUiEvent.ToggleShowArchived -> {
                _showArchived.value = !_showArchived.value
            }
            AccountsUiEvent.OpenCreateDialog -> {
                _showCreateDialog.value = true
            }
            AccountsUiEvent.DismissCreateDialog -> {
                _showCreateDialog.value = false
            }
            is AccountsUiEvent.ArchiveAccount -> {
                viewModelScope.launch {
                    archiveAccountUseCase(event.id)
                }
            }
            is AccountsUiEvent.UnarchiveAccount -> {
                // unarchive
            }
            is AccountsUiEvent.CreateAccount -> {
                viewModelScope.launch {
                    val account = Account(
                        name = event.name,
                        type = event.type,
                        currency = event.currency,
                        initialBalance = Money(event.initialBalanceMinor),
                        creditLimit = event.creditLimitMinor?.let { Money(it) },
                        closingDay = event.closingDay,
                        dueDay = event.dueDay
                    )
                    createAccountUseCase(account)
                    _showCreateDialog.value = false
                }
            }
        }
    }
}
