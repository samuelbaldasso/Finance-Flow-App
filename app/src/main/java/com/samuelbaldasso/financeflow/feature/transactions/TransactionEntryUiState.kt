package com.samuelbaldasso.financeflow.feature.transactions

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import java.time.LocalDate
import java.util.UUID

data class TransactionEntryUiState(
    val amountMinor: Long = 0L,
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: UUID? = null,
    val destinationAccountId: UUID? = null,
    val selectedCategoryId: UUID? = null,
    val description: String = "",
    val status: TransactionStatus = TransactionStatus.CLEARED,
    val competenceDate: LocalDate = LocalDate.now(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedAccount: Account? get() = accounts.find { it.id == selectedAccountId }
    val currency: CurrencyCode get() = selectedAccount?.currency ?: CurrencyCode.BRL
    val money: Money get() = Money(amountMinor)
    val isValid: Boolean get() = amountMinor > 0L && selectedAccountId != null && description.isNotBlank() &&
            (type != TransactionType.TRANSFER || (destinationAccountId != null && destinationAccountId != selectedAccountId))
}

sealed interface TransactionEntryUiEvent {
    data class AmountChanged(val amountMinor: Long) : TransactionEntryUiEvent
    data class TypeChanged(val type: TransactionType) : TransactionEntryUiEvent
    data class AccountSelected(val accountId: UUID) : TransactionEntryUiEvent
    data class DestinationAccountSelected(val accountId: UUID) : TransactionEntryUiEvent
    data class CategorySelected(val categoryId: UUID?) : TransactionEntryUiEvent
    data class DescriptionChanged(val desc: String) : TransactionEntryUiEvent
    data class DateChanged(val date: LocalDate) : TransactionEntryUiEvent
    data object Submit : TransactionEntryUiEvent
}

sealed interface TransactionEntryUiEffect {
    data object TransactionSaved : TransactionEntryUiEffect
    data class ShowError(val message: String) : TransactionEntryUiEffect
}
