package com.scarguard.app.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.scarguard.app.ble.EspBleManager
import com.scarguard.app.data.AppDatabase
import com.scarguard.app.data.Reading
import com.scarguard.app.data.ReadingSource
import com.scarguard.app.data.RiskClassifier
import com.scarguard.app.data.ScarProfile
import com.scarguard.app.settings.SettingsRepository
import com.scarguard.app.vision.RednessAnalyzer
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Single coordination point between BLE sensor data, the redness/skin-tone analyzer, the local
 * database, and the alert thresholds. Both the UI (ViewModels) and the background
 * [com.scarguard.app.service.MonitoringService] talk to the same repository instance so they
 * always see consistent state.
 */
class MonitoringRepository(
    context: Context,
    private val database: AppDatabase,
    val bleManager: EspBleManager,
    val settingsRepository: SettingsRepository,
) {
    private val appContext = context.applicationContext
    private val scarProfileDao = database.scarProfileDao()
    private val readingDao = database.readingDao()

    val activeProfile: Flow<ScarProfile?> = scarProfileDao.observeActiveProfile()
    val allProfiles: Flow<List<ScarProfile>> = scarProfileDao.observeAllProfiles()

    fun readingsForProfile(profileId: Long): Flow<List<Reading>> = readingDao.observeForProfile(profileId)

    suspend fun createBaseline(
        label: String,
        photoFile: File,
        photoBitmap: Bitmap,
        currentTemperatureC: Float?,
    ): ScarProfile {
        val summary = RednessAnalyzer.analyze(photoBitmap)
        scarProfileDao.deactivateAll()
        val profile = ScarProfile(
            label = label,
            createdAt = System.currentTimeMillis(),
            baselinePhotoPath = photoFile.absolutePath,
            baselineRednessScore = summary.rednessScore,
            baselineAvgRed = summary.avgRed,
            baselineAvgGreen = summary.avgGreen,
            baselineAvgBlue = summary.avgBlue,
            baselineTemperatureC = currentTemperatureC,
            isActive = true,
        )
        val id = scarProfileDao.upsert(profile)
        return profile.copy(id = id)
    }

    data class PhotoReadingOutcome(val reading: Reading, val heatmap: Bitmap, val comparison: RednessAnalyzer.Comparison)

    suspend fun recordPhotoReading(
        profile: ScarProfile,
        photoFile: File,
        photoBitmap: Bitmap,
        currentTemperatureC: Float?,
    ): PhotoReadingOutcome {
        val baselineBitmap = BitmapFactory.decodeFile(profile.baselinePhotoPath)
        val baselineSummary = RednessAnalyzer.ColorSummary(
            rednessScore = profile.baselineRednessScore,
            avgRed = profile.baselineAvgRed,
            avgGreen = profile.baselineAvgGreen,
            avgBlue = profile.baselineAvgBlue,
        )
        val comparison = RednessAnalyzer.compare(photoBitmap, baselineSummary, baselineBitmap)
        val tempDelta = deltaOrNull(currentTemperatureC, profile.baselineTemperatureC)
        val thresholds = settingsRepository.thresholds.first()
        val risk = RiskClassifier.classify(tempDelta, comparison.rednessDelta, thresholds)

        val reading = Reading(
            profileId = profile.id,
            timestamp = System.currentTimeMillis(),
            source = if (currentTemperatureC != null) ReadingSource.BOTH else ReadingSource.PHOTO,
            temperatureC = currentTemperatureC,
            temperatureDeltaC = tempDelta,
            photoPath = photoFile.absolutePath,
            rednessScore = comparison.current.rednessScore,
            rednessDelta = comparison.rednessDelta,
            colorShiftScore = comparison.colorShiftScore,
            riskLevel = risk,
        )
        val id = readingDao.insert(reading)
        return PhotoReadingOutcome(reading.copy(id = id), comparison.heatmap, comparison)
    }

    suspend fun recordTemperatureOnly(profile: ScarProfile, temperatureC: Float): Reading {
        val tempDelta = deltaOrNull(temperatureC, profile.baselineTemperatureC)
        val thresholds = settingsRepository.thresholds.first()
        val risk = RiskClassifier.classify(tempDelta, null, thresholds)
        val reading = Reading(
            profileId = profile.id,
            timestamp = System.currentTimeMillis(),
            source = ReadingSource.TEMPERATURE,
            temperatureC = temperatureC,
            temperatureDeltaC = tempDelta,
            riskLevel = risk,
        )
        val id = readingDao.insert(reading)
        return reading.copy(id = id)
    }

    suspend fun deleteEverything() {
        readingDao.deleteAll()
        scarProfileDao.deleteAll()
    }

    private fun deltaOrNull(current: Float?, baseline: Float?): Float? =
        if (current != null && baseline != null) current - baseline else null
}
