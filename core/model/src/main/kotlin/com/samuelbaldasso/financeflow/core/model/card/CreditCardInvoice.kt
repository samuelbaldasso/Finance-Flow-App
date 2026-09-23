package com.samuelbaldasso.financeflow.core.model.card

import com.samuelbaldasso.financeflow.core.model.money.Money
import java.time.Instant
import java.util.UUID

enum class InvoiceStatus {
    OPEN,
    CLOSED,
    PAID,
    OVERDUE
}

data class CreditCardInvoice(
    val id: UUID = UUID.randomUUID(),
    val cardAccountId: UUID,
    val closingDate: Instant,
    val dueDate: Instant,
    val totalAmount: Money = Money.ZERO,
    val paidAmount: Money = Money.ZERO,
    val status: InvoiceStatus = InvoiceStatus.OPEN
) {
    val remainingToPay: Money
        get() = Money((totalAmount.amountMinor - paidAmount.amountMinor).coerceAtLeast(0L))

    val isFullyPaid: Boolean
        get() = paidAmount.amountMinor >= totalAmount.amountMinor && totalAmount.amountMinor > 0L
}
