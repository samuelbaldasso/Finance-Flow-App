package com.samuelbaldasso.financeflow.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.designsystem.theme.MidnightCreditCardGradient
import com.samuelbaldasso.financeflow.designsystem.theme.PurpleCreditCardGradient
import com.samuelbaldasso.financeflow.designsystem.theme.SapphireCreditCardGradient

@Composable
fun CreditCardVisual(
    account: Account,
    availableLimit: Money,
    modifier: Modifier = Modifier,
    gradient: Brush = MidnightCreditCardGradient
) {
    val totalLimit = account.creditLimit ?: Money.ZERO
    val usedLimitMinor = (totalLimit.amountMinor - availableLimit.amountMinor).coerceAtLeast(0L)
    val usedPercentage = if (totalLimit.amountMinor > 0L) {
        usedLimitMinor.toFloat() / totalLimit.amountMinor.toFloat()
    } else 0f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header: Bank Name & NFC / Card Type
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = account.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Nfc,
                        contentDescription = "Contactless",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Cartão de Crédito",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Chip graphic
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE2B755))
                        .border(1.dp, Color(0xFFC59B27), RoundedCornerShape(6.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card Number Mask
            Text(
                text = "••••  ••••  ••••  ${account.id.toString().takeLast(4).uppercase()}",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                ),
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer: Dates and Limits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "FATURA",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Fecha dia ${account.closingDay ?: 10} • Vence dia ${account.dueDay ?: 20}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LIMITE DISPONÍVEL",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    FinancialMoneyText(
                        money = availableLimit,
                        currency = account.currency,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        colorOverride = Color(0xFF34D399)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Limit meter
            FinanceProgressBar(
                progress = usedPercentage,
                height = 6.dp,
                trackColor = Color.White.copy(alpha = 0.2f),
                forceColor = if (usedPercentage > 0.85f) Color(0xFFFB7185) else Color(0xFF34D399)
            )
        }
    }
}
