package com.samuelbaldasso.financeflow.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.samuelbaldasso.financeflow.core.model.money.CurrencedMoney
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import java.util.Locale

@Composable
fun FinancialMoneyText(
    money: Money,
    currency: CurrencyCode,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    style: TextStyle = MaterialTheme.typography.titleMedium,
    colorOverride: Color? = null,
    locale: Locale = Locale.getDefault()
) {
    val financialColors = LocalFinancialColors.current
    val currenced = CurrencedMoney(money, currency)
    val formatted = currenced.format(locale)

    val (prefix, textColor, spokenPrefix) = when (type) {
        TransactionType.INCOME -> Triple("+", financialColors.income, "mais ")
        TransactionType.EXPENSE -> Triple("-", financialColors.expense, "menos ")
        TransactionType.TRANSFER -> Triple("", financialColors.transfer, "transferência de ")
        TransactionType.ADJUSTMENT -> {
            if (money.amountMinor >= 0L) Triple("+", financialColors.income, "mais ")
            else Triple("", financialColors.expense, "menos ")
        }
        null -> Triple("", MaterialTheme.colorScheme.onSurface, "")
    }

    val displayText = "$prefix$formatted"
    val accessibleText = "$spokenPrefix$formatted"

    Text(
        text = displayText,
        style = style,
        color = colorOverride ?: textColor,
        modifier = modifier.semantics {
            contentDescription = accessibleText
        }
    )
}
