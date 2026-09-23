package com.samuelbaldasso.financeflow.domain.usecase.card

import java.time.LocalDate

data class InvoiceCycle(
    val closingDate: LocalDate,
    val dueDate: LocalDate
)

object CreditCardBillingCycle {

    fun calculateCycle(
        transactionDate: LocalDate,
        closingDay: Int,
        dueDay: Int
    ): InvoiceCycle {
        val effectiveClosingMonth = if (transactionDate.dayOfMonth > closingDay) {
            transactionDate.plusMonths(1)
        } else {
            transactionDate
        }

        val clampedClosingDay = minOf(closingDay, effectiveClosingMonth.lengthOfMonth())
        val closingDate = effectiveClosingMonth.withDayOfMonth(clampedClosingDay)

        val dueMonth = if (dueDay <= closingDay) {
            closingDate.plusMonths(1)
        } else {
            closingDate
        }

        val clampedDueDay = minOf(dueDay, dueMonth.lengthOfMonth())
        val dueDate = dueMonth.withDayOfMonth(clampedDueDay)

        return InvoiceCycle(closingDate = closingDate, dueDate = dueDate)
    }
}
