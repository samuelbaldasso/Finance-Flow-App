package com.samuelbaldasso.financeflow.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.component.TransactionStatusBadge
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    transactions: List<Transaction>,
    onAddTransactionClick: () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Extrato de Transações") },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransactionClick) {
                Icon(Icons.Default.Add, contentDescription = "Nova Transação")
            }
        },
        modifier = modifier
    ) { padding ->
        if (transactions.isEmpty()) {
            FinancialEmptyState(
                title = "Nenhuma transação lançada",
                description = "Registre receitas, despesas e transferências com cálculo automático de saldos.",
                icon = Icons.Default.ReceiptLong,
                actionButtonText = "Lançar Transação",
                onActionClick = onAddTransactionClick,
                modifier = Modifier.padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = transactions,
                    key = { it.id },
                    contentType = { "transaction_item" }
                ) { tx ->
                    TransactionItemCard(transaction = tx)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val colors = LocalFinancialColors.current

    val (icon, iconBg, iconTint) = when (transaction.type) {
        TransactionType.INCOME -> Triple(Icons.Default.ArrowDownward, colors.incomeContainer, colors.income)
        TransactionType.EXPENSE -> Triple(Icons.Default.ArrowUpward, colors.expenseContainer, colors.expense)
        TransactionType.TRANSFER -> Triple(Icons.AutoMirrored.Filled.CompareArrows, colors.transferContainer, colors.transfer)
        TransactionType.ADJUSTMENT -> Triple(Icons.Default.ReceiptLong, colors.warningContainer, colors.warning)
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransactionStatusBadge(status = transaction.status)
                    if (transaction.isInstallment && transaction.installmentNumber != null && transaction.totalInstallments != null) {
                        Text(
                            text = "${transaction.installmentNumber}/${transaction.totalInstallments}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            FinancialMoneyText(
                money = transaction.amount,
                currency = CurrencyCode.BRL,
                type = transaction.type,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
