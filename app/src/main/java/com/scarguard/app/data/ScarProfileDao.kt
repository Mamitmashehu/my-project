package com.scarguard.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScarProfileDao {
    @Query("SELECT * FROM scar_profiles WHERE isActive = 1 ORDER BY createdAt DESC LIMIT 1")
    fun observeActiveProfile(): Flow<ScarProfile?>

    @Query("SELECT * FROM scar_profiles ORDER BY createdAt DESC")
    fun observeAllProfiles(): Flow<List<ScarProfile>>

    @Query("SELECT * FROM scar_profiles WHERE id = :id")
    suspend fun getById(id: Long): ScarProfile?

    @Query("UPDATE scar_profiles SET isActive = 0")
    suspend fun deactivateAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ScarProfile): Long

    @Query("DELETE FROM scar_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM scar_profiles")
    suspend fun deleteAll()
}
