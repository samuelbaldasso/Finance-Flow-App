package com.samuelbaldasso.financeflow.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import com.samuelbaldasso.financeflow.domain.security.AppLockManager
import com.samuelbaldasso.financeflow.domain.usecase.security.ExportAllUserDataUseCase
import com.samuelbaldasso.financeflow.domain.usecase.security.WipeAllUserDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel internal constructor(
    private val securityRepository: SecurityRepository,
    private val exportAllUserDataUseCase: ExportAllUserDataUseCase,
    private val wipeAllUserDataUseCase: WipeAllUserDataUseCase,
    private val appLockManager: AppLockManager,
    sharingStarted: SharingStarted
) : ViewModel() {

    @Inject
    constructor(
        securityRepository: SecurityRepository,
        exportAllUserDataUseCase: ExportAllUserDataUseCase,
        wipeAllUserDataUseCase: WipeAllUserDataUseCase,
        appLockManager: AppLockManager
    ) : this(
        securityRepository = securityRepository,
        exportAllUserDataUseCase = exportAllUserDataUseCase,
        wipeAllUserDataUseCase = wipeAllUserDataUseCase,
        appLockManager = appLockManager,
        sharingStarted = SharingStarted.WhileSubscribed(5_000)
    )

    private val _showSetPinDialog = MutableStateFlow(false)
    private val _showWipeConfirmDialog = MutableStateFlow(false)
    private val _exportedDataJson = MutableStateFlow<String?>(null)
    private val _userFeedbackMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        securityRepository.securitySettingsFlow,
        _showSetPinDialog,
        _showWipeConfirmDialog,
        _exportedDataJson,
        _userFeedbackMessage
    ) { settings, showPin, showWipe, exported, feedback ->
        SettingsUiState(
            securitySettings = settings,
            isLoading = false,
            showSetPinDialog = showPin,
            showWipeConfirmDialog = showWipe,
            exportedDataJson = exported,
            userFeedbackMessage = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = sharingStarted,
        initialValue = SettingsUiState(isLoading = true)
    )

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.ToggleBiometric -> {
                viewModelScope.launch {
                    securityRepository.setBiometricEnabled(event.enabled)
                }
            }
            is SettingsUiEvent.ChangeLockTimeout -> {
                viewModelScope.launch {
                    securityRepository.setLockTimeoutMinutes(event.minutes)
                }
            }
            is SettingsUiEvent.ToggleScreenshotProtection -> {
                viewModelScope.launch {
                    securityRepository.setScreenshotProtection(event.enabled)
                }
            }
            is SettingsUiEvent.ToggleTelemetryConsent -> {
                viewModelScope.launch {
                    securityRepository.setTelemetryConsent(event.consent)
                }
            }
            SettingsUiEvent.OpenSetPinDialog -> {
                _showSetPinDialog.value = true
            }
            SettingsUiEvent.DismissSetPinDialog -> {
                _showSetPinDialog.value = false
            }
            is SettingsUiEvent.SetNewPin -> {
                _showSetPinDialog.value = false
                viewModelScope.launch {
                    securityRepository.setPin(event.pin)
                    _userFeedbackMessage.value = "PIN configurado com sucesso!"
                }
            }
            SettingsUiEvent.RemovePin -> {
                viewModelScope.launch {
                    securityRepository.clearPin()
                    _userFeedbackMessage.value = "Bloqueio por PIN desativado."
                }
            }
            SettingsUiEvent.OpenWipeConfirmDialog -> {
                _showWipeConfirmDialog.value = true
            }
            SettingsUiEvent.DismissWipeConfirmDialog -> {
                _showWipeConfirmDialog.value = false
            }
            SettingsUiEvent.ConfirmWipeAllData -> {
                _showWipeConfirmDialog.value = false
                viewModelScope.launch {
                    wipeAllUserDataUseCase()
                    _userFeedbackMessage.value = "Todos os dados foram excluídos com sucesso (LGPD Art. 18)."
                }
            }
            SettingsUiEvent.ExportAllData -> {
                viewModelScope.launch {
                    val json = exportAllUserDataUseCase()
                    _exportedDataJson.value = json
                }
            }
            SettingsUiEvent.ClearExportedData -> {
                _exportedDataJson.value = null
            }
            SettingsUiEvent.ClearFeedbackMessage -> {
                _userFeedbackMessage.value = null
            }
        }
    }
}
