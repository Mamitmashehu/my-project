package com.scarguard.app.ble

import java.util.UUID

/**
 * GATT contract shared with the ESP32 firmware (see /firmware/esp32_scar_monitor).
 * Keep these UUIDs in sync on both sides if you change either.
 */
object BleConstants {
    val SERVICE_UUID: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")

    /** Notified by the ESP32 whenever a new temperature sample is ready. Payload: 4-byte little-endian float, degrees Celsius. */
    val TEMPERATURE_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")

    /** Write-only. The app sends a single byte command; 0x01 = "flash indicator LED / mark capture moment". */
    val COMMAND_CHARACTERISTIC_UUID: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")

    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    const val COMMAND_FLASH: Byte = 0x01
}
