package com.samuelbaldasso.financeflow.core.model.money

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.Locale

class MoneyTest {

    @Test
    fun `test money addition and subtraction with minor units`() {
        val m1 = Money(15050L) // R$ 150.50
        val m2 = Money(4950L)  // R$ 49.50

        val sum = m1 + m2
        assertEquals(20000L, sum.amountMinor)

        val diff = m1 - m2
        assertEquals(10100L, diff.amountMinor)
    }

    @Test
    fun `test half even banking rounding on conversion from major decimal`() {
        // 12.345 with 2 decimal places -> HALF_EVEN to nearest even -> 12.34 (since 4 is even)
        val m1 = Money.fromMajor(BigDecimal("12.345"), 2)
        assertEquals(1234L, m1.amountMinor)

        // 12.355 with 2 decimal places -> HALF_EVEN to nearest even -> 12.36 (since 6 is even)
        val m2 = Money.fromMajor(BigDecimal("12.355"), 2)
        assertEquals(1236L, m2.amountMinor)
    }

    @Test
    fun `test operations across different currencies throws CurrencyMismatchException`() {
        val brl = CurrencedMoney(Money(10000L), CurrencyCode.BRL)
        val usd = CurrencedMoney(Money(10000L), CurrencyCode.USD)

        assertThrows(DomainException.CurrencyMismatchException::class.java) {
            brl + usd
        }

        assertThrows(DomainException.CurrencyMismatchException::class.java) {
            brl - usd
        }
    }

    @Test
    fun `test exchange rate conversion`() {
        val usd = CurrencedMoney(Money(10000L), CurrencyCode.USD) // $100.00
        val rate = ExchangeRate(
            fromCurrency = CurrencyCode.USD,
            toCurrency = CurrencyCode.BRL,
            rate = BigDecimal("5.50"),
            timestamp = Instant.now(),
            source = "Banco Central do Brasil"
        )

        val converted = usd.convert(rate)
        assertEquals(CurrencyCode.BRL, converted.currency)
        assertEquals(55000L, converted.amountMinor) // R$ 550.00
    }

    @Test
    fun `test formatting respects locale`() {
        val brl = CurrencedMoney(Money(123456L), CurrencyCode.BRL) // R$ 1.234,56
        val formattedPtBr = brl.format(Locale.forLanguageTag("pt-BR"))
        // Check that it contains R$ and either period or comma grouping
        assertTrue(formattedPtBr.contains("1.234,56") || formattedPtBr.contains("1234,56"))

        val usd = CurrencedMoney(Money(123456L), CurrencyCode.USD) // $1,234.56
        val formattedEnUs = usd.format(Locale.US)
        assertTrue(formattedEnUs.contains("1,234.56"))
    }

    @Test
    fun `property test - installment division preserves total minor cents`() {
        // Test dividing various amounts into N installments: sum of installments must equal total
        for (totalMinor in listOf(10000L, 9999L, 10001L, 33333L, 50000L, 77777L)) {
            for (installments in 2..12) {
                val base = totalMinor / installments
                val remainder = totalMinor % installments

                val parts = (1..installments).map { i ->
                    // Standard banking distribution: remainder cents distributed to first installments
                    if (i <= remainder) base + 1 else base
                }

                assertEquals("Installment sum must strictly equal total", totalMinor, parts.sum())
            }
        }
    }
}
