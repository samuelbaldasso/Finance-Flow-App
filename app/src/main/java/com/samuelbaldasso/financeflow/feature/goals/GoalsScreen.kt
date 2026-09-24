package com.samuelbaldasso.financeflow.feature.goals

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Savings
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.designsystem.component.FinanceProgressBar
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.theme.GoldGoalGradient
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import com.samuelbaldasso.financeflow.domain.repository.GoalWithProgress
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    state: GoalsUiState,
    onEvent: (GoalsUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Metas & Objetivos",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Acompanhe seus aportes e conquistas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(GoalsUiEvent.OpenCreateDialog) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Meta")
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
            // Goals Summary Hero Card
            GoalsOverviewCard(state = state)

            Spacer(modifier = Modifier.height(14.dp))

            if (state.goals.isEmpty()) {
                FinancialEmptyState(
                    title = "Nenhuma meta criada",
                    description = "Crie metas de curto e longo prazo (Reserva, Viagem, Carro) e aporte diretamente de suas contas.",
                    icon = Icons.Default.Flag,
                    actionButtonText = "Criar Primeira Meta",
                    onActionClick = { onEvent(GoalsUiEvent.OpenCreateDialog) }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = state.goals,
                        key = { it.goal.id },
                        contentType = { "goal_item" }
                    ) { item ->
                        GoalItemCard(
                            item = item,
                            onContributeClick = { onEvent(GoalsUiEvent.OpenContributeDialog(item)) },
                            onDeleteClick = { onEvent(GoalsUiEvent.DeleteGoal(item.goal.id)) }
                        )
                    }
                }
            }
        }
    }

    if (state.showCreateDialog) {
        CreateGoalDialog(
            accounts = state.accounts,
            onDismiss = { onEvent(GoalsUiEvent.DismissCreateDialog) },
            onConfirm = { name, targetMinor, targetDate, linkedAccountId ->
                onEvent(GoalsUiEvent.CreateGoal(name, targetMinor, targetDate, linkedAccountId))
            }
        )
    }

    state.contributeGoal?.let { goalWithProgress ->
        ContributeGoalDialog(
            goalWithProgress = goalWithProgress,
            accounts = state.accounts,
            onDismiss = { onEvent(GoalsUiEvent.DismissContributeDialog) },
            onConfirm = { accountId, amountMinor ->
                onEvent(
                    GoalsUiEvent.SubmitContribution(
                        goalId = goalWithProgress.goal.id,
                        accountId = accountId,
                        amountMinor = amountMinor
                    )
                )
            }
        )
    }
}

@Composable
private fun GoalsOverviewCard(state: GoalsUiState) {
    val financialColors = LocalFinancialColors.current

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL GUARDADO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (state.completedGoalsCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(financialColors.incomeContainer)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = financialColors.income,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${state.completedGoalsCount} concluída(s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = financialColors.income
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.activeGoalsCount} ativa(s)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            FinancialMoneyText(
                money = state.totalSaved,
                currency = CurrencyCode.BRL,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                colorOverride = financialColors.income
            )

            Text(
                text = "de ${state.totalTarget.format(CurrencyCode.BRL, Locale("pt", "BR"))} planejados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            FinanceProgressBar(
                progress = state.overallProgress,
                height = 8.dp,
                forceColor = Color(0xFFD97706)
            )
        }
    }
}

@Composable
private fun GoalItemCard(
    item: GoalWithProgress,
    onContributeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val financialColors = LocalFinancialColors.current
    val goal = item.goal
    val saved = item.currentSavedAmount
    val target = goal.targetAmount
    val pct = (item.progressPercentage).toInt()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isCompleted) financialColors.incomeContainer
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (item.isCompleted) Icons.Default.CheckCircle else Icons.Default.Flag,
                            contentDescription = null,
                            tint = if (item.isCompleted) financialColors.income else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (item.isCompleted) "Meta Concluída!" else "$pct% alcançado",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium,
                                color = if (item.isCompleted) financialColors.income else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir meta",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            FinanceProgressBar(
                progress = item.progressPercentage / 100f,
                height = 7.dp,
                forceColor = if (item.isCompleted) financialColors.income else Color(0xFFD97706)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${saved.format(CurrencyCode.BRL, Locale("pt", "BR"))} de ${target.format(CurrencyCode.BRL, Locale("pt", "BR"))}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!item.isCompleted) {
                    OutlinedButton(
                        onClick = onContributeClick,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Aportar", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateGoalDialog(
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, targetMinor: Long, targetDate: LocalDate?, linkedAccountId: UUID?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var targetMinorText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Nova Meta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome da meta") },
                    placeholder = { Text("Ex: Reserva de emergência") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetMinorText,
                    onValueChange = { targetMinorText = it.filter { char -> char.isDigit() } },
                    label = { Text("Valor Alvo (em centavos)") },
                    placeholder = { Text("ex: 1000000 para R$ 10.000,00") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val minor = targetMinorText.toLongOrNull() ?: 0L
                    if (name.isNotBlank() && minor > 0L) {
                        onConfirm(name, minor, null, null)
                    }
                },
                enabled = name.isNotBlank() && (targetMinorText.toLongOrNull() ?: 0L) > 0L
            ) {
                Text("Criar Meta")
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
private fun ContributeGoalDialog(
    goalWithProgress: GoalWithProgress,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onConfirm: (accountId: UUID, amountMinor: Long) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var amountText by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Selecione a conta de origem"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Aporte para Meta", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Meta: ${goalWithProgress.goal.name}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
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
                        label = { Text("Conta de Origem") },
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
                    label = { Text("Valor do Aporte (em centavos)") },
                    placeholder = { Text("ex: 20000 para R$ 200,00") },
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
                Text("Confirmar Aporte")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
