package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.LocalDate
import java.util.concurrent.CancellationException

class SubmitScreeningUseCase(
    private val screeningRepository: ScreeningRepository,
    private val calculatePhq9ScoreUseCase: CalculatePhq9ScoreUseCase,
    private val classifyPhq9ScoreUseCase: ClassifyPhq9ScoreUseCase,
    private val checkCrisisIndicatorUseCase: CheckCrisisIndicatorUseCase
) {

    suspend operator fun invoke(
        screeningType: ScreeningType,
        periodStartDate: LocalDate,
        periodEndDate: LocalDate,
        instrumentVersion: String,
        answers: List<ScreeningAnswer>
    ): Result<Long> {
        return try {
            require(!periodStartDate.isAfter(periodEndDate)) {
                "Screening start date cannot be after end date"
            }
            require(instrumentVersion.isNotBlank()) {
                "Instrument version cannot be empty"
            }
            require(answers.size == QUESTION_COUNT) {
                "Exactly 9 answers are required"
            }

            val sortedAnswers = answers.sortedBy { it.questionNumber }
            require(sortedAnswers.map { it.questionNumber } == (1..QUESTION_COUNT).toList()) {
                "Questions 1 through 9 must be answered exactly once"
            }
            require(sortedAnswers.all { it.answerValue in MIN_ANSWER..MAX_ANSWER }) {
                "Answer values must be between 0 and 3"
            }

            val totalScore = calculatePhq9ScoreUseCase(sortedAnswers)
            val category = classifyPhq9ScoreUseCase(totalScore)
            val isDemoInstrument = instrumentVersion.startsWith(DEMO_INSTRUMENT_PREFIX, ignoreCase = true)
            val helpIndicator = if (isDemoInstrument) {
                false
            } else {
                checkCrisisIndicatorUseCase(
                    answers = sortedAnswers,
                    totalScore = totalScore
                )
            }

            val screeningResult = ScreeningResult(
                screeningType = screeningType,
                periodStartDate = periodStartDate,
                periodEndDate = periodEndDate,
                instrumentVersion = instrumentVersion,
                totalScore = totalScore,
                category = category,
                helpIndicator = helpIndicator,
                answers = sortedAnswers
            )

            screeningRepository.submitScreening(
                screeningResult = screeningResult,
                answers = sortedAnswers
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Result.failure(exception)
        }
    }

    companion object {
        private const val QUESTION_COUNT = 9
        private const val MIN_ANSWER = 0
        private const val MAX_ANSWER = 3
        private const val DEMO_INSTRUMENT_PREFIX = "DEMO-"
    }
}
