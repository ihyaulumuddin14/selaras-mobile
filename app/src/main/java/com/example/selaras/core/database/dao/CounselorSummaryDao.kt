package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.CounselorSummaryEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CounselorSummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: CounselorSummaryEntity): Long

    @Update
    suspend fun updateSummary(summary: CounselorSummaryEntity)

    @Delete
    suspend fun deleteSummary(summary: CounselorSummaryEntity)

    @Query("DELETE FROM counselor_summaries WHERE id = :id")
    suspend fun deleteSummaryById(id: Long)

    @Query("SELECT * FROM counselor_summaries WHERE id = :id LIMIT 1")
    suspend fun getSummaryById(id: Long): CounselorSummaryEntity?

    @Query("SELECT * FROM counselor_summaries WHERE screening_result_id = :screeningResultId LIMIT 1")
    suspend fun getSummaryByScreeningResultId(screeningResultId: Long): CounselorSummaryEntity?

    @Query("SELECT * FROM counselor_summaries WHERE screening_result_id = :screeningResultId LIMIT 1")
    fun getSummaryByScreeningResultIdFlow(screeningResultId: Long): Flow<CounselorSummaryEntity?>

    @Query("SELECT * FROM counselor_summaries ORDER BY created_at DESC")
    fun getAllSummariesFlow(): Flow<List<CounselorSummaryEntity>>

    @Query(
        """
        SELECT * FROM counselor_summaries 
        WHERE period_start_date >= :startDate AND period_end_date <= :endDate 
        ORDER BY created_at DESC
        """
    )
    suspend fun getSummariesBetween(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<CounselorSummaryEntity>
}
