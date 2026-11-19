package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT * FROM custom_breathing_patterns WHERE id = :id LIMIT 1")
    suspend fun getPatternById(id: Long): CustomBreathingPatternEntity?

    @Query("SELECT * FROM custom_breathing_patterns ORDER BY created_at DESC")
    fun getAllPatternsFlow(): Flow<List<CustomBreathingPatternEntity>>

    @Query("SELECT * FROM custom_breathing_patterns ORDER BY created_at DESC")
    suspend fun getAllPatterns(): List<CustomBreathingPatternEntity>
}
