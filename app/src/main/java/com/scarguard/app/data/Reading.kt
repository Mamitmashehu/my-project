package com.scarguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ReadingSource { TEMPERATURE, PHOTO, BOTH }

/**
 * One point in the monitoring timeline for a [ScarProfile]: a temperature sample,
 * a follow-up photo comparison, or both taken together.
 */
@Entity(tableName = "readings")
data class Reading(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val timestamp: Long,
    val source: ReadingSource,
    val temperatureC: Float? = null,
    val temperatureDeltaC: Float? = null,
    val photoPath: String? = null,
    val rednessScore: Float? = null,
    val rednessDelta: Float? = null,
    val colorShiftScore: Float? = null,
    val riskLevel: RiskLevel,
    val note: String? = null,
)
