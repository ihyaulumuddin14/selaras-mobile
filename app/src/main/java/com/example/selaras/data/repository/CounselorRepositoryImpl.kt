package com.example.selaras.data.repository

import com.example.selaras.core.database.dao.CounselorSummaryDao
import com.example.selaras.data.local.entity.CounselorSummaryEntity
import com.example.selaras.domain.model.CounselorSummary
import com.example.selaras.domain.repository.CounselorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CounselorRepositoryImpl(
    private val counselorSummaryDao: CounselorSummaryDao
) : CounselorRepository {

    override suspend fun createSummary(summary: CounselorSummary): Result<CounselorSummary> =
        runCatching {
            val entity = summary.toEntity()
            val generatedId = counselorSummaryDao.insertSummary(entity)
            summary.copy(id = generatedId)
        }

    override suspend fun getSummaryById(id: Long): CounselorSummary? {
        return counselorSummaryDao.getSummaryById(id)?.toDomain()
    }

    override suspend fun getSummaryByScreeningResultId(screeningResultId: Long): CounselorSummary? {
        return counselorSummaryDao.getSummaryByScreeningResultId(screeningResultId)?.toDomain()
    }

    override fun getAllSummariesFlow(): Flow<List<CounselorSummary>> {
        return counselorSummaryDao.getAllSummariesFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun deleteSummary(id: Long): Result<Unit> = runCatching {
        counselorSummaryDao.deleteSummaryById(id)
    }

    private fun CounselorSummary.toEntity(): CounselorSummaryEntity {
        return CounselorSummaryEntity(
            id = id,
            screeningResultId = screeningResultId,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            includeCheckinAnalysis = includeCheckinAnalysis,
            checkinDaysCount = checkinDaysCount,
            ruleVersion = ruleVersion,
            snapshotJson = snapshotJson,
            createdAt = createdAt
        )
    }

    private fun CounselorSummaryEntity.toDomain(): CounselorSummary {
        return CounselorSummary(
            id = id,
            screeningResultId = screeningResultId,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            includeCheckinAnalysis = includeCheckinAnalysis,
            checkinDaysCount = checkinDaysCount,
            ruleVersion = ruleVersion,
            snapshotJson = snapshotJson,
            createdAt = createdAt
        )
    }
}
