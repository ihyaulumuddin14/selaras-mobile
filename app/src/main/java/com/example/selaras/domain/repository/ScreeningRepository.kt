package com.example.selaras.domain.repository

import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/**
 * Local-first source of screening results.
 *
 * The weekly limit is one MANUAL and one ROUTINE submission per local Monday-Sunday
 * week. A failed transaction must not consume either quota.
 */
interface ScreeningRepository {

    suspend fun getScreeningTypesUsedThisWeek(referenceDate: LocalDate): Set<ScreeningType>

    /** Atomically validates and saves one result and all nine answers. */
    suspend fun submitScreening(
        screeningResult: ScreeningResult,
        answers: List<ScreeningAnswer>
    ): Result<Long>

    /** Returns a result with all of its answers; null if its ID does not exist. */
    suspend fun getScreeningResultById(id: Long): ScreeningResult?

    suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResult?

    /** Lightweight history: fetch answers with getScreeningResultById when opening a detail. */
    fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>>

    suspend fun getLatestScreeningResult(): ScreeningResult?

    /** Emits metadata-only results; use getScreeningResultById for the full answers. */
    fun getLatestScreeningResultFlow(): Flow<ScreeningResult?>

    suspend fun getLatestRoutineScreeningResult(): ScreeningResult?

    suspend fun getAnswersByScreeningResultId(screeningResultId: Long): List<ScreeningAnswer>

    /** Time window is [startInclusive, endExclusive) in local-week Instant boundaries. */
    suspend fun countScreeningResultsByTypeBetween(
        screeningType: ScreeningType,
        startInclusive: Instant,
        endExclusive: Instant
    ): Int
}
