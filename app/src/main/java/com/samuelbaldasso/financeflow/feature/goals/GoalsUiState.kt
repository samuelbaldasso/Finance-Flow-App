package com.samuelbaldasso.financeflow.feature.goals

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.GoalWithProgress
import java.time.LocalDate
import java.util.UUID

data class GoalsUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val contributeGoal: GoalWithProgress? = null
) {
    val totalTarget: Money
        get() = Money(goals.sumOf { it.goal.targetAmount.amountMinor })

    val totalSaved: Money
        get() = Money(goals.sumOf { it.currentSavedAmount.amountMinor })

    val completedGoalsCount: Int
        get() = goals.count { it.isCompleted }

    val activeGoalsCount: Int
        get() = goals.count { !it.isCompleted }

    val overallProgress: Float
        get() = if (totalTarget.amountMinor > 0L) {
            (totalSaved.amountMinor.toFloat() / totalTarget.amountMinor.toFloat()).coerceIn(0f, 1f)
        } else 0f
}

sealed interface GoalsUiEvent {
    data object OpenCreateDialog : GoalsUiEvent
    data object DismissCreateDialog : GoalsUiEvent
    data class CreateGoal(
        val name: String,
        val targetAmountMinor: Long,
        val targetDate: LocalDate?,
        val linkedAccountId: UUID?
    ) : GoalsUiEvent
    data class OpenContributeDialog(val goal: GoalWithProgress) : GoalsUiEvent
    data object DismissContributeDialog : GoalsUiEvent
    data class SubmitContribution(
        val goalId: UUID,
        val accountId: UUID,
        val amountMinor: Long
    ) : GoalsUiEvent
    data class DeleteGoal(val id: UUID) : GoalsUiEvent
}
