package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.ScreeningResultEntity
import com.example.selaras.domain.model.ScreeningType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface ScreeningResultDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertScreeningResult(result: ScreeningResultEntity): Long

    @Update
    suspend fun updateScreeningResult(result: ScreeningResultEntity)

    @Delete
    suspend fun deleteScreeningResult(result: ScreeningResultEntity)

    @Query("DELETE FROM screening_results WHERE id = :id")
    suspend fun deleteScreeningResultById(id: Long)

    @Query("SELECT * FROM screening_results WHERE id = :id LIMIT 1")
    suspend fun getScreeningResultById(id: Long): ScreeningResultEntity?

    @Query(
        """
        SELECT * FROM screening_results 
        WHERE screening_type = :screeningType 
          AND period_start_date = :startDate 
          AND period_end_date = :endDate 
        LIMIT 1
        """
    )
    suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResultEntity?

    @Query("SELECT * FROM screening_results ORDER BY completed_at DESC")
    fun getAllScreeningResultsFlow(): Flow<List<ScreeningResultEntity>>

    @Query("SELECT * FROM screening_results ORDER BY completed_at DESC LIMIT 1")
    suspend fun getLatestScreeningResult(): ScreeningResultEntity?

    @Query("SELECT * FROM screening_results ORDER BY completed_at DESC LIMIT 1")
    fun getLatestScreeningResultFlow(): Flow<ScreeningResultEntity?>

    @Query(
        """
        SELECT * FROM screening_results 
        WHERE screening_type = 'routine' 
        ORDER BY completed_at DESC 
        LIMIT 1
        """
    )
    suspend fun getLatestRoutineScreeningResult(): ScreeningResultEntity?

    @Query(
        """
        SELECT * FROM screening_results 
        WHERE period_start_date >= :startDate AND period_end_date <= :endDate 
        ORDER BY completed_at DESC
        """
    )
    suspend fun getScreeningResultsBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ScreeningResultEntity>
}
