package com.samuelbaldasso.financeflow.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "finance_flow_security_prefs")

class SecurityPreferencesDataSource(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val KEY_LOCK_TIMEOUT_MINUTES = intPreferencesKey("lock_timeout_minutes")
        private val KEY_SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        private val KEY_TELEMETRY_CONSENT = booleanPreferencesKey("telemetry_consent")

        private const val SALT = "FinanceFlow_Security_Salt_v1_"

        fun hashPin(pin: String): String {
            val salted = "$SALT$pin"
            val digest = MessageDigest.getInstance("SHA-256").digest(salted.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }

    val securitySettingsFlow: Flow<SecuritySettings> = dataStore.data.map { prefs ->
        val pinHash = prefs[KEY_PIN_HASH]
        SecuritySettings(
            isPinSet = !pinHash.isNullOrBlank(),
            isBiometricEnabled = prefs[KEY_BIOMETRIC_ENABLED] ?: false,
            lockTimeoutMinutes = prefs[KEY_LOCK_TIMEOUT_MINUTES] ?: 1,
            isScreenshotProtectionEnabled = prefs[KEY_SCREENSHOT_PROTECTION] ?: true,
            telemetryConsent = prefs[KEY_TELEMETRY_CONSENT] ?: false
        )
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        dataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
        }
    }

    suspend fun clearPin() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val currentHash = dataStore.data.map { it[KEY_PIN_HASH] }.first() ?: return false
        val candidateHash = hashPin(pin)
        return currentHash == candidateHash
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setLockTimeoutMinutes(minutes: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_LOCK_TIMEOUT_MINUTES] = minutes
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setTelemetryConsent(consent: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TELEMETRY_CONSENT] = consent
        }
    }

    suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
