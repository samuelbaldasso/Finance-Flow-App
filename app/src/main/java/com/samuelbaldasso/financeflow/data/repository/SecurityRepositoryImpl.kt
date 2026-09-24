package com.samuelbaldasso.financeflow.data.repository

import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import com.samuelbaldasso.financeflow.data.datastore.SecurityPreferencesDataSource
import com.samuelbaldasso.financeflow.domain.repository.SecurityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SecurityRepositoryImpl @Inject constructor(
    private val dataSource: SecurityPreferencesDataSource
) : SecurityRepository {

    override val securitySettingsFlow: Flow<SecuritySettings>
        get() = dataSource.securitySettingsFlow

    override suspend fun setPin(pin: String) {
        dataSource.setPin(pin)
    }

    override suspend fun clearPin() {
        dataSource.clearPin()
    }

    override suspend fun verifyPin(pin: String): Boolean {
        return dataSource.verifyPin(pin)
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataSource.setBiometricEnabled(enabled)
    }

    override suspend fun setLockTimeoutMinutes(minutes: Int) {
        dataSource.setLockTimeoutMinutes(minutes)
    }

    override suspend fun setScreenshotProtection(enabled: Boolean) {
        dataSource.setScreenshotProtection(enabled)
    }

    override suspend fun setTelemetryConsent(consent: Boolean) {
        dataSource.setTelemetryConsent(consent)
    }

    override suspend fun clearSecuritySettings() {
        dataSource.clearAll()
    }
}
