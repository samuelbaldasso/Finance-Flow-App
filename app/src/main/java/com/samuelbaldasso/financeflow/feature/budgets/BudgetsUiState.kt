package com.samuelbaldasso.financeflow.feature.budgets

import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.domain.repository.BudgetWithProgress
import java.time.LocalDate
import java.util.UUID

data class BudgetsUiState(
    val month: Int = LocalDate.now().monthValue,
    val year: Int = LocalDate.now().year,
    val budgets: List<BudgetWithProgress> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false
) {
    val totalBudgeted: Money
        get() = Money(budgets.sumOf { it.budget.effectiveLimit.amountMinor })

    val totalSpent: Money
        get() = Money(budgets.sumOf { it.spentAmount.amountMinor })

    val totalRemaining: Money
        get() = Money((totalBudgeted.amountMinor - totalSpent.amountMinor).coerceAtLeast(0L))

    val overallProgress: Float
        get() = if (totalBudgeted.amountMinor > 0L) {
            (totalSpent.amountMinor.toFloat() / totalBudgeted.amountMinor.toFloat()).coerceIn(0f, 1f)
        } else 0f

    val warningCount: Int
        get() = budgets.count { it.status == com.samuelbaldasso.financeflow.core.model.budget.BudgetStatus.WARNING }

    val exceededCount: Int
        get() = budgets.count { it.status == com.samuelbaldasso.financeflow.core.model.budget.BudgetStatus.EXCEEDED }
}

sealed interface BudgetsUiEvent {
    data class MonthChanged(val month: Int, val year: Int) : BudgetsUiEvent
    data object PreviousMonth : BudgetsUiEvent
    data object NextMonth : BudgetsUiEvent
    data object OpenCreateDialog : BudgetsUiEvent
    data object DismissCreateDialog : BudgetsUiEvent
    data class CreateBudget(
        val categoryId: UUID,
        val amountMinor: Long,
        val rollover: Boolean
    ) : BudgetsUiEvent
    data class DeleteBudget(val id: UUID) : BudgetsUiEvent
}
