package com.samuelbaldasso.financeflow.core.model.recurrence

import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import java.time.LocalDate
import java.util.UUID

enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class RecurrenceRule(
    val id: UUID = UUID.randomUUID(),
    val accountId: UUID,
    val type: TransactionType,
    val amount: Money,
    val categoryId: UUID? = null,
    val description: String,
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val maxOccurrences: Int? = null
) {
    init {
        require(amount.amountMinor > 0L) { "Recurrence amount must be strictly positive" }
        require(interval >= 1) { "Recurrence interval must be >= 1" }
        if (endDate != null) {
            require(!endDate.isBefore(startDate)) { "End date cannot be before start date" }
        }
        if (maxOccurrences != null) {
            require(maxOccurrences >= 1) { "Max occurrences must be >= 1" }
        }
    }
}
