package com.samuelbaldasso.financeflow.domain

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.recurrence.RecurrenceFrequency
import com.samuelbaldasso.financeflow.core.model.recurrence.RecurrenceRule
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import com.samuelbaldasso.financeflow.domain.usecase.card.CalculateAvailableLimitUseCase
import com.samuelbaldasso.financeflow.domain.usecase.card.CreditCardBillingCycle
import com.samuelbaldasso.financeflow.domain.usecase.card.CreateInstallmentPurchaseUseCase
import com.samuelbaldasso.financeflow.domain.usecase.recurrence.RecurrenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.UUID

class CreditCardAndRecurrenceTest {

    @Test
    fun testBillingCycleBeforeAndAfterClosingDay() {
        val closingDay = 10
        val dueDay = 17

        // Purchase on day 5 -> closing date is day 10 of current month
        val purchaseBefore = LocalDate.of(2026, 5, 5)
        val cycle1 = CreditCardBillingCycle.calculateCycle(purchaseBefore, closingDay, dueDay)
        assertEquals(LocalDate.of(2026, 5, 10), cycle1.closingDate)
        assertEquals(LocalDate.of(2026, 5, 17), cycle1.dueDate)

        // Purchase on day 15 -> closing date is day 10 of NEXT month
        val purchaseAfter = LocalDate.of(2026, 5, 15)
        val cycle2 = CreditCardBillingCycle.calculateCycle(purchaseAfter, closingDay, dueDay)
        assertEquals(LocalDate.of(2026, 6, 10), cycle2.closingDate)
        assertEquals(LocalDate.of(2026, 6, 17), cycle2.dueDate)
    }

    @Test
    fun testShortMonthClampingOnBillingCycle() {
        // Closing day 31 in February 2026 -> clamped to 28
        val purchase = LocalDate.of(2026, 2, 10)
        val cycle = CreditCardBillingCycle.calculateCycle(purchase, closingDay = 31, dueDay = 10)
        assertEquals(LocalDate.of(2026, 2, 28), cycle.closingDate)
        assertEquals(LocalDate.of(2026, 3, 10), cycle.dueDate)
    }

    @Test
    fun testRecurrenceMonthClampingDay31() {
        val rule = RecurrenceRule(
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            amount = Money(50000L),
            description = "Assinatura mensal",
            frequency = RecurrenceFrequency.MONTHLY,
            startDate = LocalDate.of(2026, 1, 31)
        )

        val occurrences = RecurrenceEngine.generateOccurrences(rule, LocalDate.of(2026, 5, 1))
        assertEquals(4, occurrences.size)
        assertEquals(LocalDate.of(2026, 1, 31), occurrences[0])
        assertEquals(LocalDate.of(2026, 2, 28), occurrences[1]) // Clamped to 28
        assertEquals(LocalDate.of(2026, 3, 31), occurrences[2]) // Returned to 31
        assertEquals(LocalDate.of(2026, 4, 30), occurrences[3]) // Clamped to 30
    }

    @Test
    fun testRecurrenceMaxOccurrencesAndEndDate() {
        val rule = RecurrenceRule(
            accountId = UUID.randomUUID(),
            type = TransactionType.EXPENSE,
            amount = Money(1000L),
            description = "Semanal",
            frequency = RecurrenceFrequency.WEEKLY,
            startDate = LocalDate.of(2026, 1, 1),
            maxOccurrences = 3
        )

        val occurrences = RecurrenceEngine.generateOccurrences(rule, LocalDate.of(2026, 12, 31))
        assertEquals(3, occurrences.size)
    }

    @Test
    fun testInstallmentGenerationNoCentLossAndLimitDeduction() = runBlocking {
        val savedTransactions = mutableListOf<Transaction>()

        val fakeRepo = object : TransactionRepository {
            override fun getAllTransactionsFlow(): Flow<List<Transaction>> = flowOf(savedTransactions)
            override fun getTransactionsByAccountFlow(accountId: UUID): Flow<List<Transaction>> = flowOf(savedTransactions)
            override fun getGoalContributionsFlow(goalId: UUID): Flow<List<Transaction>> = flowOf(emptyList())
            override suspend fun getTransactionById(id: UUID): Transaction? = savedTransactions.find { it.id == id }
            override suspend fun getTransactionsByTransferId(transferId: UUID): List<Transaction> = emptyList()
            override suspend fun createTransaction(transaction: Transaction): Transaction {
                savedTransactions.add(transaction)
                return transaction
            }
            override suspend fun createTransfer(debitTx: Transaction, creditTx: Transaction) {}
            override suspend fun updateTransaction(transaction: Transaction) {}
            override suspend fun deleteTransaction(id: UUID) { savedTransactions.removeAll { it.id == id } }
            override suspend fun reconcileTransaction(id: UUID) {}
            override suspend fun unreconcileTransaction(id: UUID) {}
        }

        val card = Account(
            id = UUID.randomUUID(),
            name = "Mastercard Black",
            type = AccountType.CREDIT_CARD,
            currency = CurrencyCode.BRL,
            creditLimit = Money(1000000L), // R$ 10.000,00
            closingDay = 15,
            dueDay = 25
        )

        val useCase = CreateInstallmentPurchaseUseCase(fakeRepo)
        val installments = useCase(
            cardAccount = card,
            totalAmount = Money(100000L), // R$ 1.000,00 in 3x
            installmentsCount = 3,
            firstPurchaseDate = LocalDate.of(2026, 6, 10),
            description = "Smartphone"
        )

        assertEquals(3, installments.size)
        // Installments: 333.34 + 333.33 + 333.33 = 1000.00 (33334 + 33333 + 33333 = 100000)
        assertEquals(33334L, installments[0].amount.amountMinor)
        assertEquals(33333L, installments[1].amount.amountMinor)
        assertEquals(33333L, installments[2].amount.amountMinor)
        assertEquals(100000L, installments.sumOf { it.amount.amountMinor })

        // Check available limit calculation: 10.000,00 - 1.000,00 = 9.000,00 (900000 minor)
        val limitUseCase = CalculateAvailableLimitUseCase(fakeRepo)
        val available = limitUseCase(card)
        assertEquals(900000L, available.amountMinor)
    }
}
