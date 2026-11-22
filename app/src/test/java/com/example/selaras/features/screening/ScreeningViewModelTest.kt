package com.example.selaras.features.screening

import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.screening.CalculatePhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.CheckCrisisIndicatorUseCase
import com.example.selaras.domain.usecase.screening.ClassifyPhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.GetScreeningQuotaUseCase
import com.example.selaras.domain.usecase.screening.SubmitScreeningUseCase
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreeningViewModelTest {

    @Test
    fun initialStateLoadsBothWeeklyQuotas() = runTest {
        withViewModel { viewModel, _ ->
            val quotaState = viewModel.quotaUiState.value
            assertFalse(quotaState.isLoading)
            assertNull(quotaState.errorMessage)
            assertEquals(1, quotaState.quota?.manualRemaining)
            assertEquals(1, quotaState.quota?.routineRemaining)
        }
    }

    @Test
    fun startScreeningMovesToIntroductionWhenQuotaIsAvailable() = runTest {
        withViewModel { viewModel, _ ->
            viewModel.startScreening(isRoutine = false)
            assertEquals(ScreeningStep.INTRODUCTION, viewModel.uiState.value.step)
            assertEquals(ScreeningType.MANUAL, viewModel.uiState.value.selectedType)
        }
    }

    @Test
    fun startScreeningDoesNotStartWhenSelectedQuotaHasBeenUsed() = runTest {
        val repository = FakeScreeningRepository()
        repository.seedResult(ScreeningType.ROUTINE)

        withViewModel(repository) { viewModel, _ ->
            viewModel.startScreening(isRoutine = true)
            assertEquals(ScreeningStep.SELECTION, viewModel.uiState.value.step)
            assertFalse(viewModel.quotaUiState.value.quota!!.routineAvailable)
            assertTrue(viewModel.quotaUiState.value.quota!!.manualAvailable)
        }
    }

    @Test
    fun answerSelectionAndQuestionNavigationAreValidated() = runTest {
        withViewModel { viewModel, _ ->
            startQuestions(viewModel)

            viewModel.selectAnswer(questionNumber = 2, answerValue = 3)
            assertNull(viewModel.uiState.value.currentAnswer)

            viewModel.selectAnswer(questionNumber = 1, answerValue = 2)
            assertEquals(2, viewModel.uiState.value.currentAnswer)

            viewModel.selectAnswer(questionNumber = 1, answerValue = 4)
            assertEquals(2, viewModel.uiState.value.currentAnswer)

            viewModel.nextQuestion()
            assertEquals(2, viewModel.uiState.value.currentQuestionNumber)

            viewModel.selectAnswer(questionNumber = 2, answerValue = 3)
            assertEquals(3, viewModel.uiState.value.currentAnswer)

            viewModel.previousQuestion()
            assertEquals(1, viewModel.uiState.value.currentQuestionNumber)
            assertEquals(2, viewModel.uiState.value.currentAnswer)
        }
    }

    @Test
    fun nextQuestionCannotAdvanceWithoutCurrentAnswer() = runTest {
        withViewModel { viewModel, _ ->
            startQuestions(viewModel)
            viewModel.nextQuestion()
            assertEquals(1, viewModel.uiState.value.currentQuestionNumber)
        }
    }

    @Test
    fun submittingAnswersTriggersCrisisWhenIndicatedAndCanContinueToResult() = runTest {
        val repository = FakeScreeningRepository()

        withViewModel(repository) { viewModel, fakeRepository ->
            answerAllQuestions(viewModel, values = List(9) { 3 })
            viewModel.submitScreening()
            advanceUntilIdle()

            val crisisState = viewModel.uiState.value
            assertEquals(ScreeningStep.CRISIS, crisisState.step)
            assertNotNull(crisisState.result)
            assertEquals(27, crisisState.result?.totalScore)
            assertEquals(ScreeningViewModel.DEFAULT_INSTRUMENT_VERSION, crisisState.result?.instrumentVersion)
            assertTrue(crisisState.result!!.helpIndicator)
            assertEquals(1, fakeRepository.savedResults.size)
            assertEquals(9, fakeRepository.savedAnswers[crisisState.result!!.id]?.size)

            viewModel.continueToResult()
            val resultState = viewModel.uiState.value
            assertEquals(ScreeningStep.RESULT, resultState.step)

            val quota = viewModel.quotaUiState.value.quota
            assertTrue(quota!!.manualUsed)
            assertFalse(quota.manualAvailable)
            assertTrue(quota.routineAvailable)
            assertEquals(1, quota.routineRemaining)
        }
    }

    @Test
    fun submittingAnswersWithoutCrisisTransitionsDirectlyToResult() = runTest {
        val repository = FakeScreeningRepository()

        withViewModel(repository) { viewModel, fakeRepository ->
            answerAllQuestions(viewModel, values = listOf(1, 1, 1, 1, 1, 1, 1, 1, 0))
            viewModel.submitScreening()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(ScreeningStep.RESULT, state.step)
            assertNotNull(state.result)
            assertEquals(8, state.result?.totalScore)
            assertEquals(ScreeningViewModel.DEFAULT_INSTRUMENT_VERSION, state.result?.instrumentVersion)
            assertFalse(state.result!!.helpIndicator)
            assertEquals(1, fakeRepository.savedResults.size)
            assertEquals(9, fakeRepository.savedAnswers[state.result!!.id]?.size)
        }
    }

    @Test
    fun failedSubmissionKeepsAnswersAndDoesNotConsumeQuota() = runTest {
        val repository = FakeScreeningRepository().apply {
            failNextSubmission = true
        }

        withViewModel(repository) { viewModel, fakeRepository ->
            answerAllQuestions(viewModel)
            val answersBeforeSubmit = viewModel.uiState.value.answers

            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.ERROR, viewModel.uiState.value.step)
            assertEquals(answersBeforeSubmit, viewModel.uiState.value.answers)
            assertTrue(viewModel.quotaUiState.value.quota!!.manualAvailable)
            assertTrue(fakeRepository.savedResults.isEmpty())

            fakeRepository.failNextSubmission = false
            viewModel.returnToQuestions()
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)
            assertEquals(1, fakeRepository.savedResults.size)
        }
    }

    @Test
    fun repeatedSubmitWhileProcessingDoesNotCreateDuplicateResults() = runTest {
        val repository = FakeScreeningRepository()

        withViewModel(repository) { viewModel, fakeRepository ->
            answerAllQuestions(viewModel)
            viewModel.submitScreening()
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(1, fakeRepository.savedResults.size)
            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)
        }
    }

    @Test
    fun retryAfterSavedResultCannotBeReadDoesNotSubmitTwice() = runTest {
        val repository = FakeScreeningRepository().apply {
            failNextResultRead = true
        }

        withViewModel(repository) { viewModel, fakeRepository ->
            answerAllQuestions(viewModel)
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.ERROR, viewModel.uiState.value.step)
            assertEquals(1, fakeRepository.savedResults.size)

            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)
            assertEquals(1, fakeRepository.savedResults.size)
        }
    }

    @Test
    fun manualAndRoutineHaveIndependentWeeklyQuotas() = runTest {
        withViewModel { viewModel, repository ->
            answerAllQuestions(viewModel, isRoutine = false)
            viewModel.submitScreening()
            advanceUntilIdle()
            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)

            viewModel.resetScreening()
            advanceUntilIdle()

            answerAllQuestions(viewModel, isRoutine = true)
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)
            assertEquals(2, repository.savedResults.size)
            assertEquals(
                setOf(ScreeningType.MANUAL, ScreeningType.ROUTINE),
                repository.savedResults.map { it.screeningType }.toSet()
            )
            val quota = viewModel.quotaUiState.value.quota!!
            assertTrue(quota.manualUsed)
            assertTrue(quota.routineUsed)
            assertEquals(0, quota.manualRemaining)
            assertEquals(0, quota.routineRemaining)
        }
    }

    @Test
    fun editedAnswerIsUsedForSubmittedScore() = runTest {
        withViewModel { viewModel, repository ->
            startQuestions(viewModel)
            viewModel.selectAnswer(questionNumber = 1, answerValue = 3)
            viewModel.selectAnswer(questionNumber = 1, answerValue = 0)
            viewModel.nextQuestion()

            (2..8).forEach { number ->
                viewModel.selectAnswer(questionNumber = number, answerValue = 1)
                viewModel.nextQuestion()
            }
            viewModel.selectAnswer(questionNumber = 9, answerValue = 0)
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.RESULT, viewModel.uiState.value.step)
            assertEquals(7, viewModel.uiState.value.result?.totalScore)
            assertEquals(0, repository.savedAnswers.values.single().first { it.questionNumber == 1 }.answerValue)
            assertEquals(0, repository.savedAnswers.values.single().first { it.questionNumber == 9 }.answerValue)
        }
    }

    @Test
    fun resetWhileSubmissionIsQueuedDoesNotCreateRecord() = runTest {
        withViewModel { viewModel, repository ->
            answerAllQuestions(viewModel)
            viewModel.submitScreening()
            // StandardTestDispatcher has not executed the queued submit coroutine yet.
            viewModel.resetScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.SELECTION, viewModel.uiState.value.step)
            assertTrue(repository.savedResults.isEmpty())
            assertTrue(viewModel.quotaUiState.value.quota!!.manualAvailable)
        }
    }

    @Test
    fun incompleteScreeningCannotSubmitOrConsumeQuota() = runTest {
        withViewModel { viewModel, repository ->
            startQuestions(viewModel)
            viewModel.selectAnswer(questionNumber = 1, answerValue = 2)
            viewModel.submitScreening()
            advanceUntilIdle()

            assertEquals(ScreeningStep.QUESTIONS, viewModel.uiState.value.step)
            assertTrue(repository.savedResults.isEmpty())
            assertTrue(viewModel.quotaUiState.value.quota!!.manualAvailable)
        }
    }

    private suspend fun TestScope.withViewModel(
        repository: FakeScreeningRepository = FakeScreeningRepository(),
        block: suspend (ScreeningViewModel, FakeScreeningRepository) -> Unit
    ) {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val submitUseCase = SubmitScreeningUseCase(
                screeningRepository = repository,
                calculatePhq9ScoreUseCase = CalculatePhq9ScoreUseCase(),
                classifyPhq9ScoreUseCase = ClassifyPhq9ScoreUseCase(),
                checkCrisisIndicatorUseCase = CheckCrisisIndicatorUseCase()
            )
            val viewModel = ScreeningViewModel(
                submitScreeningUseCase = submitUseCase,
                screeningRepository = repository,
                getScreeningQuotaUseCase = GetScreeningQuotaUseCase(repository)
            )
            advanceUntilIdle()
            block(viewModel, repository)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun startQuestions(viewModel: ScreeningViewModel, isRoutine: Boolean = false) {
        viewModel.startScreening(isRoutine = isRoutine)
        viewModel.beginQuestions()
    }

    private fun answerAllQuestions(
        viewModel: ScreeningViewModel,
        values: List<Int> = listOf(1, 1, 1, 1, 1, 1, 1, 1, 0),
        isRoutine: Boolean = false
    ) {
        startQuestions(viewModel, isRoutine)
        values.forEachIndexed { index, value ->
            viewModel.selectAnswer(index + 1, value)
            if (index < values.lastIndex) viewModel.nextQuestion()
        }
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    private class FakeScreeningRepository : ScreeningRepository {
        val savedResults = mutableListOf<ScreeningResult>()
        val savedAnswers = mutableMapOf<Long, List<ScreeningAnswer>>()
        var failNextSubmission: Boolean = false
        var failNextResultRead: Boolean = false
        private var nextId = 1L
        private val resultsFlow = MutableStateFlow<List<ScreeningResult>>(emptyList())
        private val latestResultFlow = MutableStateFlow<ScreeningResult?>(null)

        fun seedResult(type: ScreeningType) {
            val result = createResult(
                id = nextId++,
                type = type,
                completedAt = Instant.now()
            )
            savedResults += result
            savedAnswers[result.id] = emptyList()
            publishResults()
        }

        override suspend fun getScreeningTypesUsedThisWeek(
            referenceDate: LocalDate
        ): Set<ScreeningType> {
            val weekStart = referenceDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            )
            val weekEnd = weekStart.plusDays(6)
            return savedResults.filter { result ->
                val completedDate = result.completedAt
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                completedDate in weekStart..weekEnd
            }.map { it.screeningType }.toSet()
        }

        override suspend fun submitScreening(
            screeningResult: ScreeningResult,
            answers: List<ScreeningAnswer>
        ): Result<Long> {
            if (failNextSubmission) {
                return Result.failure(IllegalStateException("Simulasi penyimpanan gagal"))
            }

            val start = screeningResult.periodEndDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            ).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = screeningResult.periodEndDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
            ).plusWeeks(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            if (countScreeningResultsByTypeBetween(screeningResult.screeningType, start, end) > 0) {
                return Result.failure(IllegalStateException("Kuota sudah digunakan"))
            }

            val id = nextId++
            val saved = screeningResult.copy(id = id, answers = answers)
            savedResults += saved
            savedAnswers[id] = answers
            publishResults()
            return Result.success(id)
        }

        override suspend fun getScreeningResultById(id: Long): ScreeningResult? {
            if (failNextResultRead) {
                failNextResultRead = false
                return null
            }
            return savedResults.firstOrNull { it.id == id }?.let { result ->
                result.copy(answers = savedAnswers[id].orEmpty())
            }
        }

        override suspend fun getScreeningResultByPeriod(
            screeningType: ScreeningType,
            startDate: LocalDate,
            endDate: LocalDate
        ): ScreeningResult? = savedResults.firstOrNull {
            it.screeningType == screeningType &&
                it.periodStartDate == startDate &&
                it.periodEndDate == endDate
        }

        override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> =
            resultsFlow.asStateFlow()

        override suspend fun getLatestScreeningResult(): ScreeningResult? = savedResults.maxByOrNull { it.completedAt }

        override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> =
            latestResultFlow.asStateFlow()

        override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? =
            savedResults.filter { it.screeningType == ScreeningType.ROUTINE }
                .maxByOrNull { it.completedAt }

        override suspend fun getAnswersByScreeningResultId(
            screeningResultId: Long
        ): List<ScreeningAnswer> = savedAnswers[screeningResultId].orEmpty()

        override suspend fun countScreeningResultsByTypeBetween(
            screeningType: ScreeningType,
            startInclusive: Instant,
            endExclusive: Instant
        ): Int = savedResults.count {
            it.screeningType == screeningType &&
                !it.completedAt.isBefore(startInclusive) &&
                it.completedAt.isBefore(endExclusive)
        }

        private fun publishResults() {
            resultsFlow.value = savedResults.toList()
            latestResultFlow.value = savedResults.maxByOrNull { it.completedAt }
        }

        private fun createResult(
            id: Long,
            type: ScreeningType,
            completedAt: Instant
        ) = ScreeningResult(
            id = id,
            screeningType = type,
            periodStartDate = LocalDate.now().minusDays(13),
            periodEndDate = LocalDate.now(),
            instrumentVersion = ScreeningViewModel.DEFAULT_INSTRUMENT_VERSION,
            completedAt = completedAt,
            totalScore = 0,
            category = ScreeningCategory.MINIMAL,
            helpIndicator = false
        )
    }
}
