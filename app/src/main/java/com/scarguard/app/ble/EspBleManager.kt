package com.scarguard.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Thin wrapper around the platform BLE GATT client APIs for talking to the ESP32 sensor node.
 *
 * The ESP32 is expected to run as a BLE peripheral advertising [BleConstants.SERVICE_UUID] and
 * exposing a notifying temperature characteristic (see /firmware/esp32_scar_monitor for a
 * reference sketch). This class only depends on android.bluetooth so it can be driven from a
 * plain ViewModel or a foreground Service without dragging in any UI code.
 *
 * Callers must hold BLUETOOTH_SCAN / BLUETOOTH_CONNECT (API 31+) or BLUETOOTH / BLUETOOTH_ADMIN +
 * ACCESS_FINE_LOCATION (API <=30) before calling [startScan] or [connect]; this class does not
 * check permissions itself, it assumes the UI layer already gated the action.
 */
@SuppressLint("MissingPermission")
class EspBleManager(private val appContext: Context) {

    private val bluetoothManager =
        appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager.adapter
    private val mainHandler = Handler(Looper.getMainLooper())

    private var gatt: BluetoothGatt? = null
    private var scanning = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanResults = MutableStateFlow<List<ScannedDevice>>(emptyList())
    val scanResults: StateFlow<List<ScannedDevice>> = _scanResults.asStateFlow()

    /** Emits one Celsius value per notification received from the sensor. */
    private val _temperatureUpdates = MutableSharedFlow<Float>(
        replay = 0,
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val temperatureUpdates = _temperatureUpdates.asSharedFlow()

    val isBluetoothSupported: Boolean get() = adapter != null
    val isBluetoothEnabled: Boolean get() = adapter?.isEnabled == true

    private val foundDevices = linkedMapOf<String, ScannedDevice>()

    fun startScan(timeoutMs: Long = 12_000L) {
        val bleScanner = adapter?.bluetoothLeScanner ?: run {
            _connectionState.value = ConnectionState.Failed("Bluetooth is not available on this device")
            return
        }
        if (scanning) return
        foundDevices.clear()
        _scanResults.value = emptyList()
        _connectionState.value = ConnectionState.Scanning

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        scanning = true
        // Deliberately unfiltered: many ESP32 example sketches don't advertise the 128-bit
        // service UUID at all (only a 16-bit one, or none), so filtering here would hide real
        // devices. Results are presented as a plain device picker instead.
        bleScanner.startScan(emptyList(), settings, scanCallback)

        mainHandler.postDelayed({ stopScan() }, timeoutMs)
    }

    fun stopScan() {
        if (!scanning) return
        scanning = false
        adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val address = device.address ?: return
            val name = result.scanRecord?.deviceName ?: device.name ?: "Unknown device"
            foundDevices[address] = ScannedDevice(name = name, address = address, rssi = result.rssi)
            _scanResults.value = foundDevices.values.sortedByDescending { it.rssi }
        }

        override fun onScanFailed(errorCode: Int) {
            scanning = false
            _connectionState.value = ConnectionState.Failed("Scan failed (code $errorCode)")
        }
    }

    fun connect(address: String) {
        stopScan()
        val device: BluetoothDevice = try {
            adapter?.getRemoteDevice(address) ?: return
        } catch (e: IllegalArgumentException) {
            _connectionState.value = ConnectionState.Failed("Invalid device address")
            return
        }
        _connectionState.value = ConnectionState.Connecting(address)
        gatt = device.connectGatt(appContext, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    /** Tells the ESP32 to blink its indicator LED, e.g. to help the user line up the camera. */
    fun sendFlashCommand() {
        val g = gatt ?: return
        val characteristic = g.getService(BleConstants.SERVICE_UUID)
            ?.getCharacteristic(BleConstants.COMMAND_CHARACTERISTIC_UUID) ?: return
        writeCharacteristic(g, characteristic, byteArrayOf(BleConstants.COMMAND_FLASH))
    }

    private fun writeCharacteristic(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            g.writeCharacteristic(characteristic, value, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
        } else {
            @Suppress("DEPRECATION")
            characteristic.value = value
            @Suppress("DEPRECATION")
            g.writeCharacteristic(characteristic)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    g.requestMtu(185)
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    _connectionState.value = ConnectionState.Disconnected
                    g.close()
                    if (gatt === g) gatt = null
                }
            }
        }

        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
            // no-op: informational only
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = ConnectionState.Failed("Could not read services from device")
                return
            }
            val service = g.getService(BleConstants.SERVICE_UUID)
            val tempCharacteristic = service?.getCharacteristic(BleConstants.TEMPERATURE_CHARACTERISTIC_UUID)
            if (service == null || tempCharacteristic == null) {
                _connectionState.value = ConnectionState.Failed(
                    "Connected, but this device doesn't expose the expected sensor service"
                )
                return
            }
            g.setCharacteristicNotification(tempCharacteristic, true)
            val cccd = tempCharacteristic.getDescriptor(BleConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID)
            if (cccd != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    g.writeDescriptor(cccd, BluetoothGattDescriptorCompatValue.ENABLE_NOTIFICATION)
                } else {
                    @Suppress("DEPRECATION")
                    cccd.value = BluetoothGattDescriptorCompatValue.ENABLE_NOTIFICATION
                    @Suppress("DEPRECATION")
                    g.writeDescriptor(cccd)
                }
            }
            _connectionState.value = ConnectionState.Connected(
                name = g.device.name ?: "ESP32 sensor",
                address = g.device.address
            )
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleConstants.TEMPERATURE_CHARACTERISTIC_UUID) {
                emitTemperature(characteristic.value)
            }
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            if (characteristic.uuid == BleConstants.TEMPERATURE_CHARACTERISTIC_UUID) {
                emitTemperature(value)
            }
        }
    }

    private fun emitTemperature(bytes: ByteArray?) {
        val celsius = parseTemperature(bytes) ?: return
        _temperatureUpdates.tryEmit(celsius)
    }

    companion object {
        /**
         * Accepts either a 4-byte little-endian IEEE-754 float (the firmware's native format) or,
         * as a fallback, an ASCII decimal string (handy when testing with a generic BLE terminal
         * app that only sends text), e.g. "36.7".
         */
        fun parseTemperature(bytes: ByteArray?): Float? {
            if (bytes == null || bytes.isEmpty()) return null
            return if (bytes.size == 4) {
                ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).float
            } else {
                String(bytes, Charsets.US_ASCII).trim().toFloatOrNull()
            }
        }
    }
}

private object BluetoothGattDescriptorCompatValue {
    val ENABLE_NOTIFICATION: ByteArray =
        android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
}
