package com.samuelbaldasso.financeflow.core.model.transaction

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import com.samuelbaldasso.financeflow.core.model.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class TransactionTest {

    private val sampleAccountId = UUID.randomUUID()

    @Test
    fun `test zero or negative amount throws ZeroAmountException`() {
        assertThrows(DomainException.ZeroAmountException::class.java) {
            Transaction(
                accountId = sampleAccountId,
                type = TransactionType.EXPENSE,
                amount = Money(0L),
                competenceDate = Instant.now(),
                effectiveDate = Instant.now(),
                description = "Coffee"
            )
        }

        assertThrows(DomainException.ZeroAmountException::class.java) {
            Transaction(
                accountId = sampleAccountId,
                type = TransactionType.INCOME,
                amount = Money(-500L),
                competenceDate = Instant.now(),
                effectiveDate = Instant.now(),
                description = "Salary"
            )
        }
    }

    @Test
    fun `test future effective date without PENDING status throws InvalidFutureDateException`() {
        val future = Instant.now().plus(5, ChronoUnit.DAYS)
        val tx = Transaction(
            accountId = sampleAccountId,
            type = TransactionType.EXPENSE,
            amount = Money(5000L),
            competenceDate = future,
            effectiveDate = future,
            description = "Upcoming bill",
            status = TransactionStatus.CLEARED
        )

        assertThrows(DomainException.InvalidFutureDateException::class.java) {
            tx.validateFutureDate(Instant.now())
        }
    }

    @Test
    fun `test reconciled transaction cannot be modified directly`() {
        val tx = Transaction(
            accountId = sampleAccountId,
            type = TransactionType.EXPENSE,
            amount = Money(5000L),
            competenceDate = Instant.now(),
            effectiveDate = Instant.now(),
            description = "Audited payment",
            status = TransactionStatus.RECONCILED
        )

        assertThrows(DomainException.ReconciledTransactionLockedException::class.java) {
            tx.validateModificationAllowed()
        }
    }

    @Test
    fun `test transfer requires transferId`() {
        assertThrows(IllegalArgumentException::class.java) {
            Transaction(
                accountId = sampleAccountId,
                type = TransactionType.TRANSFER,
                amount = Money(10000L),
                competenceDate = Instant.now(),
                effectiveDate = Instant.now(),
                description = "Transfer",
                transferId = null
            )
        }
    }
}
