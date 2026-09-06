package com.scarguard.app.util

import android.Manifest
import android.os.Build

object Permissions {
    val CAMERA = arrayOf(Manifest.permission.CAMERA)

    /** Everything needed to scan for and connect to the ESP32 over BLE, for the running SDK. */
    val BLUETOOTH: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val NOTIFICATIONS: Array<String> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        emptyArray()
    }
}
