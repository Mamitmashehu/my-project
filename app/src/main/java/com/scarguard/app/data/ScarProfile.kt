package com.scarguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single monitored site (e.g. one incision). Holds the "before" reference values
 * everything else is compared against: the baseline photo and the baseline skin
 * temperature captured right after it was taken.
 */
@Entity(tableName = "scar_profiles")
data class ScarProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val createdAt: Long,
    val baselinePhotoPath: String,
    val baselineRednessScore: Float,
    val baselineAvgRed: Float,
    val baselineAvgGreen: Float,
    val baselineAvgBlue: Float,
    val baselineTemperatureC: Float?,
    val isActive: Boolean = true,
)
