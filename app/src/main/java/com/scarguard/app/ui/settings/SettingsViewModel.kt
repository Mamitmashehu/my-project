package com.scarguard.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.settings.AlertThresholds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: ScarGuardApp) : ViewModel() {
    private val repository = app.monitoringRepository
    private val settings = repository.settingsRepository

    val thresholds: StateFlow<AlertThresholds> =
        settings.thresholds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlertThresholds())

    fun setTempThresholds(watch: Float, alert: Float) {
        viewModelScope.launch { settings.setTempThresholds(watch, alert) }
    }

    fun setRednessThresholds(watch: Float, alert: Float) {
        viewModelScope.launch { settings.setRednessThresholds(watch, alert) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setNotificationsEnabled(enabled) }
    }

    fun forgetDevice() {
        viewModelScope.launch {
            repository.bleManager.disconnect()
            settings.forgetDevice()
        }
    }

    fun deleteAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteEverything()
            onDone()
        }
    }
}
