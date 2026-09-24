package com.samuelbaldasso.financeflow.feature.cards

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import java.util.UUID

data class CreditCardDetail(
    val account: Account,
    val availableLimit: Money,
    val usedLimit: Money,
    val openInvoiceTotal: Money,
    val transactions: List<Transaction>,
    val installments: List<Transaction>
)

data class CardsUiState(
    val cards: List<CreditCardDetail> = emptyList(),
    val selectedCardIndex: Int = 0,
    val checkingAccounts: List<Account> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val showInstallmentDialog: Boolean = false,
    val showPayInvoiceDialog: Boolean = false
) {
    val selectedCard: CreditCardDetail?
        get() = cards.getOrNull(selectedCardIndex) ?: cards.firstOrNull()
}

sealed interface CardsUiEvent {
    data class SelectCard(val index: Int) : CardsUiEvent
    data object OpenInstallmentDialog : CardsUiEvent
    data object DismissInstallmentDialog : CardsUiEvent
    data class CreateInstallmentPurchase(
        val description: String,
        val totalAmountMinor: Long,
        val installmentsCount: Int,
        val categoryId: UUID?
    ) : CardsUiEvent
    data object OpenPayInvoiceDialog : CardsUiEvent
    data object DismissPayInvoiceDialog : CardsUiEvent
    data class PayInvoice(
        val sourceAccountId: UUID,
        val amountMinor: Long
    ) : CardsUiEvent
}
