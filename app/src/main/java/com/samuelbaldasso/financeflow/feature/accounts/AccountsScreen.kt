package com.samuelbaldasso.financeflow.feature.accounts

import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialAmountField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuelbaldasso.financeflow.core.model.account.AccountType
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.theme.EmeraldHeroGradient
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import com.samuelbaldasso.financeflow.domain.repository.AccountWithBalance

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(
    state: AccountsUiState,
    onEvent: (AccountsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var balancesVisible by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Column {
                        Text(
                            text = "FinanceFlow",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Seu dinheiro, com clareza",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
            ExtendedFloatingActionButton(
                text = { Text("Nova conta") },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = { onEvent(AccountsUiEvent.OpenCreateDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Hero Net Worth Card
            HeroNetWorthCard(
                state = state,
                balancesVisible = balancesVisible,
                onToggleVisibility = { balancesVisible = !balancesVisible }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Suas contas · ${state.accounts.size}",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (state.accounts.isEmpty()) {
                FinancialEmptyState(
                    title = "Nenhuma conta cadastrada",
                    description = "Crie sua primeira conta corrente, poupança, dinheiro ou cartão para começar a controlar seus saldos.",
                    icon = Icons.Default.AccountBalance,
                    actionButtonText = "Adicionar Conta",
                    onActionClick = { onEvent(AccountsUiEvent.OpenCreateDialog) }
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.accounts,
                        key = { it.account.id },
                        contentType = { "account_item" }
                    ) { item ->
                        AccountItemCard(
                            item = item,
                            balancesVisible = balancesVisible,
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
private fun HeroNetWorthCard(
    state: AccountsUiState,
    balancesVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(EmeraldHeroGradient)
                .padding(24.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PATRIMÔNIO LÍQUIDO",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    IconButton(
                        onClick = onToggleVisibility,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (balancesVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (balancesVisible) "Ocultar saldos" else "Mostrar saldos",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                if (balancesVisible) {
                    FinancialMoneyText(
                        money = state.totalBrlBalance,
                        currency = CurrencyCode.BRL,
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        colorOverride = Color.White
                    )

                    if (state.totalUsdBalance.amountMinor > 0L) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "USD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            FinancialMoneyText(
                                money = state.totalUsdBalance,
                                currency = CurrencyCode.USD,
                                style = MaterialTheme.typography.titleMedium,
                                colorOverride = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "••••••••",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        ),
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "${state.accounts.size} conta(s) ativa(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AccountItemCard(
    item: AccountWithBalance,
    balancesVisible: Boolean,
    onArchiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val financialColors = LocalFinancialColors.current
    val account = item.account

    val (icon, typeLabel, iconBgColor, iconTint) = when (account.type) {
        AccountType.CHECKING -> Quadruple(Icons.Default.AccountBalance, "Conta Corrente", financialColors.transferContainer, financialColors.transfer)
        AccountType.SAVINGS -> Quadruple(Icons.Default.Savings, "Poupança", financialColors.incomeContainer, financialColors.income)
        AccountType.CASH -> Quadruple(Icons.Default.Wallet, "Carteira / Dinheiro", financialColors.warningContainer, financialColors.warning)
        AccountType.INVESTMENT -> Quadruple(Icons.Default.TrendingUp, "Investimentos", Color(0xFFF3E8FF), Color(0xFF7E22CE))
        AccountType.CREDIT_CARD -> Quadruple(Icons.Default.CreditCard, "Cartão de Crédito", financialColors.expenseContainer, financialColors.expense)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (account.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (account.isArchived) "$typeLabel • Arquivada" else typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                if (balancesVisible) {
                    FinancialMoneyText(
                        money = item.derivedBalance,
                        currency = account.currency,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Text(
                        text = "••••••",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (!account.isArchived) {
                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = "Arquivar conta",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    var creditLimitStr by remember { mutableStateOf("") }
    var closingDayStr by remember { mutableStateOf("10") }
    var dueDayStr by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nova Conta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da conta") },
                    placeholder = { Text("Ex: Nubank, Itaú, Carteira") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Tipo de Conta:", style = MaterialTheme.typography.labelMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.CHECKING -> "Corrente"
                            AccountType.SAVINGS -> "Poupança"
                            AccountType.CASH -> "Dinheiro"
                            AccountType.INVESTMENT -> "Invest."
                            AccountType.CREDIT_CARD -> "Cartão"
                        }
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Moeda:", style = MaterialTheme.typography.labelMedium)
                    FilterChip(
                        selected = selectedCurrency == CurrencyCode.BRL,
                        onClick = { selectedCurrency = CurrencyCode.BRL },
                        label = { Text("BRL (R$)") }
                    )
                    FilterChip(
                        selected = selectedCurrency == CurrencyCode.USD,
                        onClick = { selectedCurrency = CurrencyCode.USD },
                        label = { Text("USD ($)") }
                    )
                }

                if (selectedType != AccountType.CREDIT_CARD) {
                    FinancialAmountField(
                        value = initialBalanceStr,
                        onValueChange = { initialBalanceStr = it },
                        label = "Saldo Inicial",
                        modifier = Modifier.fillMaxWidth(),
                        currency = selectedCurrency
                    )
                } else {
                    FinancialAmountField(
                        value = creditLimitStr,
                        onValueChange = { creditLimitStr = it },
                        label = "Limite do Cartão",
                        modifier = Modifier.fillMaxWidth(),
                        currency = selectedCurrency
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = closingDayStr,
                            onValueChange = { closingDayStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Dia Fechamento") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = dueDayStr,
                            onValueChange = { dueDayStr = it.filter { char -> char.isDigit() } },
                            label = { Text("Dia Vencimento") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val initialMinor = initialBalanceStr.toLongOrNull() ?: 0L
                    val limitMinor = creditLimitStr.toLongOrNull()
                    val closing = closingDayStr.toIntOrNull()
                    val due = dueDayStr.toIntOrNull()
                    onConfirm(name, selectedType, selectedCurrency, initialMinor, limitMinor, closing, due)
                },
                enabled = name.isNotBlank() && (selectedType != AccountType.CREDIT_CARD ||
                    ((creditLimitStr.toLongOrNull() ?: 0L) > 0L &&
                    closingDayStr.toIntOrNull() in 1..31 && dueDayStr.toIntOrNull() in 1..31))
            ) {
                Text("Criar Conta")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
