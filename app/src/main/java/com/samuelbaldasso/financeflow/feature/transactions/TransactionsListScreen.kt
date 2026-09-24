package com.samuelbaldasso.financeflow.feature.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionType
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.component.TransactionStatusBadge
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward

import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsListScreen(
    transactions: List<Transaction>,
    onAddTransactionClick: () -> Unit,
    pagedTransactions: LazyPagingItems<Transaction>? = null,
    onOpenReportsClick: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<TransactionType?>(null) }

    val filteredTransactions = if (selectedFilter == null) {
        transactions
    } else {
        transactions.filter { it.type == selectedFilter }
    }

    val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.amountMinor }
    val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.amountMinor }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Extrato Financeiro",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Histórico de entradas, saídas e transferências",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                        }
                    }
                },
                actions = {
                    if (onOpenReportsClick != null) {
                        IconButton(onClick = onOpenReportsClick) {
                            Icon(Icons.Default.Analytics, contentDescription = "Relatórios de Fluxo de Caixa")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransactionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Transação")
            }
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Cash Flow Mini Summary Header
            CashFlowMiniSummary(incomeMinor = totalIncome, expenseMinor = totalExpense)

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("Todas (${transactions.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionType.EXPENSE,
                        onClick = { selectedFilter = TransactionType.EXPENSE },
                        label = { Text("Despesas") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionType.INCOME,
                        onClick = { selectedFilter = TransactionType.INCOME },
                        label = { Text("Receitas") }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == TransactionType.TRANSFER,
                        onClick = { selectedFilter = TransactionType.TRANSFER },
                        label = { Text("Transferências") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val isUsingPaging = pagedTransactions != null && selectedFilter == null
            val isListEmpty = if (isUsingPaging) {
                pagedTransactions.itemCount == 0
            } else {
                filteredTransactions.isEmpty()
            }

            if (isListEmpty) {
                FinancialEmptyState(
                    title = "Nenhuma transação encontrada",
                    description = "Registre receitas, despesas e transferências com cálculo automático de saldos.",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    actionButtonText = "Lançar Transação",
                    onActionClick = onAddTransactionClick
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isUsingPaging) {
                        items(
                            count = pagedTransactions.itemCount,
                            key = pagedTransactions.itemKey { it.id },
                            contentType = pagedTransactions.itemContentType { "transaction_item" }
                        ) { index ->
                            val tx = pagedTransactions[index]
                            if (tx != null) {
                                TransactionItemCard(transaction = tx)
                            }
                        }
                    } else {
                        items(
                            items = filteredTransactions,
                            key = { it.id },
                            contentType = { "transaction_item" }
                        ) { tx ->
                            TransactionItemCard(transaction = tx)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashFlowMiniSummary(incomeMinor: Long, expenseMinor: Long) {
    val financialColors = LocalFinancialColors.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(financialColors.incomeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = financialColors.income,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Entradas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                FinancialMoneyText(
                    money = Money(incomeMinor),
                    currency = CurrencyCode.BRL,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    colorOverride = financialColors.income
                )
            }

            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(36.dp)
                    .background(financialColors.cardBorder)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(financialColors.expenseContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = financialColors.expense,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Saídas",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                FinancialMoneyText(
                    money = Money(expenseMinor),
                    currency = CurrencyCode.BRL,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    colorOverride = financialColors.expense
                )
            }
        }
    }
}

@Composable
private fun TransactionItemCard(transaction: Transaction) {
    val financialColors = LocalFinancialColors.current
    val (icon, iconBgColor, iconTint, sign) = when (transaction.type) {
        TransactionType.INCOME -> Quadruple(
            Icons.Default.ArrowDownward,
            financialColors.incomeContainer,
            financialColors.income,
            "+"
        )
        TransactionType.EXPENSE -> Quadruple(
            Icons.Default.ArrowUpward,
            financialColors.expenseContainer,
            financialColors.expense,
            "-"
        )
        TransactionType.TRANSFER -> Quadruple(
            Icons.AutoMirrored.Filled.CompareArrows,
            financialColors.transferContainer,
            financialColors.transfer,
            ""
        )
        TransactionType.ADJUSTMENT -> Quadruple(
            Icons.AutoMirrored.Filled.CompareArrows,
            financialColors.warningContainer,
            financialColors.warning,
            ""
        )
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd 'de' MMM, HH:mm", Locale.getDefault())
    val formattedDate = dateFormatter.format(transaction.competenceDate.atZone(ZoneId.systemDefault()))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (sign.isNotEmpty()) {
                        Text(
                            text = sign,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = iconTint
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    FinancialMoneyText(
                        money = transaction.amount,
                        currency = CurrencyCode.BRL,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        colorOverride = iconTint
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                TransactionStatusBadge(status = transaction.status)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
