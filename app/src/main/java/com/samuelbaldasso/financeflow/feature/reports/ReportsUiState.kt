package com.samuelbaldasso.financeflow.feature.reports

import com.samuelbaldasso.financeflow.core.model.account.Account
import com.samuelbaldasso.financeflow.core.model.report.CashFlowReport
import com.samuelbaldasso.financeflow.domain.usecase.importer.ImportCandidate
import java.time.LocalDate
import java.util.UUID

data class ReportsUiState(
    val month: Int = LocalDate.now().monthValue,
    val year: Int = LocalDate.now().year,
    val report: CashFlowReport? = null,
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val exportedCsvContent: String? = null,
    val showImportDialog: Boolean = false,
    val importCandidates: List<ImportCandidate> = emptyList(),
    val selectedImportAccountId: UUID? = null,
    val importSummaryMessage: String? = null
)

sealed interface ReportsUiEvent {
    data class MonthChanged(val month: Int, val year: Int) : ReportsUiEvent
    data object PreviousMonth : ReportsUiEvent
    data object NextMonth : ReportsUiEvent
    data object ExportTransactionsCsv : ReportsUiEvent
    data object ClearExportedData : ReportsUiEvent
    data object OpenImportDialog : ReportsUiEvent
    data object DismissImportDialog : ReportsUiEvent
    data class ParseCsvContent(val csvText: String, val accountId: UUID) : ReportsUiEvent
    data class ConfirmImport(val accountId: UUID, val items: List<ImportCandidate>) : ReportsUiEvent
}
