package com.scarguard.app.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "scarguard_settings")

data class AlertThresholds(
    val watchTempDeltaC: Float = 0.8f,
    val alertTempDeltaC: Float = 1.5f,
    val watchRednessDelta: Float = 10f,
    val alertRednessDelta: Float = 20f,
    val notificationsEnabled: Boolean = true,
    val lastConnectedDeviceAddress: String? = null,
    val lastConnectedDeviceName: String? = null,
)

/** Small DataStore-backed store for user-tunable alert thresholds and the last paired device. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val WATCH_TEMP = floatPreferencesKey("watch_temp_delta_c")
        val ALERT_TEMP = floatPreferencesKey("alert_temp_delta_c")
        val WATCH_REDNESS = floatPreferencesKey("watch_redness_delta")
        val ALERT_REDNESS = floatPreferencesKey("alert_redness_delta")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val DEVICE_ADDRESS = stringPreferencesKey("device_address")
        val DEVICE_NAME = stringPreferencesKey("device_name")
    }

    val thresholds: Flow<AlertThresholds> = context.dataStore.data.map { prefs ->
        AlertThresholds(
            watchTempDeltaC = prefs[Keys.WATCH_TEMP] ?: 0.8f,
            alertTempDeltaC = prefs[Keys.ALERT_TEMP] ?: 1.5f,
            watchRednessDelta = prefs[Keys.WATCH_REDNESS] ?: 10f,
            alertRednessDelta = prefs[Keys.ALERT_REDNESS] ?: 20f,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            lastConnectedDeviceAddress = prefs[Keys.DEVICE_ADDRESS],
            lastConnectedDeviceName = prefs[Keys.DEVICE_NAME],
        )
    }

    suspend fun setTempThresholds(watch: Float, alert: Float) {
        context.dataStore.edit {
            it[Keys.WATCH_TEMP] = watch
            it[Keys.ALERT_TEMP] = alert
        }
    }

    suspend fun setRednessThresholds(watch: Float, alert: Float) {
        context.dataStore.edit {
            it[Keys.WATCH_REDNESS] = watch
            it[Keys.ALERT_REDNESS] = alert
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun rememberDevice(address: String, name: String?) {
        context.dataStore.edit {
            it[Keys.DEVICE_ADDRESS] = address
            it[Keys.DEVICE_NAME] = name ?: "ESP32 sensor"
        }
    }

    suspend fun forgetDevice() {
        context.dataStore.edit {
            it.remove(Keys.DEVICE_ADDRESS)
            it.remove(Keys.DEVICE_NAME)
        }
    }
}
