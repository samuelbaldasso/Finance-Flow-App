package com.samuelbaldasso.financeflow.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.core.model.transaction.Transaction
import com.samuelbaldasso.financeflow.core.model.transaction.TransactionStatus
import com.samuelbaldasso.financeflow.domain.repository.AccountRepository
import com.samuelbaldasso.financeflow.domain.repository.TransactionRepository
import com.samuelbaldasso.financeflow.domain.usecase.importer.CsvTransactionService
import com.samuelbaldasso.financeflow.domain.usecase.importer.ImportCandidate
import com.samuelbaldasso.financeflow.domain.usecase.report.GetCashFlowReportUseCase
import com.samuelbaldasso.financeflow.domain.usecase.transaction.CreateTransactionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel @Inject constructor(
    private val getCashFlowReportUseCase: GetCashFlowReportUseCase,
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val createTransactionUseCase: CreateTransactionUseCase
) : ViewModel() {

    private val _currentPeriod = MutableStateFlow(
        Pair(LocalDate.now().monthValue, LocalDate.now().year)
    )
    private val _dialogState = MutableStateFlow(false)
    private val _exportedCsv = MutableStateFlow<String?>(null)
    private val _importCandidates = MutableStateFlow<List<ImportCandidate>>(emptyList())
    private val _importSummary = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ReportsUiState> = combine(
        _currentPeriod,
        _dialogState,
        _exportedCsv,
        _importCandidates,
        _importSummary
    ) { period, showImport, exported, candidates, summary ->
        DataState(period, showImport, exported, candidates, summary)
    }.flatMapLatest { data ->
        val (month, year) = data.period
        combine(
            getCashFlowReportUseCase(month, year),
            accountRepository.getActiveAccountsFlow()
        ) { report, accounts ->
            ReportsUiState(
                month = month,
                year = year,
                report = report,
                accounts = accounts,
                isLoading = false,
                exportedCsvContent = data.exported,
                showImportDialog = data.showImport,
                importCandidates = data.candidates,
                importSummaryMessage = data.summary
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReportsUiState(isLoading = true)
    )

    fun onEvent(event: ReportsUiEvent) {
        when (event) {
            is ReportsUiEvent.MonthChanged -> {
                _currentPeriod.value = Pair(event.month, event.year)
            }
            ReportsUiEvent.PreviousMonth -> {
                _currentPeriod.update { (m, y) ->
                    if (m == 1) Pair(12, y - 1) else Pair(m - 1, y)
                }
            }
            ReportsUiEvent.NextMonth -> {
                _currentPeriod.update { (m, y) ->
                    if (m == 12) Pair(1, y + 1) else Pair(m + 1, y)
                }
            }
            ReportsUiEvent.ExportTransactionsCsv -> {
                viewModelScope.launch {
                    val allTxs = transactionRepository.getAllTransactionsFlow().first()
                    val csv = CsvTransactionService.exportTransactionsToCsv(allTxs)
                    _exportedCsv.value = csv
                }
            }
            ReportsUiEvent.ClearExportedData -> {
                _exportedCsv.value = null
            }
            ReportsUiEvent.OpenImportDialog -> {
                _dialogState.value = true
                _importSummary.value = null
            }
            ReportsUiEvent.DismissImportDialog -> {
                _dialogState.value = false
                _importCandidates.value = emptyList()
            }
            is ReportsUiEvent.ParseCsvContent -> {
                viewModelScope.launch {
                    val table = CsvTransactionService.parseCsv(event.csvText)
                    val existing = transactionRepository.getAllTransactionsFlow().first()
                    // Default to col 0: date, col 1: description, col 2: amount
                    val candidates = CsvTransactionService.generatePreview(
                        table = table,
                        targetAccountId = event.accountId,
                        existingTransactions = existing,
                        dateColIndex = 0,
                        descColIndex = 1,
                        amountColIndex = 2
                    )
                    _importCandidates.value = candidates
                }
            }
            is ReportsUiEvent.ConfirmImport -> {
                viewModelScope.launch {
                    val nonDuplicates = event.items.filter { !it.isDuplicate }
                    var importedCount = 0
                    for (item in nonDuplicates) {
                        val tx = Transaction(
                            id = UUID.randomUUID(),
                            accountId = event.accountId,
                            type = item.type,
                            amount = item.amount,
                            competenceDate = item.date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                            effectiveDate = item.date.atStartOfDay(ZoneOffset.UTC).toInstant(),
                            description = item.description,
                            status = TransactionStatus.CLEARED
                        )
                        createTransactionUseCase(tx)
                        importedCount++
                    }
                    _importCandidates.value = emptyList()
                    _dialogState.value = false
                    _importSummary.value = "$importedCount transações importadas com sucesso!"
                }
            }
        }
    }

    private data class DataState(
        val period: Pair<Int, Int>,
        val showImport: Boolean,
        val exported: String?,
        val candidates: List<ImportCandidate>,
        val summary: String?
    )
}
