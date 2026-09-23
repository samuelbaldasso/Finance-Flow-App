package com.samuelbaldasso.financeflow.core.model.money

import java.util.Currency

enum class CurrencyCode(val code: String, val minorUnits: Int, val symbol: String) {
    BRL("BRL", 2, "R$"),
    USD("USD", 2, "$");

    val javaCurrency: Currency get() = Currency.getInstance(code)

    companion object {
        fun fromCode(code: String): CurrencyCode =
            entries.find { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unsupported currency code: $code")
    }
}
