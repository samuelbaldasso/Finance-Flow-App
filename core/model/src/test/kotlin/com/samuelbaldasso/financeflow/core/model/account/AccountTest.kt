package com.samuelbaldasso.financeflow.core.model.account

import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountTest {

    @Test
    fun `test account creation with valid parameters`() {
        val account = Account(
            name = "Nubank PF",
            type = AccountType.CHECKING,
            currency = CurrencyCode.BRL,
            initialBalance = Money(50000L) // R$ 500,00
        )

        assertEquals("Nubank PF", account.name)
        assertEquals(AccountType.CHECKING, account.type)
        assertEquals(CurrencyCode.BRL, account.currency)
        assertEquals(50000L, account.initialBalance.amountMinor)
        assertFalse(account.isArchived)
    }

    @Test
    fun `test account name blank throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            Account(
                name = "   ",
                type = AccountType.CASH,
                currency = CurrencyCode.BRL
            )
        }
    }

    @Test
    fun `test credit card requires closing and due days`() {
        assertThrows(IllegalArgumentException::class.java) {
            Account(
                name = "Cartão Black",
                type = AccountType.CREDIT_CARD,
                currency = CurrencyCode.BRL
            )
        }

        val card = Account(
            name = "Cartão Black",
            type = AccountType.CREDIT_CARD,
            currency = CurrencyCode.BRL,
            closingDay = 15,
            dueDay = 22,
            creditLimit = Money(500000L) // R$ 5.000,00
        )
        assertEquals(15, card.closingDay)
        assertEquals(22, card.dueDay)
    }
}
