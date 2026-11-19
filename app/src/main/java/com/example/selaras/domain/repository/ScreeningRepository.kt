package com.example.selaras.domain.repository

import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ScreeningRepository {
    suspend fun submitScreening(
        screeningResult: ScreeningResult,
        answers: List<ScreeningAnswer>
    ): Result<Long>

    suspend fun getScreeningResultById(id: Long): ScreeningResult?
    suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResult?
    fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>>
    suspend fun getLatestScreeningResult(): ScreeningResult?
    fun getLatestScreeningResultFlow(): Flow<ScreeningResult?>
    suspend fun getLatestRoutineScreeningResult(): ScreeningResult?
    suspend fun getAnswersByScreeningResultId(screeningResultId: Long): List<ScreeningAnswer>
}
