package com.samuelbaldasso.financeflow.core.model.budget

import com.samuelbaldasso.financeflow.core.model.money.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class BudgetTest {

    @Test
    fun `test budget threshold calculation`() {
        val budget = Budget(
            categoryId = UUID.randomUUID(),
            periodMonth = 10,
            periodYear = 2026,
            limitAmount = Money(100000L) // R$ 1.000,00
        )

        // Spent 700 -> OK (<80%)
        assertEquals(BudgetStatus.OK, budget.calculateStatus(Money(70000L)))

        // Spent 850 -> WARNING (85%)
        assertEquals(BudgetStatus.WARNING, budget.calculateStatus(Money(85000L)))

        // Spent 1000 -> EXCEEDED (100%)
        assertEquals(BudgetStatus.EXCEEDED, budget.calculateStatus(Money(100000L)))

        // Spent 1200 -> EXCEEDED (120%)
        assertEquals(BudgetStatus.EXCEEDED, budget.calculateStatus(Money(120000L)))
    }

    @Test
    fun `test budget rollover adds to effective limit`() {
        val budgetWithRollover = Budget(
            categoryId = UUID.randomUUID(),
            periodMonth = 10,
            periodYear = 2026,
            limitAmount = Money(100000L),
            rolloverEnabled = true,
            previousRolloverAmount = Money(20000L) // R$ 200,00 saved previous month
        )

        // Effective limit is 1.200,00
        assertEquals(120000L, budgetWithRollover.effectiveLimit.amountMinor)

        // Spent 900 -> 900 / 1200 = 75% -> OK
        assertEquals(BudgetStatus.OK, budgetWithRollover.calculateStatus(Money(90000L)))
        assertEquals(30000L, budgetWithRollover.remainingAmount(Money(90000L)).amountMinor)
    }
}
