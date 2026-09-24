package com.samuelbaldasso.financeflow.feature.settings

import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings

data class SettingsUiState(
    val securitySettings: SecuritySettings = SecuritySettings(),
    val isLoading: Boolean = false,
    val showSetPinDialog: Boolean = false,
    val showWipeConfirmDialog: Boolean = false,
    val exportedDataJson: String? = null,
    val userFeedbackMessage: String? = null
)

sealed interface SettingsUiEvent {
    data class ToggleBiometric(val enabled: Boolean) : SettingsUiEvent
    data class ChangeLockTimeout(val minutes: Int) : SettingsUiEvent
    data class ToggleScreenshotProtection(val enabled: Boolean) : SettingsUiEvent
    data class ToggleTelemetryConsent(val consent: Boolean) : SettingsUiEvent
    data object OpenSetPinDialog : SettingsUiEvent
    data object DismissSetPinDialog : SettingsUiEvent
    data class SetNewPin(val pin: String) : SettingsUiEvent
    data object RemovePin : SettingsUiEvent
    data object OpenWipeConfirmDialog : SettingsUiEvent
    data object DismissWipeConfirmDialog : SettingsUiEvent
    data object ConfirmWipeAllData : SettingsUiEvent
    data object ExportAllData : SettingsUiEvent
    data object ClearExportedData : SettingsUiEvent
    data object ClearFeedbackMessage : SettingsUiEvent
}
