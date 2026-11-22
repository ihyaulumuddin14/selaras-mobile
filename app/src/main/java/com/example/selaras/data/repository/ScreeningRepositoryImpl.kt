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
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists result + 9 answers in one Room transaction, with a per-type weekly quota. */
class ScreeningRepositoryImpl(
    private val database: SelarasDatabase,
    private val screeningResultDao: ScreeningResultDao = database.screeningResultDao(),
    private val screeningAnswerDao: ScreeningAnswerDao = database.screeningAnswerDao()
) : ScreeningRepository {

    override suspend fun getScreeningTypesUsedThisWeek(referenceDate: LocalDate): Set<ScreeningType> {
        val (start, end) = getWeekRange(referenceDate)
        return screeningResultDao.getScreeningTypesBetween(start, end).toSet()
    }

    override suspend fun submitScreening(
        screeningResult: ScreeningResult,
        answers: List<ScreeningAnswer>
    ): Result<Long> {
        return try {
            validateScreening(screeningResult, answers)

            val savedId = database.withTransaction {
                val completedDate = screeningResult.completedAt
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                val (weekStart, weekEnd) = getWeekRange(completedDate)

                // Checking quota inside the transaction prevents two saves from using it twice.
                val used = screeningResultDao.countScreeningResultsByTypeBetween(
                    screeningResult.screeningType,
                    weekStart,
                    weekEnd
                )
                check(used == 0) {
                    "Kuota skrining ${screeningResult.screeningType.value} minggu ini sudah digunakan."
                }

                val resultId = screeningResultDao.insertScreeningResult(screeningResult.toEntity())
                check(resultId > 0L) { "Penyimpanan hasil skrining tidak menghasilkan ID yang valid." }

                screeningAnswerDao.insertAnswers(
                    answers.sortedBy { it.questionNumber }.map { answer ->
                        ScreeningAnswerEntity(
                            id = 0,
                            screeningResultId = resultId,
                            questionNumber = answer.questionNumber,
                            answerValue = answer.answerValue
                        )
                    }
                )
                resultId
            }
            Result.success(savedId)
        } catch (exception: CancellationException) {
            // Room rolls the transaction back on coroutine cancellation.
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    private fun validateScreening(result: ScreeningResult, answers: List<ScreeningAnswer>) {
        require(!result.periodStartDate.isAfter(result.periodEndDate)) {
            "Tanggal mulai tidak boleh melebihi tanggal akhir."
        }
        require(result.instrumentVersion.isNotBlank()) { "Versi instrumen tidak boleh kosong." }
        require(answers.size == QUESTION_COUNT) { "Skrining harus memiliki tepat 9 jawaban." }
        require(answers.map { it.questionNumber }.sorted() == (1..QUESTION_COUNT).toList()) {
            "Pertanyaan 1 sampai 9 harus dijawab masing-masing satu kali."
        }
        require(answers.all { it.answerValue in MIN_ANSWER..MAX_ANSWER }) {
            "Nilai setiap jawaban harus berada pada rentang 0 sampai 3."
        }
        require(answers.sumOf { it.answerValue } == result.totalScore) {
            "Skor total tidak cocok dengan jumlah nilai jawaban."
        }
        require(
            !result.instrumentVersion.startsWith(DEMO_PREFIX, ignoreCase = true) || !result.helpIndicator
        ) { "Indikator krisis klinis tidak boleh diaktifkan untuk instrumen demo." }
    }

    override suspend fun getScreeningResultById(id: Long): ScreeningResult? =
        database.withTransaction {
            val entity = screeningResultDao.getScreeningResultById(id) ?: return@withTransaction null
            entity.toDomain(loadAnswers(id))
        }

    override suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResult? = database.withTransaction {
        val entity = screeningResultDao.getScreeningResultByPeriod(
            screeningType, startDate, endDate
        ) ?: return@withTransaction null
        entity.toDomain(loadAnswers(entity.id))
    }

    override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> =
        screeningResultDao.getAllScreeningResultsFlow().map { rows ->
            rows.map { it.toDomain() }
        }

    override suspend fun getLatestScreeningResult(): ScreeningResult? =
        database.withTransaction {
            val entity = screeningResultDao.getLatestScreeningResult() ?: return@withTransaction null
            entity.toDomain(loadAnswers(entity.id))
        }

    override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> =
        screeningResultDao.getLatestScreeningResultFlow().map { it?.toDomain() }

    override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? =
        database.withTransaction {
            val entity = screeningResultDao.getLatestRoutineScreeningResult()
                ?: return@withTransaction null
            entity.toDomain(loadAnswers(entity.id))
        }

    override suspend fun getAnswersByScreeningResultId(
        screeningResultId: Long
    ): List<ScreeningAnswer> = loadAnswers(screeningResultId)

    override suspend fun countScreeningResultsByTypeBetween(
        screeningType: ScreeningType,
        startInclusive: Instant,
        endExclusive: Instant
    ): Int = screeningResultDao.countScreeningResultsByTypeBetween(
        screeningType, startInclusive, endExclusive
    )

    private suspend fun loadAnswers(resultId: Long): List<ScreeningAnswer> =
        screeningAnswerDao.getAnswersByScreeningResultId(resultId).map { it.toDomain() }

    private fun getWeekRange(referenceDate: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return monday.atStartOfDay(zone).toInstant() to
            monday.plusWeeks(1).atStartOfDay(zone).toInstant()
    }

    private fun ScreeningResult.toEntity() = ScreeningResultEntity(
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

    private fun ScreeningResultEntity.toDomain(
        answers: List<ScreeningAnswer> = emptyList()
    ) = ScreeningResult(
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

    private fun ScreeningAnswerEntity.toDomain() = ScreeningAnswer(
        id = id,
        screeningResultId = screeningResultId,
        questionNumber = questionNumber,
        answerValue = answerValue
    )

    private companion object {
        const val QUESTION_COUNT = 9
        const val MIN_ANSWER = 0
        const val MAX_ANSWER = 3
        const val DEMO_PREFIX = "DEMO-"
    }
}
