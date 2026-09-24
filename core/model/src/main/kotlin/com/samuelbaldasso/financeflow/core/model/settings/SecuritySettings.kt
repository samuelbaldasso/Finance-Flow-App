package com.samuelbaldasso.financeflow.core.model.settings

data class SecuritySettings(
    val isPinSet: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 1,
    val isScreenshotProtectionEnabled: Boolean = false,
    val telemetryConsent: Boolean = false
) {
    val isAppLockEnabled: Boolean
        get() = isPinSet || isBiometricEnabled
}
