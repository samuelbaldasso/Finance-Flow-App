package com.samuelbaldasso.financeflow.core.model.money

import java.math.BigDecimal
import java.time.Instant

data class ExchangeRate(
    val fromCurrency: CurrencyCode,
    val toCurrency: CurrencyCode,
    val rate: BigDecimal,
    val timestamp: Instant,
    val source: String
) {
    init {
        require(rate > BigDecimal.ZERO) { "Exchange rate must be positive: $rate" }
    }
}
