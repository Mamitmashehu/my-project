package com.scarguard.app.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.scarguard.app.ScarGuardApp
import com.scarguard.app.data.RiskLevel
import com.scarguard.app.notifications.AlertNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Keeps the BLE connection to the ESP32 alive while the app is backgrounded, persists periodic
 * temperature readings, and raises a high-priority notification the moment risk moves out of
 * NORMAL. Started from [com.scarguard.app.ui.connect.ConnectViewModel] once a device connects,
 * and stopped when the user disconnects.
 */
class MonitoringService : LifecycleService() {

    private val container get() = (application as ScarGuardApp)

    // Avoid writing a DB row on every single BLE notification; a sample every few minutes is
    // plenty for a temperature trend, while live UI still gets every update via the shared flow.
    private var lastPersistedAt = 0L
    private var lastNotifiedRisk = RiskLevel.NORMAL
    private var latestTemperature: Float? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        AlertNotifier.ensureChannels(this)
        startForeground(
            AlertNotifier.MONITORING_NOTIFICATION_ID,
            AlertNotifier.buildMonitoringNotification(this, connected = true, latestTemperatureC = null)
        )
        observeTemperature()
        return Service.START_STICKY
    }

    private fun observeTemperature() {
        lifecycleScope.launch {
            container.monitoringRepository.bleManager.temperatureUpdates.collect { celsius ->
                latestTemperature = celsius
                updateNotification(celsius)

                val profile = container.monitoringRepository.activeProfile.first() ?: return@collect
                val now = System.currentTimeMillis()
                if (now - lastPersistedAt >= PERSIST_INTERVAL_MS) {
                    lastPersistedAt = now
                    val reading = container.monitoringRepository.recordTemperatureOnly(profile, celsius)
                    maybeNotify(reading.riskLevel)
                } else {
                    // Still evaluate risk against the baseline for immediate alerting, without
                    // writing a new row every time.
                    val delta = profile.baselineTemperatureC?.let { celsius - it }
                    val thresholds = container.monitoringRepository.settingsRepository.thresholds.first()
                    val risk = com.scarguard.app.data.RiskClassifier.classify(delta, null, thresholds)
                    maybeNotify(risk)
                }
            }
        }
    }

    private fun updateNotification(celsius: Float?) {
        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager?.notify(
            AlertNotifier.MONITORING_NOTIFICATION_ID,
            AlertNotifier.buildMonitoringNotification(this, connected = true, latestTemperatureC = celsius)
        )
    }

    private suspend fun maybeNotify(risk: RiskLevel) {
        if (risk == lastNotifiedRisk) return
        lastNotifiedRisk = risk
        if (risk == RiskLevel.NORMAL) return
        val thresholds = container.monitoringRepository.settingsRepository.thresholds.first()
        if (!thresholds.notificationsEnabled) return
        val message = com.scarguard.app.data.RiskClassifier.adviceFor(risk)
        AlertNotifier.notifyRisk(this, risk, message)
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    companion object {
        private const val PERSIST_INTERVAL_MS = 5 * 60 * 1000L
    }
}
