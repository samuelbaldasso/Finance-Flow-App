package com.samuelbaldasso.financeflow.domain.security

import com.samuelbaldasso.financeflow.core.model.settings.SecuritySettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppLockManager {

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var lastBackgroundTimestamp: Long = 0L

    fun onAppBackgrounded() {
        lastBackgroundTimestamp = System.currentTimeMillis()
    }

    fun onAppForegrounded(settings: SecuritySettings) {
        if (!settings.isAppLockEnabled) {
            _isLocked.value = false
            return
        }

        if (settings.lockTimeoutMinutes == -1) {
            // Lock disabled on timeout, only locks if already locked
            return
        }

        val elapsed = System.currentTimeMillis() - lastBackgroundTimestamp
        val timeoutMillis = settings.lockTimeoutMinutes * 60 * 1000L

        if (lastBackgroundTimestamp == 0L || elapsed >= timeoutMillis) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
    }

    fun lockImmediately() {
        _isLocked.value = true
    }
}
