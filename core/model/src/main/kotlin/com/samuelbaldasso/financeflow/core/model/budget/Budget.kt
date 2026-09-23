package com.samuelbaldasso.financeflow.core.model.budget

import com.samuelbaldasso.financeflow.core.model.money.Money
import java.util.UUID

enum class BudgetStatus {
    OK,       // < 80%
    WARNING,  // 80% - 99%
    EXCEEDED  // >= 100%
}

data class Budget(
    val id: UUID = UUID.randomUUID(),
    val categoryId: UUID,
    val periodMonth: Int,
    val periodYear: Int,
    val limitAmount: Money,
    val rolloverEnabled: Boolean = false,
    val previousRolloverAmount: Money = Money.ZERO
) {
    init {
        require(periodMonth in 1..12) { "Month must be between 1 and 12" }
        require(periodYear > 2000) { "Year must be greater than 2000" }
        require(limitAmount.amountMinor > 0L) { "Budget limit must be greater than zero" }
    }

    val effectiveLimit: Money get() = limitAmount + previousRolloverAmount

    fun calculateStatus(spentAmount: Money): BudgetStatus {
        val effective = effectiveLimit.amountMinor
        if (effective <= 0L) return BudgetStatus.EXCEEDED

        val spent = spentAmount.amountMinor
        val percentage = (spent.toDouble() / effective.toDouble()) * 100.0

        return when {
            percentage >= 100.0 -> BudgetStatus.EXCEEDED
            percentage >= 80.0 -> BudgetStatus.WARNING
            else -> BudgetStatus.OK
        }
    }

    fun remainingAmount(spentAmount: Money): Money {
        val remaining = effectiveLimit.amountMinor - spentAmount.amountMinor
        return Money(remaining)
    }
}
