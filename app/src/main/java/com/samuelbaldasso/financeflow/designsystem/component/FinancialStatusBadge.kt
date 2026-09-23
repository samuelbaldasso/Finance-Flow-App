package com.samuelbaldasso.financeflow.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.samuelbaldasso.financeflow.core.model.budget.BudgetStatus
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors

@Composable
fun BudgetStatusBadge(
    status: BudgetStatus,
    modifier: Modifier = Modifier
) {
    val colors = LocalFinancialColors.current
    val (bgColor, textColor, icon, label) = when (status) {
        BudgetStatus.OK -> Quadruple(
            colors.incomeContainer,
            colors.income,
            Icons.Default.CheckCircle,
            "Dentro do limite"
        )
        BudgetStatus.WARNING -> Quadruple(
            colors.warningContainer,
            colors.warning,
            Icons.Default.Warning,
            "Atenção (>80%)"
        )
        BudgetStatus.EXCEEDED -> Quadruple(
            colors.expenseContainer,
            colors.expense,
            Icons.Default.Error,
            "Estourado (≥100%)"
        )
    }

    StatusBadge(
        label = label,
        icon = icon,
        backgroundColor = bgColor,
        contentColor = textColor,
        modifier = modifier
    )
}

@Composable
fun TransactionStatusBadge(
    status: TransactionStatus,
    modifier: Modifier = Modifier
) {
    val colors = LocalFinancialColors.current
    val (bgColor, textColor, icon, label) = when (status) {
        TransactionStatus.PENDING -> Quadruple(
            colors.warningContainer,
            colors.warning,
            Icons.Default.HourglassTop,
            "Pendente"
        )
        TransactionStatus.CLEARED -> Quadruple(
            colors.transferContainer,
            colors.transfer,
            Icons.Default.CheckCircle,
            "Efetivada"
        )
        TransactionStatus.RECONCILED -> Quadruple(
            colors.incomeContainer,
            colors.income,
            Icons.Default.Lock,
            "Reconciliada"
        )
    }

    StatusBadge(
        label = label,
        icon = icon,
        backgroundColor = bgColor,
        contentColor = textColor,
        modifier = modifier
    )
}

@Composable
private fun StatusBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
