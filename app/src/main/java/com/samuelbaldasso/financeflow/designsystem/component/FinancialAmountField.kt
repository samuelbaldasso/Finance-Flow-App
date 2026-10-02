package com.samuelbaldasso.financeflow.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import java.util.Locale

@Composable
fun FinancialAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    currency: CurrencyCode = CurrencyCode.BRL
) {
    OutlinedTextField(
        value = value,
        onValueChange = { text ->
            val digits = text.filter { it in '0'..'9' }
            if (digits.isEmpty() || digits.toLongOrNull() != null) onValueChange(digits)
        },
        label = { Text(label) },
        placeholder = { Text("0,00") },
        supportingText = { Text("Digite os números; a vírgula é automática.") },
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = remember(currency) { MoneyInputTransformation(currency) },
        modifier = modifier
    )
}

internal class MoneyInputTransformation(private val currency: CurrencyCode) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val formatted = Money(text.text.toLongOrNull() ?: 0L).format(currency, Locale.forLanguageTag("pt-BR"))
        val positions = formatted.indices.filter { formatted[it].isDigit() }
        // Leading zeros may be removed by formatting; map them to the first digit.
        val padding = positions.size - text.length
        val offsets = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset == text.length) return formatted.length
                return positions[(offset + padding).coerceIn(0, positions.lastIndex)]
            }
            override fun transformedToOriginal(offset: Int): Int =
                (positions.count { it < offset } - padding).coerceIn(0, text.length)
        }
        return TransformedText(AnnotatedString(formatted), offsets)
    }
}
