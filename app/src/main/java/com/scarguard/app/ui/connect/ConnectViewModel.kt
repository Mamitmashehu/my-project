package com.scarguard.app.ui.connect

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.ble.ConnectionState
import com.scarguard.app.ble.ScannedDevice
import com.scarguard.app.service.MonitoringService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConnectUiState(
    val connectionState: ConnectionState = ConnectionState.Idle,
    val scanResults: List<ScannedDevice> = emptyList(),
    val bluetoothSupported: Boolean = true,
    val bluetoothEnabled: Boolean = true,
    val rememberedDeviceName: String? = null,
)

class ConnectViewModel(private val app: ScarGuardApp) : ViewModel() {
    private val ble = app.monitoringRepository.bleManager
    private val settings = app.monitoringRepository.settingsRepository

    val uiState: StateFlow<ConnectUiState> = combine(
        ble.connectionState,
        ble.scanResults,
        settings.thresholds,
    ) { connectionState, scanResults, thresholds ->
        ConnectUiState(
            connectionState = connectionState,
            scanResults = scanResults,
            bluetoothSupported = ble.isBluetoothSupported,
            bluetoothEnabled = ble.isBluetoothEnabled,
            rememberedDeviceName = thresholds.lastConnectedDeviceName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectUiState())

    init {
        viewModelScope.launch {
            ble.connectionState.collect { state ->
                when (state) {
                    is ConnectionState.Connected -> {
                        settings.rememberDevice(state.address, state.name)
                        startMonitoringService()
                    }
                    ConnectionState.Disconnected, ConnectionState.Idle -> stopMonitoringService()
                    else -> Unit
                }
            }
        }
    }

    fun startScan() = ble.startScan()
    fun stopScan() = ble.stopScan()
    fun connect(address: String) = ble.connect(address)
    fun disconnect() = ble.disconnect()

    private fun startMonitoringService() {
        ContextCompat.startForegroundService(app, Intent(app, MonitoringService::class.java))
    }

    private fun stopMonitoringService() {
        app.stopService(Intent(app, MonitoringService::class.java))
    }
}
