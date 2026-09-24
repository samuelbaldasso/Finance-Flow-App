package com.samuelbaldasso.financeflow.feature.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.goal.Goal
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalRepository
import com.samuelbaldasso.financeflow.domain.repository.GoalWithProgress
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset

class GoalsViewModel(
    private val goalRepository: GoalRepository,
    private val accountRepository: AccountRepository,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _showCreateDialog = MutableStateFlow(false)
    private val _contributeGoal = MutableStateFlow<GoalWithProgress?>(null)

    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getGoalsWithProgressFlow(),
        accountRepository.getActiveAccountsFlow(),
        _showCreateDialog,
        _contributeGoal
    ) { goals, accounts, showDialog, contribute ->
        GoalsUiState(
            goals = goals,
            accounts = accounts,
            isLoading = false,
            showCreateDialog = showDialog,
            contributeGoal = contribute
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GoalsUiState(isLoading = true)
    )

    fun onEvent(event: GoalsUiEvent) {
        when (event) {
            GoalsUiEvent.OpenCreateDialog -> {
                _showCreateDialog.value = true
            }
            GoalsUiEvent.DismissCreateDialog -> {
                _showCreateDialog.value = false
            }
            is GoalsUiEvent.CreateGoal -> {
                viewModelScope.launch {
                    val targetInstant = event.targetDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()
                    val goal = Goal(
                        name = event.name,
                        targetAmount = Money(event.targetAmountMinor),
                        targetDate = targetInstant,
                        linkedAccountId = event.linkedAccountId
                    )
                    goalRepository.createGoal(goal)
                    _showCreateDialog.value = false
                }
            }
            is GoalsUiEvent.OpenContributeDialog -> {
                _contributeGoal.value = event.goal
            }
            GoalsUiEvent.DismissContributeDialog -> {
                _contributeGoal.value = null
            }
            is GoalsUiEvent.SubmitContribution -> {
                viewModelScope.launch {
                    val tx = Transaction(
                        accountId = event.accountId,
                        type = TransactionType.EXPENSE,
                        amount = Money(event.amountMinor),
                        competenceDate = Instant.now(),
                        effectiveDate = Instant.now(),
                        description = "Aporte: ${_contributeGoal.value?.goal?.name ?: "Meta"}",
                        goalId = event.goalId,
                        status = TransactionStatus.CLEARED
                    )
                    createTransactionUseCase(tx)
                    _contributeGoal.value = null
                }
            }
            is GoalsUiEvent.DeleteGoal -> {
                viewModelScope.launch {
                    goalRepository.deleteGoal(event.id)
                }
            }
        }
    }
}
