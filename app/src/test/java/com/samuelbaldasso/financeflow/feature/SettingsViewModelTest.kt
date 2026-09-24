package com.samuelbaldasso.financeflow.feature

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.samuelbaldasso.financeflow.core.database.FinanceFlowDatabase
import com.samuelbaldasso.financeflow.data.datastore.SecurityPreferencesDataSource
import com.samuelbaldasso.financeflow.data.datastore.securityDataStore
import com.samuelbaldasso.financeflow.data.repository.AccountRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.AuditRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.BudgetRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.CategoryRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.GoalRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.SecurityRepositoryImpl
import com.samuelbaldasso.financeflow.data.repository.TransactionRepositoryImpl
import com.samuelbaldasso.financeflow.domain.security.AppLockManager
import com.samuelbaldasso.financeflow.domain.usecase.security.ExportAllUserDataUseCase
import com.samuelbaldasso.financeflow.domain.usecase.security.WipeAllUserDataUseCase
import com.samuelbaldasso.financeflow.feature.settings.SettingsUiEvent
import com.samuelbaldasso.financeflow.feature.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: FinanceFlowDatabase
    private lateinit var accountRepo: AccountRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var transactionRepo: TransactionRepositoryImpl
    private lateinit var budgetRepo: BudgetRepositoryImpl
    private lateinit var goalRepo: GoalRepositoryImpl
    private lateinit var auditRepo: AuditRepositoryImpl
    private lateinit var securityRepo: SecurityRepositoryImpl
    private lateinit var appLockManager: AppLockManager

    private lateinit var exportAllUserDataUseCase: ExportAllUserDataUseCase
    private lateinit var wipeAllUserDataUseCase: WipeAllUserDataUseCase
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, FinanceFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        auditRepo = AuditRepositoryImpl(db.auditLogDao())
        accountRepo = AccountRepositoryImpl(db.accountDao(), db.transactionDao(), auditRepo)
        categoryRepo = CategoryRepositoryImpl(db.categoryDao(), auditRepo)
        transactionRepo = TransactionRepositoryImpl(db.transactionDao(), auditRepo)
        budgetRepo = BudgetRepositoryImpl(db.budgetDao(), db.categoryDao(), db.transactionDao())
        goalRepo = GoalRepositoryImpl(db.goalDao(), db.transactionDao())

        val ds = SecurityPreferencesDataSource(context.securityDataStore)
        securityRepo = SecurityRepositoryImpl(ds)
        appLockManager = AppLockManager()

        exportAllUserDataUseCase = ExportAllUserDataUseCase(
            accountRepo, transactionRepo, categoryRepo, budgetRepo, goalRepo, auditRepo
        )
        wipeAllUserDataUseCase = WipeAllUserDataUseCase(db, securityRepo, categoryRepo)

        runTest(testDispatcher) {
            securityRepo.clearSecuritySettings()
        }

        viewModel = SettingsViewModel(
            securityRepository = securityRepo,
            exportAllUserDataUseCase = exportAllUserDataUseCase,
            wipeAllUserDataUseCase = wipeAllUserDataUseCase,
            appLockManager = appLockManager,
            sharingStarted = SharingStarted.Eagerly
        )
    }

    @After
    fun tearDown() {
        runTest(testDispatcher) {
            securityRepo.clearSecuritySettings()
        }
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `setting and removing PIN updates uiState`() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        val initial = viewModel.uiState.first { !it.isLoading && !it.securitySettings.isPinSet }
        assertFalse(initial.securitySettings.isPinSet)

        viewModel.onEvent(SettingsUiEvent.OpenSetPinDialog)
        val dialogState = viewModel.uiState.first { it.showSetPinDialog }
        assertTrue(dialogState.showSetPinDialog)

        viewModel.onEvent(SettingsUiEvent.SetNewPin("1234"))
        val pinSetState = viewModel.uiState.first {
            it.securitySettings.isPinSet && !it.showSetPinDialog && it.userFeedbackMessage != null
        }
        assertTrue(pinSetState.securitySettings.isPinSet)
        assertFalse(pinSetState.showSetPinDialog)
        assertNotNull(pinSetState.userFeedbackMessage)

        viewModel.onEvent(SettingsUiEvent.RemovePin)
        val removedState = viewModel.uiState.first { !it.securitySettings.isPinSet }
        assertFalse(removedState.securitySettings.isPinSet)
    }

    @Test
    fun `toggling options updates settings in uiState`() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(SettingsUiEvent.ToggleBiometric(true))
        val state1 = viewModel.uiState.first { it.securitySettings.isBiometricEnabled }
        assertTrue(state1.securitySettings.isBiometricEnabled)

        viewModel.onEvent(SettingsUiEvent.ChangeLockTimeout(15))
        val state2 = viewModel.uiState.first { it.securitySettings.lockTimeoutMinutes == 15 }
        assertEquals(15, state2.securitySettings.lockTimeoutMinutes)

        viewModel.onEvent(SettingsUiEvent.ToggleScreenshotProtection(false))
        val state3 = viewModel.uiState.first { !it.securitySettings.isScreenshotProtectionEnabled }
        assertFalse(state3.securitySettings.isScreenshotProtectionEnabled)

        viewModel.onEvent(SettingsUiEvent.ToggleTelemetryConsent(true))
        val state4 = viewModel.uiState.first { it.securitySettings.telemetryConsent }
        assertTrue(state4.securitySettings.telemetryConsent)
    }

    @Test
    fun `export and wipe flow updates uiState with exported JSON and feedback`() = runTest(testDispatcher) {
        backgroundScope.launch(testDispatcher) { viewModel.uiState.collect() }

        viewModel.uiState.first { !it.isLoading }

        viewModel.onEvent(SettingsUiEvent.ExportAllData)
        val exportedState = viewModel.uiState.first { it.exportedDataJson != null }
        assertNotNull(exportedState.exportedDataJson)
        assertTrue(exportedState.exportedDataJson!!.contains("FinanceFlow"))

        viewModel.onEvent(SettingsUiEvent.ClearExportedData)
        val clearedExportState = viewModel.uiState.first { it.exportedDataJson == null }
        assertNull(clearedExportState.exportedDataJson)

        // Wipe confirmation dialog
        viewModel.onEvent(SettingsUiEvent.OpenWipeConfirmDialog)
        val wipeConfirmState = viewModel.uiState.first { it.showWipeConfirmDialog }
        assertTrue(wipeConfirmState.showWipeConfirmDialog)

        viewModel.onEvent(SettingsUiEvent.ConfirmWipeAllData)
        val afterWipeState = viewModel.uiState.first { !it.showWipeConfirmDialog && it.userFeedbackMessage != null }
        assertFalse(afterWipeState.showWipeConfirmDialog)
        assertNotNull(afterWipeState.userFeedbackMessage)
        assertTrue(afterWipeState.userFeedbackMessage!!.contains("LGPD"))
    }
}
