package com.samuelbaldasso.financeflow.feature.accounts

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.domain.repository.AccountWithBalance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onEvent: (AccountsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contas & Saldos") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "Arquivadas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = state.showArchived,
                            onCheckedChange = { onEvent(AccountsUiEvent.ToggleShowArchived) }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(AccountsUiEvent.OpenCreateDialog) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Conta")
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
            // Summary Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Patrimônio Líquido",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FinancialMoneyText(
                        money = state.totalBrlBalance,
                        currency = CurrencyCode.BRL,
                        style = MaterialTheme.typography.headlineMedium,
                        colorOverride = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (state.totalUsdBalance.amountMinor > 0L) {
                        Spacer(modifier = Modifier.height(2.dp))
                        FinancialMoneyText(
                            money = state.totalUsdBalance,
                            currency = CurrencyCode.USD,
                            style = MaterialTheme.typography.titleMedium,
                            colorOverride = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (state.accounts.isEmpty()) {
                FinancialEmptyState(
                    title = "Nenhuma conta cadastrada",
                    description = "Crie sua primeira conta bancária, carteira ou cartão para começar a controlar seu fluxo financeiro.",
                    icon = Icons.Default.AccountBalance,
                    actionButtonText = "Adicionar Conta",
                    onActionClick = { onEvent(AccountsUiEvent.OpenCreateDialog) }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.accounts,
                        key = { it.account.id },
                        contentType = { "account_item" }
                    ) { item ->
                        AccountItemCard(
                            item = item,
                            onArchiveClick = { onEvent(AccountsUiEvent.ArchiveAccount(item.account.id)) }
                        )
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateAccountDialog(
            onDismiss = { onEvent(AccountsUiEvent.DismissCreateDialog) },
            onConfirm = { name, type, currency, initialMinor, limitMinor, closing, due ->
                onEvent(
                    AccountsUiEvent.CreateAccount(
                        name = name,
                        type = type,
                        currency = currency,
                        initialBalanceMinor = initialMinor,
                        creditLimitMinor = limitMinor,
                        closingDay = closing,
                        dueDay = due
                    )
                )
            }
        )
    }
}

@Composable
private fun AccountItemCard(
    item: AccountWithBalance,
    onArchiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val account = item.account
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (account.isArchived) 0.dp else 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val (icon, typeLabel) = when (account.type) {
                AccountType.CHECKING -> Icons.Default.AccountBalance to "Conta Corrente"
                AccountType.SAVINGS -> Icons.Default.Savings to "Poupança"
                AccountType.CASH -> Icons.Default.Wallet to "Dinheiro"
                AccountType.INVESTMENT -> Icons.Default.TrendingUp to "Investimento"
                AccountType.CREDIT_CARD -> Icons.Default.CreditCard to "Cartão de Crédito"
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (account.isArchived) "$typeLabel • Arquivada" else typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                FinancialMoneyText(
                    money = item.derivedBalance,
                    currency = account.currency,
                    style = MaterialTheme.typography.titleMedium
                )
                if (!account.isArchived) {
                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Arquivar conta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAccountDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        currency: CurrencyCode,
        initialMinor: Long,
        limitMinor: Long?,
        closingDay: Int?,
        dueDay: Int?
    ) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.CHECKING) }
    var selectedCurrency by remember { mutableStateOf(CurrencyCode.BRL) }
    var initialBalanceStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova Conta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da conta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Type selector
                Text("Tipo:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(AccountType.CHECKING, AccountType.SAVINGS, AccountType.CASH, AccountType.CREDIT_CARD).forEach { type ->
                        val isSelected = selectedType == type
                        Button(
                            onClick = { selectedType = type },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (type) {
                                    AccountType.CHECKING -> "CC"
                                    AccountType.SAVINGS -> "Poup"
                                    AccountType.CASH -> "Din"
                                    AccountType.CREDIT_CARD -> "Cart"
                                    AccountType.INVESTMENT -> "Inv"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Currency selector
                Text("Moeda:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CurrencyCode.entries.forEach { curr ->
                        Button(
                            onClick = { selectedCurrency = curr },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(curr.code)
                        }
                    }
                }

                OutlinedTextField(
                    value = initialBalanceStr,
                    onValueChange = { initialBalanceStr = it },
                    label = { Text("Saldo inicial em centavos (ex: 1000 = R$ 10,00)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val initialMinor = initialBalanceStr.toLongOrNull() ?: 0L
                        val closing = if (selectedType == AccountType.CREDIT_CARD) 15 else null
                        val due = if (selectedType == AccountType.CREDIT_CARD) 22 else null
                        onConfirm(name, selectedType, selectedCurrency, initialMinor, null, closing, due)
                    }
                }
            ) {
                Text("Criar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
