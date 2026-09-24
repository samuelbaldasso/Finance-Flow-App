package com.samuelbaldasso.financeflow.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.category.Category
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.designsystem.component.CreditCardVisual
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.component.TransactionStatusBadge
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    state: CardsUiState,
    onEvent: (CardsUiEvent) -> Unit,
    onNavigateToAccounts: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cartões de Crédito",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Faturas, limites e parcelamentos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        if (state.cards.isEmpty()) {
            FinancialEmptyState(
                title = "Nenhum cartão de crédito",
                description = "Cadastre uma conta do tipo Cartão de Crédito com limite e dias de fechamento para gerenciar suas faturas.",
                icon = Icons.Default.CreditCard,
                actionButtonText = "Ir para Contas",
                onActionClick = onNavigateToAccounts,
                modifier = Modifier.padding(padding)
            )
        } else {
            val selectedCard = state.selectedCard ?: state.cards.first()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Card Selector if multiple
                if (state.cards.size > 1) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        itemsIndexed(state.cards) { index, cardDetail ->
                            FilterChip(
                                selected = state.selectedCardIndex == index,
                                onClick = { onEvent(CardsUiEvent.SelectCard(index)) },
                                label = { Text(cardDetail.account.name) }
                            )
                        }
                    }
                }

                // Realistic Physical Card Visual
                CreditCardVisual(
                    account = selectedCard.account,
                    availableLimit = selectedCard.availableLimit
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons: Nova Compra Parcelada & Pagar Fatura
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onEvent(CardsUiEvent.OpenInstallmentDialog) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Parcelar Compra")
                    }

                    Button(
                        onClick = { onEvent(CardsUiEvent.OpenPayInvoiceDialog) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pagar Fatura")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Invoice Summary Box
                InvoiceSummaryBox(card = selectedCard)

                Spacer(modifier = Modifier.height(16.dp))

                // Installments List Header
                Text(
                    text = "Lançamentos & Parcelamentos",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedCard.transactions.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum lançamento nesta fatura",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = selectedCard.transactions,
                            key = { it.id },
                            contentType = { "card_transaction" }
                        ) { tx ->
                            CardTransactionItem(tx = tx)
                        }
                    }
                }
            }
        }
    }

    if (state.showInstallmentDialog) {
        CreateInstallmentDialog(
            categories = state.availableCategories,
            onDismiss = { onEvent(CardsUiEvent.DismissInstallmentDialog) },
            onConfirm = { desc, totalMinor, count, catId ->
                onEvent(CardsUiEvent.CreateInstallmentPurchase(desc, totalMinor, count, catId))
            }
        )
    }

    if (state.showPayInvoiceDialog) {
        PayInvoiceDialog(
            accounts = state.checkingAccounts,
            defaultAmountMinor = state.selectedCard?.openInvoiceTotal?.amountMinor ?: 0L,
            onDismiss = { onEvent(CardsUiEvent.DismissPayInvoiceDialog) },
            onConfirm = { srcAccId, minor ->
                onEvent(CardsUiEvent.PayInvoice(srcAccId, minor))
            }
        )
    }
}

@Composable
private fun InvoiceSummaryBox(card: CreditCardDetail) {
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "FATURA ATUAL (ABERTA)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                FinancialMoneyText(
                    money = card.openInvoiceTotal,
                    currency = card.account.currency,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    colorOverride = financialColors.expense
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "VENCIMENTO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dia ${card.account.dueDay ?: 20}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CardTransactionItem(tx: com.samuelbaldasso.financeflow.core.model.transaction.Transaction) {
    val financialColors = LocalFinancialColors.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(financialColors.expenseContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        tint = financialColors.expense,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = tx.description,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (tx.installmentNumber != null) {
                        Text(
                            text = "Parcela ${tx.installmentNumber} de ${tx.totalInstallments}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                FinancialMoneyText(
                    money = tx.amount,
                    currency = CurrencyCode.BRL,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    colorOverride = financialColors.expense
                )
                TransactionStatusBadge(status = tx.status)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateInstallmentDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (description: String, totalMinor: Long, installmentsCount: Int, categoryId: UUID?) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var installmentsText by remember { mutableStateOf("3") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }

    val selectedCategoryName = categories.find { it.id == selectedCategoryId }?.name ?: "Selecione a categoria"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Compra Parcelada", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição da compra") },
                    placeholder = { Text("Ex: Notebook Dell") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Valor Total (em centavos)") },
                    placeholder = { Text("Ex: 120000 para R$ 1.200,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = installmentsText,
                    onValueChange = { installmentsText = it.filter { char -> char.isDigit() } },
                    label = { Text("Número de Parcelas (1 a 48)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategoryName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    selectedCategoryId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = amountText.toLongOrNull() ?: 0L
                    val count = installmentsText.toIntOrNull() ?: 1
                    if (description.isNotBlank() && minor > 0L && count in 1..48) {
                        onConfirm(description, minor, count, selectedCategoryId)
                    }
                },
                enabled = description.isNotBlank() && (amountText.toLongOrNull() ?: 0L) > 0L &&
                        ((installmentsText.toIntOrNull() ?: 0) in 1..48)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayInvoiceDialog(
    accounts: List<Account>,
    defaultAmountMinor: Long,
    onDismiss: () -> Unit,
    onConfirm: (sourceAccountId: UUID, amountMinor: Long) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf(defaultAmountMinor.toString()) }
    var expanded by remember { mutableStateOf(false) }

    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Selecione a conta de débito"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pagar Fatura", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Transferência da conta corrente para pagamento da fatura do cartão",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Account Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Conta de Débito (Origem)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text("${acc.name} (${acc.currency.code})") },
                                onClick = {
                                    selectedAccountId = acc.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { char -> char.isDigit() } },
                    label = { Text("Valor a Pagar (em centavos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = amountText.toLongOrNull() ?: 0L
                    val accId = selectedAccountId
                    if (accId != null && minor > 0L) {
                        onConfirm(accId, minor)
                    }
                },
                enabled = selectedAccountId != null && (amountText.toLongOrNull() ?: 0L) > 0L
            ) {
                Text("Efetuar Pagamento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
