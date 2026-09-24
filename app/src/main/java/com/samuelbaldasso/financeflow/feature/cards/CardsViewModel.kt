package com.samuelbaldasso.financeflow.feature.cards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import com.samuelbaldasso.financeflow.domain.usecase.card.CalculateAvailableLimitUseCase
import com.samuelbaldasso.financeflow.domain.usecase.card.CreateInstallmentPurchaseUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CardsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val calculateAvailableLimitUseCase: CalculateAvailableLimitUseCase,
    private val createInstallmentPurchaseUseCase: CreateInstallmentPurchaseUseCase,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _selectedCardIndex = MutableStateFlow(0)
    private val _showInstallmentDialog = MutableStateFlow(false)
    private val _showPayInvoiceDialog = MutableStateFlow(false)

    private val _dialogsState = combine(
        _selectedCardIndex,
        _showInstallmentDialog,
        _showPayInvoiceDialog
    ) { index, showInstallment, showPayInvoice ->
        Triple(index, showInstallment, showPayInvoice)
    }

    val uiState: StateFlow<CardsUiState> = combine(
        accountRepository.getActiveAccountsFlow(),
        categoryRepository.getAllCategoriesFlow(),
        transactionRepository.getAllTransactionsFlow(),
        _dialogsState
    ) { accounts, categories, allTransactions, dialogs ->
        val (selectedIndex, showInstallment, showPayInvoice) = dialogs
        val cardAccounts = accounts.filter { it.type == AccountType.CREDIT_CARD }
        val checkingAccounts = accounts.filter { it.type == AccountType.CHECKING }

        val cardDetails = cardAccounts.map { card ->
            val cardTxs = allTransactions.filter { it.accountId == card.id }
            val committedExpenses = cardTxs
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount.amountMinor }
            val totalLimit = card.creditLimit ?: Money.ZERO
            val availableLimit = Money((totalLimit.amountMinor - committedExpenses).coerceAtLeast(0L))
            val usedLimit = Money(committedExpenses)
            val installments = cardTxs.filter { it.installmentGroupId != null }

            CreditCardDetail(
                account = card,
                availableLimit = availableLimit,
                usedLimit = usedLimit,
                openInvoiceTotal = usedLimit,
                transactions = cardTxs,
                installments = installments
            )
        }

        CardsUiState(
            cards = cardDetails,
            selectedCardIndex = selectedIndex.coerceIn(0, (cardDetails.size - 1).coerceAtLeast(0)),
            checkingAccounts = checkingAccounts,
            availableCategories = categories,
            isLoading = false,
            showInstallmentDialog = showInstallment,
            showPayInvoiceDialog = showPayInvoice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CardsUiState(isLoading = true)
    )

    fun onEvent(event: CardsUiEvent) {
        when (event) {
            is CardsUiEvent.SelectCard -> {
                _selectedCardIndex.value = event.index
            }
            CardsUiEvent.OpenInstallmentDialog -> {
                _showInstallmentDialog.value = true
            }
            CardsUiEvent.DismissInstallmentDialog -> {
                _showInstallmentDialog.value = false
            }
            is CardsUiEvent.CreateInstallmentPurchase -> {
                val currentCard = uiState.value.selectedCard?.account ?: return
                viewModelScope.launch {
                    createInstallmentPurchaseUseCase(
                        cardAccount = currentCard,
                        totalAmount = Money(event.totalAmountMinor),
                        installmentsCount = event.installmentsCount,
                        firstPurchaseDate = LocalDate.now(),
                        description = event.description,
                        categoryId = event.categoryId
                    )
                    _showInstallmentDialog.value = false
                }
            }
            CardsUiEvent.OpenPayInvoiceDialog -> {
                _showPayInvoiceDialog.value = true
            }
            CardsUiEvent.DismissPayInvoiceDialog -> {
                _showPayInvoiceDialog.value = false
            }
            is CardsUiEvent.PayInvoice -> {
                val currentCard = uiState.value.selectedCard?.account ?: return
                viewModelScope.launch {
                    val transferId = UUID.randomUUID()
                    val debitTx = Transaction(
                        accountId = event.sourceAccountId,
                        destinationAccountId = currentCard.id,
                        type = TransactionType.TRANSFER,
                        amount = Money(event.amountMinor),
                        competenceDate = Instant.now(),
                        effectiveDate = Instant.now(),
                        description = "Pagamento de fatura: ${currentCard.name}",
                        transferId = transferId,
                        status = TransactionStatus.CLEARED
                    )
                    createTransactionUseCase(debitTx)
                    _showPayInvoiceDialog.value = false
                }
            }
        }
    }
}
