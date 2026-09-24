package com.samuelbaldasso.financeflow.domain.repository

import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import kotlinx.coroutines.flow.Flow

interface SecurityRepository {
    val securitySettingsFlow: Flow<SecuritySettings>
    suspend fun setPin(pin: String)
    suspend fun clearPin()
    suspend fun verifyPin(pin: String): Boolean
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun setLockTimeoutMinutes(minutes: Int)
    suspend fun setScreenshotProtection(enabled: Boolean)
    suspend fun setTelemetryConsent(consent: Boolean)
    suspend fun clearSecuritySettings()
}
