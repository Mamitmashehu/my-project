package com.scarguard.app.ble

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object Scanning : ConnectionState
    data class Connecting(val address: String) : ConnectionState
    data class Connected(val name: String, val address: String) : ConnectionState
    data object Disconnected : ConnectionState
    data class Failed(val message: String) : ConnectionState
}

data class ScannedDevice(
    val name: String,
    val address: String,
    val rssi: Int,
)
