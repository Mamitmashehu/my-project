package com.scarguard.app

import android.app.Application
import com.scarguard.app.ble.EspBleManager
import com.scarguard.app.data.AppDatabase
import com.scarguard.app.notifications.AlertNotifier
import com.scarguard.app.repository.MonitoringRepository
import com.scarguard.app.settings.SettingsRepository

/**
 * Hand-rolled composition root. The app is small enough that a DI framework (Hilt/Koin) would
 * add more ceremony than it saves -- everything is created lazily, once, and handed out from here.
 */
class ScarGuardApp : Application() {

    val database by lazy { AppDatabase.getInstance(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val bleManager by lazy { EspBleManager(this) }
    val monitoringRepository by lazy {
        MonitoringRepository(this, database, bleManager, settingsRepository)
    }

    override fun onCreate() {
        super.onCreate()
        AlertNotifier.ensureChannels(this)
    }
}
