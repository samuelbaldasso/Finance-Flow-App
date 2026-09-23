package com.samuelbaldasso.financeflow.core.model.goal

import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

enum class GoalStatus {
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}

data class Goal(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    val targetAmount: Money,
    val targetDate: Instant? = null,
    val linkedAccountId: UUID? = null,
    val status: GoalStatus = GoalStatus.IN_PROGRESS,
    val createdAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Goal name cannot be blank" }
        require(targetAmount.amountMinor > 0L) { "Goal target amount must be strictly positive" }
    }

    fun isTargetReached(currentSavedAmount: Money): Boolean =
        currentSavedAmount.amountMinor >= targetAmount.amountMinor

    fun progressPercentage(currentSavedAmount: Money): Float {
        if (targetAmount.amountMinor <= 0L) return 100f
        val pct = (currentSavedAmount.amountMinor.toFloat() / targetAmount.amountMinor.toFloat()) * 100f
        return pct.coerceIn(0f, 100f)
    }
}
