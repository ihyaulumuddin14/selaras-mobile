package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.selaras.data.local.entity.ScreeningAnswerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScreeningAnswerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnswer(answer: ScreeningAnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAnswers(answers: List<ScreeningAnswerEntity>): List<Long>

    @Query(
        """
        SELECT * FROM screening_answers 
        WHERE screening_result_id = :screeningResultId 
        ORDER BY question_number ASC
        """
    )
    suspend fun getAnswersByScreeningResultId(screeningResultId: Long): List<ScreeningAnswerEntity>

    @Query(
        """
        SELECT * FROM screening_answers 
        WHERE screening_result_id = :screeningResultId 
        ORDER BY question_number ASC
        """
    )
    fun getAnswersByScreeningResultIdFlow(screeningResultId: Long): Flow<List<ScreeningAnswerEntity>>

    @Query("DELETE FROM screening_answers WHERE screening_result_id = :screeningResultId")
    suspend fun deleteAnswersByScreeningResultId(screeningResultId: Long)
}
