package com.example.selaras

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.checkin.GetTodayCheckInUseCase
import com.example.selaras.domain.usecase.checkin.GetWeeklyCheckInAnalysisUseCase
import com.example.selaras.features.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class FakeCheckInRepository : CheckInRepository {
    private val checkIns = mutableListOf<CheckIn>()
    private val checkInsFlow = MutableStateFlow<List<CheckIn>>(emptyList())

    override suspend fun saveCheckIn(checkIn: CheckIn): Result<CheckIn> {
        val saved = checkIn.copy(id = (checkIns.size + 1).toLong())
        checkIns.removeIf { it.checkInDate == checkIn.checkInDate }
        checkIns.add(saved)
        checkInsFlow.value = checkIns.toList()
        return Result.success(saved)
    }

    override suspend fun updateCheckIn(checkIn: CheckIn): Result<Unit> {
        checkIns.removeIf { it.id == checkIn.id || it.checkInDate == checkIn.checkInDate }
        checkIns.add(checkIn)
        checkInsFlow.value = checkIns.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteCheckIn(checkIn: CheckIn): Result<Unit> {
        checkIns.removeIf { it.id == checkIn.id }
        checkInsFlow.value = checkIns.toList()
        return Result.success(Unit)
    }

    override suspend fun deleteCheckInById(id: Long): Result<Unit> {
        checkIns.removeIf { it.id == id }
        checkInsFlow.value = checkIns.toList()
        return Result.success(Unit)
    }

    override suspend fun getCheckInByDate(date: LocalDate): CheckIn? =
        checkIns.firstOrNull { it.checkInDate == date }

    override fun getCheckInByDateFlow(date: LocalDate): Flow<CheckIn?> =
        checkInsFlow.map { list -> list.firstOrNull { it.checkInDate == date } }

    override suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckIn> =
        checkIns.filter { it.checkInDate in startDate..endDate }

    override fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckIn>> =
        checkInsFlow.map { list -> list.filter { it.checkInDate in startDate..endDate } }

    override fun getAllCheckInsFlow(): Flow<List<CheckIn>> = checkInsFlow.asStateFlow()

    override suspend fun getLatestCheckIn(): CheckIn? =
        checkIns.maxByOrNull { it.checkInDate }

    override suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int =
        checkIns.count { it.checkInDate in startDate..endDate }

    override suspend fun getCheckInByUuid(uuid: String): CheckIn? =
        checkIns.firstOrNull { it.uuid == uuid }
}

class FakeScreeningRepository : ScreeningRepository {
    private val _latestResult = MutableStateFlow<ScreeningResult?>(null)

    override suspend fun submitScreening(
        screeningResult: ScreeningResult,
        answers: List<ScreeningAnswer>
    ): Result<Long> {
        _latestResult.value = screeningResult
        return Result.success(1L)
    }

    override suspend fun getScreeningResultById(id: Long): ScreeningResult? = _latestResult.value
    override suspend fun getScreeningResultByPeriod(
        screeningType: ScreeningType,
        startDate: LocalDate,
        endDate: LocalDate
    ): ScreeningResult? = _latestResult.value

    override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> =
        flowOf(listOfNotNull(_latestResult.value))

    override suspend fun getLatestScreeningResult(): ScreeningResult? = _latestResult.value
    override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> = _latestResult.asStateFlow()
    override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? = _latestResult.value
    override suspend fun getAnswersByScreeningResultId(screeningResultId: Long): List<ScreeningAnswer> =
        emptyList()

    fun emitResult(result: ScreeningResult?) {
        _latestResult.value = result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var checkInRepository: FakeCheckInRepository
    private lateinit var fakeScreeningRepository: FakeScreeningRepository
    private lateinit var viewModel: DashboardViewModel

    private val fixedDate = LocalDate.of(2026, 11, 21) // Sabtu, 21 November 2026
    private val morningTime = LocalTime.of(8, 30) // 08:30

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        checkInRepository = FakeCheckInRepository()
        fakeScreeningRepository = FakeScreeningRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(
        time: LocalTime = morningTime,
        date: LocalDate = fixedDate
    ): DashboardViewModel {
        return DashboardViewModel(
            getTodayCheckInUseCase = GetTodayCheckInUseCase(checkInRepository),
            getWeeklyAnalysisUseCase = GetWeeklyCheckInAnalysisUseCase(checkInRepository),
            screeningRepository = fakeScreeningRepository,
            timeProvider = { time },
            dateProvider = { date }
        )
    }

    @Test
    fun testGreetingComputation() {
        assertEquals("Selamat Pagi", DashboardViewModel.computeGreeting(LocalTime.of(7, 0)))
        assertEquals("Selamat Siang", DashboardViewModel.computeGreeting(LocalTime.of(12, 30)))
        assertEquals("Selamat Sore", DashboardViewModel.computeGreeting(LocalTime.of(16, 45)))
        assertEquals("Selamat Malam", DashboardViewModel.computeGreeting(LocalTime.of(21, 15)))
        assertEquals("Selamat Malam", DashboardViewModel.computeGreeting(LocalTime.of(2, 0)))
    }

    @Test
    fun testDateFormattingIndonesian() {
        val date = LocalDate.of(2026, 11, 21)
        val formatted = DashboardViewModel.formatDateIndonesian(date)
        assertEquals("Sabtu, 21 November 2026", formatted)
    }

    @Test
    fun testInitialStateWhenNoCheckInRecorded() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Selamat Pagi", state.greeting)
        assertEquals("Sabtu, 21 November 2026", state.formattedDate)
        assertFalse(state.isTodayCheckedIn)
        assertNull(state.todayCheckIn)
        assertEquals(0, state.daysRecordedThisWeek)
        assertFalse(state.isEligibleForCounselorSummary)
        assertEquals(4, state.daysNeededForCounselorSummary)
    }

    @Test
    fun testStateUpdatesWhenTodayCheckInIsSaved() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTodayCheckedIn)

        // Save a check-in for today
        val todayEntry = CheckIn(
            checkInDate = fixedDate,
            mood = 4,
            energy = 7,
            sleepDurationMinutes = 450,
            note = "Hari ini produktif dan tenang"
        )
        checkInRepository.saveCheckIn(todayEntry)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isTodayCheckedIn)
        assertNotNull(state.todayCheckIn)
        assertEquals(4, state.todayCheckIn?.mood)
        assertEquals(7, state.todayCheckIn?.energy)
        assertEquals(450, state.todayCheckIn?.sleepDurationMinutes)
        assertEquals("Hari ini produktif dan tenang", state.todayCheckIn?.note)
    }

    @Test
    fun testWeeklyAnalysisAndCounselorEligibility() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        // Insert 4 entries within the 7-day window
        for (i in 0..3) {
            checkInRepository.saveCheckIn(
                CheckIn(
                    checkInDate = fixedDate.minusDays(i.toLong()),
                    mood = 3 + (i % 2),
                    energy = 6 + (i % 3),
                    sleepDurationMinutes = 420
                )
            )
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(4, state.daysRecordedThisWeek)
        assertTrue(state.isEligibleForCounselorSummary)
        assertEquals(0, state.daysNeededForCounselorSummary)
        assertNotNull(state.weeklyAnalysis)
    }

    @Test
    fun testScreeningResultObservation() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.latestScreening)

        val mockScreening = ScreeningResult(
            id = 1L,
            screeningType = ScreeningType.ROUTINE,
            periodStartDate = fixedDate.minusDays(6),
            periodEndDate = fixedDate,
            instrumentVersion = "PHQ-9",
            totalScore = 6,
            category = ScreeningCategory.RINGAN,
            helpIndicator = false,
            createdAt = Instant.now()
        )
        fakeScreeningRepository.emitResult(mockScreening)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.latestScreening)
        assertEquals(6, state.latestScreening?.totalScore)
        assertEquals(ScreeningCategory.RINGAN, state.latestScreening?.category)
    }
}
