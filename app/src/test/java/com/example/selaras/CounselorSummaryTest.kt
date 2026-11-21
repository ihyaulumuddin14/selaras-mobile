@file:Suppress("NewApi")

package com.example.selaras

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.CounselorSummary
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.CounselorRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.counselor.CreateCounselorSummaryUseCase
import com.example.selaras.domain.usecase.counselor.GetCounselorEligibilityUseCase
import com.example.selaras.features.counselor.CounselorScreenState
import com.example.selaras.features.counselor.CounselorSummaryViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class CounselorSummaryTest {

    private lateinit var fakeCheckInRepository: FakeCheckInRepo
    private lateinit var fakeScreeningRepository: FakeScreeningRepo
    private lateinit var fakeCounselorRepository: FakeCounselorRepo

    private lateinit var getEligibilityUseCase: GetCounselorEligibilityUseCase
    private lateinit var createSummaryUseCase: CreateCounselorSummaryUseCase

    @Before
    fun setup() {
        fakeCheckInRepository = FakeCheckInRepo()
        fakeScreeningRepository = FakeScreeningRepo()
        fakeCounselorRepository = FakeCounselorRepo()

        getEligibilityUseCase = GetCounselorEligibilityUseCase(fakeCheckInRepository)
        createSummaryUseCase = CreateCounselorSummaryUseCase(
            fakeCheckInRepository,
            fakeScreeningRepository,
            fakeCounselorRepository
        )
    }

    @Test
    fun `eligibility returns true when 4 or more check-ins recorded`() = runBlocking {
        val startDate = LocalDate.of(2026, 10, 7)
        val endDate = LocalDate.of(2026, 10, 13)

        // Seed 4 days: 7, 9, 11, 13
        fakeCheckInRepository.checkIns.addAll(
            listOf(
                CheckIn(id = 1, checkInDate = LocalDate.of(2026, 10, 7), mood = 2, energy = 2, sleepDurationMinutes = 360),
                CheckIn(id = 2, checkInDate = LocalDate.of(2026, 10, 9), mood = 3, energy = 3, sleepDurationMinutes = 420),
                CheckIn(id = 3, checkInDate = LocalDate.of(2026, 10, 11), mood = 2, energy = 3, sleepDurationMinutes = 360),
                CheckIn(id = 4, checkInDate = LocalDate.of(2026, 10, 13), mood = 1, energy = 2, sleepDurationMinutes = 300)
            )
        )

        val eligibility = getEligibilityUseCase(startDate, endDate)
        assertTrue(eligibility.isEligible)
        assertEquals(4, eligibility.recordedDays)
        assertEquals(0, eligibility.daysNeeded)
        assertEquals(3, eligibility.missingDates.size)
    }

    @Test
    fun `eligibility returns false when less than 4 check-ins recorded`() = runBlocking {
        val startDate = LocalDate.of(2026, 10, 7)
        val endDate = LocalDate.of(2026, 10, 13)

        // Seed only 3 days: 7, 9, 11
        fakeCheckInRepository.checkIns.addAll(
            listOf(
                CheckIn(id = 1, checkInDate = LocalDate.of(2026, 10, 7), mood = 2, energy = 2, sleepDurationMinutes = 360),
                CheckIn(id = 2, checkInDate = LocalDate.of(2026, 10, 9), mood = 3, energy = 3, sleepDurationMinutes = 420),
                CheckIn(id = 3, checkInDate = LocalDate.of(2026, 10, 11), mood = 2, energy = 3, sleepDurationMinutes = 360)
            )
        )

        val eligibility = getEligibilityUseCase(startDate, endDate)
        assertFalse(eligibility.isEligible)
        assertEquals(3, eligibility.recordedDays)
        assertEquals(1, eligibility.daysNeeded)
    }

    @Test
    fun `createSummaryUseCase creates summary successfully when eligible`() = runBlocking {
        val startDate = LocalDate.of(2026, 10, 7)
        val endDate = LocalDate.of(2026, 10, 13)

        fakeCheckInRepository.checkIns.addAll(
            listOf(
                CheckIn(id = 1, checkInDate = LocalDate.of(2026, 10, 7), mood = 2, energy = 2, sleepDurationMinutes = 360),
                CheckIn(id = 2, checkInDate = LocalDate.of(2026, 10, 9), mood = 3, energy = 3, sleepDurationMinutes = 420),
                CheckIn(id = 3, checkInDate = LocalDate.of(2026, 10, 11), mood = 2, energy = 3, sleepDurationMinutes = 360),
                CheckIn(id = 4, checkInDate = LocalDate.of(2026, 10, 13), mood = 1, energy = 2, sleepDurationMinutes = 300)
            )
        )

        val result = createSummaryUseCase(
            periodStartDate = startDate,
            periodEndDate = endDate
        )

        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(4, summary.checkinDaysCount)
        assertTrue(summary.includeCheckinAnalysis)
        assertEquals("T06", summary.ruleVersion)
        assertTrue(summary.snapshotJson.contains("avgMood"))
    }

    @Test
    fun `createSummaryUseCase fails when less than 4 days recorded`() = runBlocking {
        val startDate = LocalDate.of(2026, 10, 7)
        val endDate = LocalDate.of(2026, 10, 13)

        // Only 2 check-ins
        fakeCheckInRepository.checkIns.addAll(
            listOf(
                CheckIn(id = 1, checkInDate = LocalDate.of(2026, 10, 7), mood = 2, energy = 2, sleepDurationMinutes = 360),
                CheckIn(id = 2, checkInDate = LocalDate.of(2026, 10, 9), mood = 3, energy = 3, sleepDurationMinutes = 420)
            )
        )

        val result = createSummaryUseCase(
            periodStartDate = startDate,
            periodEndDate = endDate
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `viewModel transitions to ineligible when less than 4 days`() {
        val vm = CounselorSummaryViewModel(
            fakeCheckInRepository,
            fakeScreeningRepository,
            fakeCounselorRepository,
            createSummaryUseCase,
            getEligibilityUseCase
        )

        vm.simulateIneligible()
        assertEquals(CounselorScreenState.INELIGIBLE, vm.uiState.value.screenState)
        assertEquals(3, vm.uiState.value.recordedDays)

        vm.resetToEligible()
        assertEquals(CounselorScreenState.SELECT_CONTENT, vm.uiState.value.screenState)
        assertEquals(4, vm.uiState.value.recordedDays)
    }

    @Test
    fun `isTodayCheckedIn updates properly and can be checked`() {
        val vm = CounselorSummaryViewModel(
            fakeCheckInRepository,
            fakeScreeningRepository,
            fakeCounselorRepository,
            createSummaryUseCase,
            getEligibilityUseCase
        )

        assertFalse(vm.uiState.value.isTodayCheckedIn)
        vm.setTodayCheckedIn(true)
        assertTrue(vm.uiState.value.isTodayCheckedIn)
        vm.setTodayCheckedIn(false)
        assertFalse(vm.uiState.value.isTodayCheckedIn)
    }

    // Fake Repositories
    private class FakeCheckInRepo : CheckInRepository {
        val checkIns = mutableListOf<CheckIn>()

        override suspend fun saveCheckIn(checkIn: CheckIn): Result<CheckIn> {
            checkIns.add(checkIn)
            return Result.success(checkIn)
        }
        override suspend fun updateCheckIn(checkIn: CheckIn): Result<Unit> = Result.success(Unit)
        override suspend fun deleteCheckIn(checkIn: CheckIn): Result<Unit> = Result.success(Unit)
        override suspend fun deleteCheckInById(id: Long): Result<Unit> = Result.success(Unit)
        override suspend fun getCheckInByDate(date: LocalDate): CheckIn? = checkIns.find { it.checkInDate == date }
        override fun getCheckInByDateFlow(date: LocalDate): Flow<CheckIn?> = flowOf(checkIns.find { it.checkInDate == date })
        override suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckIn> {
            return checkIns.filter { it.checkInDate in startDate..endDate }
        }
        override fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckIn>> {
            return flowOf(checkIns.filter { it.checkInDate in startDate..endDate })
        }
        override fun getAllCheckInsFlow(): Flow<List<CheckIn>> = flowOf(checkIns)
        override suspend fun getLatestCheckIn(): CheckIn? = checkIns.maxByOrNull { it.checkInDate }
        override suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int {
            return checkIns.count { it.checkInDate in startDate..endDate }
        }
        override suspend fun getCheckInByUuid(uuid: String): CheckIn? = checkIns.find { it.uuid == uuid }
    }

    private class FakeScreeningRepo : ScreeningRepository {
        val screenings = mutableListOf<ScreeningResult>()

        override suspend fun submitScreening(
            screeningResult: ScreeningResult,
            answers: List<com.example.selaras.domain.model.ScreeningAnswer>
        ): Result<Long> {
            screenings.add(screeningResult)
            return Result.success(1L)
        }
        override suspend fun getScreeningResultById(id: Long): ScreeningResult? = screenings.find { it.id == id }
        override suspend fun getScreeningResultByPeriod(
            screeningType: ScreeningType,
            startDate: LocalDate,
            endDate: LocalDate
        ): ScreeningResult? = null
        override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> = flowOf(screenings)
        override suspend fun getLatestScreeningResult(): ScreeningResult? = screenings.lastOrNull()
        override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> = flowOf(screenings.lastOrNull())
        override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? = null
        override suspend fun getAnswersByScreeningResultId(screeningResultId: Long) = emptyList<com.example.selaras.domain.model.ScreeningAnswer>()
    }

    private class FakeCounselorRepo : CounselorRepository {
        val summaries = mutableListOf<CounselorSummary>()

        override suspend fun createSummary(summary: CounselorSummary): Result<CounselorSummary> {
            val withId = summary.copy(id = (summaries.size + 1).toLong())
            summaries.add(withId)
            return Result.success(withId)
        }
        override suspend fun getSummaryById(id: Long): CounselorSummary? = summaries.find { it.id == id }
        override suspend fun getSummaryByScreeningResultId(screeningResultId: Long): CounselorSummary? =
            summaries.find { it.screeningResultId == screeningResultId }
        override fun getAllSummariesFlow(): Flow<List<CounselorSummary>> = flowOf(summaries)
        override suspend fun deleteSummary(id: Long): Result<Unit> {
            summaries.removeAll { it.id == id }
            return Result.success(Unit)
        }
    }
}
