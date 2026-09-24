package com.samuelbaldasso.financeflow.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.budget.Budget
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.BudgetRepository
import com.samuelbaldasso.financeflow.domain.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _currentPeriod = MutableStateFlow(
        Pair(LocalDate.now().monthValue, LocalDate.now().year)
    )
    private val _showCreateDialog = MutableStateFlow(false)

    val uiState: StateFlow<BudgetsUiState> = combine(
        _currentPeriod,
        _showCreateDialog,
        categoryRepository.getAllCategoriesFlow()
    ) { (month, year), showDialog, categories ->
        Triple(Pair(month, year), showDialog, categories)
    }.flatMapLatest { (period, showDialog, categories) ->
        val (month, year) = period
        budgetRepository.getBudgetsWithProgressFlow(month, year).combine(
            _showCreateDialog
        ) { budgets, dialogVisible ->
            BudgetsUiState(
                month = month,
                year = year,
                budgets = budgets,
                availableCategories = categories,
                isLoading = false,
                showCreateDialog = dialogVisible
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BudgetsUiState(isLoading = true)
    )

    fun onEvent(event: BudgetsUiEvent) {
        when (event) {
            is BudgetsUiEvent.MonthChanged -> {
                _currentPeriod.value = Pair(event.month, event.year)
            }
            BudgetsUiEvent.PreviousMonth -> {
                _currentPeriod.update { (m, y) ->
                    if (m == 1) Pair(12, y - 1) else Pair(m - 1, y)
                }
            }
            BudgetsUiEvent.NextMonth -> {
                _currentPeriod.update { (m, y) ->
                    if (m == 12) Pair(1, y + 1) else Pair(m + 1, y)
                }
            }
            BudgetsUiEvent.OpenCreateDialog -> {
                _showCreateDialog.value = true
            }
            BudgetsUiEvent.DismissCreateDialog -> {
                _showCreateDialog.value = false
            }
            is BudgetsUiEvent.CreateBudget -> {
                viewModelScope.launch {
                    val (month, year) = _currentPeriod.value
                    val budget = Budget(
                        categoryId = event.categoryId,
                        periodMonth = month,
                        periodYear = year,
                        limitAmount = Money(event.amountMinor),
                        rolloverEnabled = event.rollover
                    )
                    budgetRepository.setBudget(budget)
                    _showCreateDialog.value = false
                }
            }
            is BudgetsUiEvent.DeleteBudget -> {
                viewModelScope.launch {
                    budgetRepository.deleteBudget(event.id)
                }
            }
        }
    }
}
