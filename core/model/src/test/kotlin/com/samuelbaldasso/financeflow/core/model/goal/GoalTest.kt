package com.samuelbaldasso.financeflow.core.model.goal

import com.samuelbaldasso.financeflow.core.model.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalTest {

    @Test
    fun `test goal progress calculation and completion status`() {
        val goal = Goal(
            name = "Reserva de Emergência",
            targetAmount = Money(1000000L) // R$ 10.000,00
        )

        assertFalse(goal.isTargetReached(Money(500000L)))
        assertEquals(50.0f, goal.progressPercentage(Money(500000L)), 0.01f)

        assertTrue(goal.isTargetReached(Money(1000000L)))
        assertEquals(100.0f, goal.progressPercentage(Money(1000000L)), 0.01f)

        // Exceeded still clamps to 100% progress display
        assertTrue(goal.isTargetReached(Money(1200000L)))
        assertEquals(100.0f, goal.progressPercentage(Money(1200000L)), 0.01f)
    }
}
