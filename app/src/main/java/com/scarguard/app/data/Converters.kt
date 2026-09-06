package com.scarguard.app.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromRiskLevel(value: RiskLevel): String = value.name

    @TypeConverter
    fun toRiskLevel(value: String): RiskLevel = RiskLevel.valueOf(value)

    @TypeConverter
    fun fromReadingSource(value: ReadingSource): String = value.name

    @TypeConverter
    fun toReadingSource(value: String): ReadingSource = ReadingSource.valueOf(value)
}
