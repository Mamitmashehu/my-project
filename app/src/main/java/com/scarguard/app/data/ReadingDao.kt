package com.scarguard.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDao {
    @Insert
    suspend fun insert(reading: Reading): Long

    @Query("SELECT * FROM readings WHERE profileId = :profileId ORDER BY timestamp DESC")
    fun observeForProfile(profileId: Long): Flow<List<Reading>>

    @Query("SELECT * FROM readings WHERE profileId = :profileId ORDER BY timestamp DESC LIMIT 1")
    fun observeLatestForProfile(profileId: Long): Flow<Reading?>

    @Query(
        "SELECT * FROM readings WHERE profileId = :profileId AND timestamp >= :sinceMillis " +
            "ORDER BY timestamp ASC"
    )
    fun observeSince(profileId: Long, sinceMillis: Long): Flow<List<Reading>>

    @Query("DELETE FROM readings WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long)

    @Query("DELETE FROM readings")
    suspend fun deleteAll()
}
