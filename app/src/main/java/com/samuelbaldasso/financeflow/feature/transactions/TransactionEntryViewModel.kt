package com.samuelbaldasso.financeflow.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TransactionEntryViewModel(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionEntryUiState())
    val uiState = _uiState.asStateFlow()

    private val _effect = Channel<TransactionEntryUiEffect>()
    val effect = _effect.receiveAsFlow()

    init {
        viewModelScope.launch {
            accountRepository.getActiveAccountsFlow().collect { accounts ->
                _uiState.update { current ->
                    val selected = current.selectedAccountId ?: accounts.firstOrNull()?.id
                    current.copy(
                        accounts = accounts,
                        selectedAccountId = selected
                    )
                }
            }
        }

        viewModelScope.launch {
            categoryRepository.getAllCategoriesFlow().collect { categories ->
                _uiState.update { current ->
                    current.copy(
                        categories = categories.filter {
                            it.type.name == current.type.name || current.type == TransactionType.TRANSFER
                        }
                    )
                }
            }
        }
    }

    fun onEvent(event: TransactionEntryUiEvent) {
        when (event) {
            is TransactionEntryUiEvent.AmountChanged -> {
                _uiState.update { it.copy(amountMinor = event.amountMinor) }
            }
            is TransactionEntryUiEvent.TypeChanged -> {
                _uiState.update { current ->
                    current.copy(
                        type = event.type,
                        selectedCategoryId = null
                    )
                }
            }
            is TransactionEntryUiEvent.AccountSelected -> {
                _uiState.update { it.copy(selectedAccountId = event.accountId) }
            }
            is TransactionEntryUiEvent.DestinationAccountSelected -> {
                _uiState.update { it.copy(destinationAccountId = event.accountId) }
            }
            is TransactionEntryUiEvent.CategorySelected -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
            }
            is TransactionEntryUiEvent.DescriptionChanged -> {
                _uiState.update { it.copy(description = event.desc) }
            }
            is TransactionEntryUiEvent.DateChanged -> {
                _uiState.update { it.copy(competenceDate = event.date) }
            }
            TransactionEntryUiEvent.Submit -> submitTransaction()
        }
    }

    private fun submitTransaction() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(errorMessage = "Preencha todos os campos obrigatórios") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val competenceInstant = state.competenceDate.atStartOfDay(ZoneOffset.UTC).toInstant()
                val isFuture = competenceInstant.isAfter(Instant.now())
                val status = if (isFuture) TransactionStatus.PENDING else TransactionStatus.CLEARED

                val tx = Transaction(
                    id = UUID.randomUUID(),
                    accountId = state.selectedAccountId!!,
                    type = state.type,
                    amount = Money(state.amountMinor),
                    competenceDate = competenceInstant,
                    effectiveDate = competenceInstant,
                    categoryId = state.selectedCategoryId,
                    description = state.description.trim(),
                    destinationAccountId = if (state.type == TransactionType.TRANSFER) state.destinationAccountId else null,
                    status = status
                )

                createTransactionUseCase(tx)
                _effect.send(TransactionEntryUiEffect.TransactionSaved)
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Erro ao salvar transação") }
                _effect.send(TransactionEntryUiEffect.ShowError(e.message ?: "Erro"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
