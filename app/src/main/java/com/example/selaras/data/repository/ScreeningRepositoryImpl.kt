package com.example.selaras.data.repository

import androidx.room.withTransaction
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.core.database.dao.ScreeningAnswerDao
import com.example.selaras.core.database.dao.ScreeningResultDao
import com.example.selaras.data.local.entity.ScreeningAnswerEntity
import com.example.selaras.data.local.entity.ScreeningResultEntity
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class ScreeningRepositoryImpl(
    private val database: SelarasDatabase,
    private val screeningResultDao: ScreeningResultDao = database.screeningResultDao(),
    private val screeningAnswerDao: ScreeningAnswerDao = database.screeningAnswerDao()
) : ScreeningRepository {

    override suspend fun submitScreening(
        screeningResult: ScreeningResult,
        answers: List<ScreeningAnswer>
    ): Result<Long> = runCatching {
        require(answers.size == 9) { "PHQ-9 screening must have exactly 9 answers" }

        // Atomic transaction: both result and all 9 answers must succeed together
        database.withTransaction {
            val resultEntity = screeningResult.toEntity()
            val generatedResultId = screeningResultDao.insertScreeningResult(resultEntity)

            val answerEntities = answers.map { answer ->
                ScreeningAnswerEntity(
                    id = 0,
                    screeningResultId = generatedResultId,
                    questionNumber = answer.questionNumber,
                    answerValue = answer.answerValue
                )
            }
            screeningAnswerDao.insertAnswers(answerEntities)

            generatedResultId
        }
    }

    override suspend fun getScreeningResultById(id: Long): ScreeningResult? {
        val result = screeningResultDao.getScreeningResultById(id) ?: return null
        val answers = screeningAnswerDao.getAnswersByScreeningResultId(id).map { it.toDomain() }
        return result.toDomain(answers)
    }

    override suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResult? {
        val result = screeningResultDao.getScreeningResultByPeriod(screeningType, startDate, endDate) ?: return null
        val answers = screeningAnswerDao.getAnswersByScreeningResultId(result.id).map { it.toDomain() }
        return result.toDomain(answers)
    }

    override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> {
        return screeningResultDao.getAllScreeningResultsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getLatestScreeningResult(): ScreeningResult? {
        val result = screeningResultDao.getLatestScreeningResult() ?: return null
        val answers = screeningAnswerDao.getAnswersByScreeningResultId(result.id).map { it.toDomain() }
        return result.toDomain(answers)
    }

    override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> {
        return screeningResultDao.getLatestScreeningResultFlow().map { it?.toDomain() }
    }

    override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? {
        val result = screeningResultDao.getLatestRoutineScreeningResult() ?: return null
        val answers = screeningAnswerDao.getAnswersByScreeningResultId(result.id).map { it.toDomain() }
        return result.toDomain(answers)
    }

    override suspend fun getAnswersByScreeningResultId(screeningResultId: Long): List<ScreeningAnswer> {
        return screeningAnswerDao.getAnswersByScreeningResultId(screeningResultId).map { it.toDomain() }
    }

    private fun ScreeningResult.toEntity(): ScreeningResultEntity {
        return ScreeningResultEntity(
            id = id,
            screeningType = screeningType,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            instrumentVersion = instrumentVersion,
            completedAt = completedAt,
            totalScore = totalScore,
            category = category,
            helpIndicator = helpIndicator,
            createdAt = createdAt
        )
    }

    private fun ScreeningResultEntity.toDomain(answers: List<ScreeningAnswer> = emptyList()): ScreeningResult {
        return ScreeningResult(
            id = id,
            screeningType = screeningType,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            instrumentVersion = instrumentVersion,
            completedAt = completedAt,
            totalScore = totalScore,
            category = category,
            helpIndicator = helpIndicator,
            createdAt = createdAt,
            answers = answers
        )
    }

    private fun ScreeningAnswerEntity.toDomain(): ScreeningAnswer {
        return ScreeningAnswer(
            id = id,
            screeningResultId = screeningResultId,
            questionNumber = questionNumber,
            answerValue = answerValue
        )
    }
}
