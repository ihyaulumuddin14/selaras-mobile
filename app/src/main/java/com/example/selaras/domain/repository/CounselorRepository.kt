package com.example.selaras.domain.repository

import com.example.selaras.domain.model.CounselorSummary
import kotlinx.coroutines.flow.Flow

interface CounselorRepository {
    suspend fun createSummary(summary: CounselorSummary): Result<CounselorSummary>
    suspend fun getSummaryById(id: Long): CounselorSummary?
    suspend fun getSummaryByScreeningResultId(screeningResultId: Long): CounselorSummary?
    fun getAllSummariesFlow(): Flow<List<CounselorSummary>>
    suspend fun deleteSummary(id: Long): Result<Unit>
}
