package com.samuelbaldasso.financeflow.core.model.money

import com.samuelbaldasso.financeflow.core.model.error.DomainException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

@JvmInline
value class Money(val amountMinor: Long) : Comparable<Money> {

    fun toBigDecimal(minorUnits: Int = 2): BigDecimal =
        BigDecimal(amountMinor).divide(BigDecimal.TEN.pow(minorUnits))

    operator fun plus(other: Money): Money = Money(Math.addExact(this.amountMinor, other.amountMinor))
    operator fun minus(other: Money): Money = Money(Math.subtractExact(this.amountMinor, other.amountMinor))
    operator fun unaryMinus(): Money = Money(Math.negateExact(this.amountMinor))

    fun isZero(): Boolean = amountMinor == 0L
    fun isPositive(): Boolean = amountMinor > 0L
    fun isNegative(): Boolean = amountMinor < 0L

    override fun compareTo(other: Money): Int = this.amountMinor.compareTo(other.amountMinor)

    companion object {
        val ZERO = Money(0L)

        fun fromMinor(amountMinor: Long): Money = Money(amountMinor)

        fun fromMajor(amountMajor: BigDecimal, minorUnits: Int = 2): Money {
            val factor = BigDecimal.TEN.pow(minorUnits)
            val scaled = amountMajor.multiply(factor).setScale(0, RoundingMode.HALF_EVEN)
            return Money(scaled.longValueExact())
        }
    }
}

data class CurrencedMoney(
    val money: Money,
    val currency: CurrencyCode
) : Comparable<CurrencedMoney> {
    val amountMinor: Long get() = money.amountMinor

    operator fun plus(other: CurrencedMoney): CurrencedMoney {
        ensureSameCurrency(other)
        return copy(money = money + other.money)
    }

    operator fun minus(other: CurrencedMoney): CurrencedMoney {
        ensureSameCurrency(other)
        return copy(money = money - other.money)
    }

    operator fun unaryMinus(): CurrencedMoney = copy(money = -money)

    operator fun times(factor: BigDecimal): CurrencedMoney {
        val currentMajor = money.toBigDecimal(currency.minorUnits)
        val multiplied = currentMajor.multiply(factor)
        return CurrencedMoney(Money.fromMajor(multiplied, currency.minorUnits), currency)
    }

    fun convert(rate: ExchangeRate): CurrencedMoney {
        if (rate.fromCurrency != this.currency) {
            throw DomainException.CurrencyMismatchException(
                "Exchange rate from ${rate.fromCurrency.code} does not match current currency ${this.currency.code}"
            )
        }
        val currentMajor = money.toBigDecimal(currency.minorUnits)
        val convertedMajor = currentMajor.multiply(rate.rate)
        return CurrencedMoney(
            money = Money.fromMajor(convertedMajor, rate.toCurrency.minorUnits),
            currency = rate.toCurrency
        )
    }

    private fun ensureSameCurrency(other: CurrencedMoney) {
        if (this.currency != other.currency) {
            throw DomainException.CurrencyMismatchException(
                "Cannot operate between currencies ${this.currency.code} and ${other.currency.code} without conversion"
            )
        }
    }

    override fun compareTo(other: CurrencedMoney): Int {
        ensureSameCurrency(other)
        return this.amountMinor.compareTo(other.amountMinor)
    }

    fun format(locale: Locale = Locale.getDefault()): String {
        val format = NumberFormat.getCurrencyInstance(locale).apply {
            currency = this@CurrencedMoney.currency.javaCurrency
            maximumFractionDigits = this@CurrencedMoney.currency.minorUnits
            minimumFractionDigits = this@CurrencedMoney.currency.minorUnits
            roundingMode = RoundingMode.HALF_EVEN
        }
        return format.format(money.toBigDecimal(currency.minorUnits))
    }

    companion object {
        fun zero(currency: CurrencyCode): CurrencedMoney = CurrencedMoney(Money.ZERO, currency)
    }
}
