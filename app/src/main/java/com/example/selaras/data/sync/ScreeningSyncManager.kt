package com.example.selaras.data.sync

import com.example.selaras.core.database.dao.ScreeningAnswerDao
import com.example.selaras.core.database.dao.ScreeningResultDao
import com.example.selaras.data.datasource.remote.ScreeningRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.ScreeningAnswerNetworkDto
import com.example.selaras.data.datasource.remote.dto.ScreeningNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncScreeningBatchRequestDto
import com.example.selaras.data.local.entity.ScreeningAnswerEntity
import com.example.selaras.data.local.entity.ScreeningResultEntity
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningType
import kotlinx.coroutines.flow.first
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Orchestrates offline-first synchronization for PHQ-9 Screening results & 9 answers.
 * Single Source of Truth (SSoT) remains the local Room database.
 * Resolves conflicts using Last-Write-Wins (LWW) based on completed_at / created_at.
 */
class ScreeningSyncManager(
    private val screeningResultDao: ScreeningResultDao,
    private val screeningAnswerDao: ScreeningAnswerDao,
    private val remoteDataSource: ScreeningRemoteDataSource
) {

    suspend fun sync(lastSyncedAt: Instant = Instant.EPOCH): SyncResult {
        var pushed = 0
        var pulled = 0

        // ==========================================
        // 1. PUSH PHASE: Local -> Remote
        // ==========================================
        val allLocalScreenings = screeningResultDao.getAllScreeningResultsFlow().first()
        if (allLocalScreenings.isNotEmpty()) {
            val dtoList = allLocalScreenings.map { entity ->
                val answers = screeningAnswerDao.getAnswersByScreeningResultId(entity.id)
                entity.toNetworkDto(answers)
            }

            val pushResult = remoteDataSource.pushBatch(
                SyncScreeningBatchRequestDto(items = dtoList)
            )

            if (pushResult.isFailure) {
                return SyncResult(
                    isSuccess = false,
                    errorMessage = pushResult.exceptionOrNull()?.message ?: "Gagal sinkronisasi push skrining PHQ-9"
                )
            }

            pushed = pushResult.getOrThrow().syncedUuids.size
        }

        // ==========================================
        // 2. PULL PHASE: Remote -> Local (Smart Merge)
        // ==========================================
        val pullResult = remoteDataSource.fetchChangesSince(lastSyncedAt)
        if (pullResult.isFailure) {
            return SyncResult(
                pushedCount = pushed,
                isSuccess = false,
                errorMessage = pullResult.exceptionOrNull()?.message ?: "Gagal mengambil data skrining dari server"
            )
        }

        val remoteScreenings = pullResult.getOrThrow()
        for (remoteItem in remoteScreenings) {
            val type = runCatching { ScreeningType.valueOf(remoteItem.screeningType) }.getOrDefault(ScreeningType.MANUAL)
            val startDate = LocalDate.parse(remoteItem.periodStartDate)
            val endDate = LocalDate.parse(remoteItem.periodEndDate)

            val existing = screeningResultDao.getScreeningResultByPeriod(type, startDate, endDate)

            val completedAt = runCatching { Instant.parse(remoteItem.completedAt) }.getOrDefault(Instant.now())
            val createdAt = runCatching { Instant.parse(remoteItem.createdAt) }.getOrDefault(Instant.now())
            val category = runCatching { ScreeningCategory.valueOf(remoteItem.category) }.getOrDefault(ScreeningCategory.MINIMAL)

            if (existing != null) {
                // Last-Write-Wins comparison
                if (completedAt.isAfter(existing.completedAt)) {
                    val updated = existing.copy(
                        totalScore = remoteItem.totalScore,
                        category = category,
                        helpIndicator = remoteItem.helpIndicator,
                        completedAt = completedAt,
                        instrumentVersion = remoteItem.instrumentVersion
                    )
                    screeningResultDao.updateScreeningResult(updated)

                    // Recreate answers
                    screeningAnswerDao.deleteAnswersByScreeningResultId(existing.id)
                    val newAnswers = remoteItem.answers.map {
                        ScreeningAnswerEntity(
                            id = 0L,
                            screeningResultId = existing.id,
                            questionNumber = it.questionNumber,
                            answerValue = it.answerValue
                        )
                    }
                    screeningAnswerDao.insertAnswers(newAnswers)
                    pulled++
                }
            } else {
                // Insert new screening result into Room
                val newEntity = ScreeningResultEntity(
                    id = 0L,
                    screeningType = type,
                    periodStartDate = startDate,
                    periodEndDate = endDate,
                    instrumentVersion = remoteItem.instrumentVersion,
                    completedAt = completedAt,
                    totalScore = remoteItem.totalScore,
                    category = category,
                    helpIndicator = remoteItem.helpIndicator,
                    createdAt = createdAt
                )
                val newId = screeningResultDao.insertScreeningResult(newEntity)
                val answers = remoteItem.answers.map {
                    ScreeningAnswerEntity(
                        id = 0L,
                        screeningResultId = newId,
                        questionNumber = it.questionNumber,
                        answerValue = it.answerValue
                    )
                }
                screeningAnswerDao.insertAnswers(answers)
                pulled++
            }
        }

        return SyncResult(
            pushedCount = pushed,
            pulledCount = pulled,
            isSuccess = true
        )
    }

    private fun ScreeningResultEntity.toNetworkDto(answers: List<ScreeningAnswerEntity>): ScreeningNetworkDto {
        // Deterministic UUID based on screening key for idempotent sync
        val uuidKey = "${screeningType}_${periodStartDate}_${periodEndDate}"
        val uuid = UUID.nameUUIDFromBytes(uuidKey.toByteArray(StandardCharsets.UTF_8)).toString()

        return ScreeningNetworkDto(
            uuid = uuid,
            screeningType = screeningType.name,
            periodStartDate = periodStartDate.toString(),
            periodEndDate = periodEndDate.toString(),
            instrumentVersion = instrumentVersion,
            totalScore = totalScore,
            category = category.name,
            helpIndicator = helpIndicator,
            completedAt = completedAt.toString(),
            isDeleted = false,
            createdAt = createdAt.toString(),
            updatedAt = completedAt.toString(),
            answers = answers.map {
                ScreeningAnswerNetworkDto(
                    questionNumber = it.questionNumber,
                    answerValue = it.answerValue
                )
            }
        )
    }
}
