package com.samuelbaldasso.financeflow.feature.reports

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.money.CurrencyCode
import com.samuelbaldasso.financeflow.core.model.money.Money
import com.samuelbaldasso.financeflow.core.model.report.CashFlowReport
import com.samuelbaldasso.financeflow.core.model.report.CategoryExpenseBreakdown
import com.samuelbaldasso.financeflow.designsystem.component.FinanceProgressBar
import com.samuelbaldasso.financeflow.designsystem.component.FinancialEmptyState
import com.samuelbaldasso.financeflow.designsystem.component.FinancialMoneyText
import com.samuelbaldasso.financeflow.designsystem.theme.LocalFinancialColors
import com.samuelbaldasso.financeflow.domain.usecase.importer.ImportCandidate
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    state: ReportsUiState,
    onEvent: (ReportsUiEvent) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val monthName = Month.of(state.month).getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Relatórios & Fluxo",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Fluxo de caixa e despesas por categoria",
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
                    IconButton(onClick = { onEvent(ReportsUiEvent.OpenImportDialog) }) {
                        Icon(Icons.Default.Upload, contentDescription = "Importar CSV")
                    }
                    IconButton(onClick = { onEvent(ReportsUiEvent.ExportTransactionsCsv) }) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar CSV")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Month selector bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onEvent(ReportsUiEvent.PreviousMonth) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mês anterior")
                }
                Text(
                    text = "$monthName de ${state.year}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                IconButton(onClick = { onEvent(ReportsUiEvent.NextMonth) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Próximo mês")
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            state.importSummaryMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp)
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            state.report?.let { report ->
                // Cash Flow Hero Card
                CashFlowCard(report = report)

                Spacer(modifier = Modifier.height(16.dp))

                // Breakdown Header
                Text(
                    text = "Despesas por Categoria",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (report.categoryBreakdowns.isEmpty()) {
                    FinancialEmptyState(
                        title = "Nenhuma despesa no período",
                        description = "Nenhum gasto categorizado foi registrado em $monthName.",
                        icon = Icons.Default.PieChart
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = report.categoryBreakdowns,
                            key = { it.categoryId ?: UUID.randomUUID() },
                            contentType = { "category_breakdown" }
                        ) { cat ->
                            CategoryExpenseBarItem(breakdown = cat)
                        }
                    }
                }
            }
        }
    }

    // Import Dialog
    if (state.showImportDialog) {
        CsvImportDialog(
            accounts = state.accounts,
            candidates = state.importCandidates,
            onDismiss = { onEvent(ReportsUiEvent.DismissImportDialog) },
            onParse = { text, accId -> onEvent(ReportsUiEvent.ParseCsvContent(text, accId)) },
            onConfirm = { accId, items -> onEvent(ReportsUiEvent.ConfirmImport(accId, items)) }
        )
    }

    // Exported CSV Dialog
    state.exportedCsvContent?.let { csv ->
        val clipboard = LocalClipboardManager.current
        var copied by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { onEvent(ReportsUiEvent.ClearExportedData) },
            title = {
                Text("Exportar CSV", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Column {
                    Text(
                        text = "Arquivo CSV gerado com todas as transações cadastradas:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = csv,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboard.setText(AnnotatedString(csv))
                    copied = true
                }) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (copied) "Copiado!" else "Copiar CSV")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ReportsUiEvent.ClearExportedData) }) {
                    Text("Fechar")
                }
            }
        )
    }
}

@Composable
private fun CashFlowCard(report: CashFlowReport) {
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
                    text = "RESULTADO DO MÊS (FLUXO LÍQUIDO)",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (report.isPositiveCashFlow) financialColors.incomeContainer
                            else financialColors.expenseContainer
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (report.isPositiveCashFlow) "Superávit" else "Déficit",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (report.isPositiveCashFlow) financialColors.income else financialColors.expense
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            FinancialMoneyText(
                money = report.netSavings,
                currency = CurrencyCode.BRL,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                colorOverride = if (report.isPositiveCashFlow) financialColors.income else financialColors.expense
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(financialColors.incomeContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = financialColors.income,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Receitas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FinancialMoneyText(
                            money = report.totalIncome,
                            currency = CurrencyCode.BRL,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            colorOverride = financialColors.income
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(financialColors.expenseContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = financialColors.expense,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Despesas", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FinancialMoneyText(
                            money = report.totalExpense,
                            currency = CurrencyCode.BRL,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            colorOverride = financialColors.expense
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryExpenseBarItem(breakdown: CategoryExpenseBreakdown) {
    val financialColors = LocalFinancialColors.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, financialColors.cardBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = breakdown.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FinancialMoneyText(
                        money = breakdown.totalExpense,
                        currency = CurrencyCode.BRL,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        colorOverride = financialColors.expense
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "(${breakdown.percentageOfTotal.toInt()}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FinanceProgressBar(
                progress = breakdown.percentageOfTotal / 100f,
                height = 6.dp,
                forceColor = financialColors.expense
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CsvImportDialog(
    accounts: List<Account>,
    candidates: List<ImportCandidate>,
    onDismiss: () -> Unit,
    onParse: (csvText: String, accountId: UUID) -> Unit,
    onConfirm: (accountId: UUID, items: List<ImportCandidate>) -> Unit
) {
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var csvText by remember { mutableStateOf("Data,Descricao,Valor\n2026-09-24,Mercado,85.50\n2026-09-24,Gasolina,120.00") }
    var expanded by remember { mutableStateOf(false) }

    val selectedAccountName = accounts.find { it.id == selectedAccountId }?.name ?: "Selecione a conta"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Importar Extrato CSV", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecione a conta e cole os dados em formato CSV para pré-visualização:",
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
                        label = { Text("Conta de Destino") },
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
                                text = { Text(acc.name) },
                                onClick = {
                                    selectedAccountId = acc.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    label = { Text("Conteúdo CSV") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val accId = selectedAccountId
                            if (accId != null && csvText.isNotBlank()) {
                                onParse(csvText, accId)
                            }
                        },
                        enabled = selectedAccountId != null && csvText.isNotBlank()
                    ) {
                        Text("Pré-visualizar")
                    }
                }

                if (candidates.isNotEmpty()) {
                    val duplicateCount = candidates.count { it.isDuplicate }
                    val newCount = candidates.size - duplicateCount

                    Text(
                        text = "Pré-visualização: $newCount novos, $duplicateCount duplicados",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(candidates) { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.description, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                                        Text(item.date.toString(), style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (item.isDuplicate) {
                                        Text("DUPLICADA", style = MaterialTheme.typography.labelSmall, color = Color.Red)
                                    } else {
                                        Text("NOVA", style = MaterialTheme.typography.labelSmall, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val nonDuplicates = candidates.filter { !it.isDuplicate }
            Button(
                onClick = {
                    val accId = selectedAccountId
                    if (accId != null && nonDuplicates.isNotEmpty()) {
                        onConfirm(accId, candidates)
                    }
                },
                enabled = selectedAccountId != null && candidates.any { !it.isDuplicate }
            ) {
                Text("Confirmar Importação")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
