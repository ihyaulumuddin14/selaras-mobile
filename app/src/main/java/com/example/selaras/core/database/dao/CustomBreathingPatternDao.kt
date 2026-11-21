package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface CustomBreathingPatternDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPattern(pattern: CustomBreathingPatternEntity): Long

    @Update
    suspend fun updatePattern(pattern: CustomBreathingPatternEntity)

    @Delete
    suspend fun deletePattern(pattern: CustomBreathingPatternEntity)

    @Query("DELETE FROM custom_breathing_patterns WHERE id = :id")
    suspend fun deletePatternById(id: Long)

    @Query("UPDATE custom_breathing_patterns SET is_deleted = 1, sync_status = 'PENDING_DELETE', updated_at = :now WHERE id = :id")
    suspend fun softDeletePatternById(id: Long, now: Instant)

    @Query("UPDATE custom_breathing_patterns SET sync_status = :syncStatus, updated_at = :now WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String, now: Instant)

    @Query("SELECT * FROM custom_breathing_patterns WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getPatternById(id: Long): CustomBreathingPatternEntity?

    @Query("SELECT * FROM custom_breathing_patterns WHERE uuid = :uuid AND is_deleted = 0 LIMIT 1")
    suspend fun getPatternByUuid(uuid: String): CustomBreathingPatternEntity?

    @Query("SELECT * FROM custom_breathing_patterns WHERE is_deleted = 0 ORDER BY created_at DESC")
    fun getAllPatternsFlow(): Flow<List<CustomBreathingPatternEntity>>

    @Query("SELECT * FROM custom_breathing_patterns WHERE is_deleted = 0 ORDER BY created_at DESC")
    suspend fun getAllPatterns(): List<CustomBreathingPatternEntity>

    @Query("SELECT * FROM custom_breathing_patterns WHERE sync_status != 'SYNCED'")
    suspend fun getPendingSyncPatterns(): List<CustomBreathingPatternEntity>
}
