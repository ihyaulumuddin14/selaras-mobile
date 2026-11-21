package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.repository.CheckInRepositoryImpl
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.MoodTrend
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.usecase.checkin.GetTodayCheckInUseCase
import com.example.selaras.domain.usecase.checkin.GetWeeklyCheckInAnalysisUseCase
import com.example.selaras.domain.usecase.checkin.SaveCheckInUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CheckInUseCasesTest {

    private lateinit var database: SelarasDatabase
    private lateinit var repository: CheckInRepository
    private lateinit var saveCheckInUseCase: SaveCheckInUseCase
    private lateinit var getTodayCheckInUseCase: GetTodayCheckInUseCase
    private lateinit var getWeeklyAnalysisUseCase: GetWeeklyCheckInAnalysisUseCase

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val localDataSource = CheckInLocalDataSource(database.checkInDao())
        repository = CheckInRepositoryImpl(localDataSource)

        saveCheckInUseCase = SaveCheckInUseCase(repository)
        getTodayCheckInUseCase = GetTodayCheckInUseCase(repository)
        getWeeklyAnalysisUseCase = GetWeeklyCheckInAnalysisUseCase(repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveTodayCheckInSuccess() = runTest {
        val today = LocalDate.now()
        val checkIn = CheckIn(
            checkInDate = today,
            mood = 4,
            energy = 8,
            sleepDurationMinutes = 450,
            note = "Hari ini produktif"
        )

        val result = saveCheckInUseCase(checkIn)
        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertTrue(saved.id > 0)

        val todayObserved = getTodayCheckInUseCase().first()
        assertNotNull(todayObserved)
        assertEquals(4, todayObserved?.mood)
        assertEquals(8, todayObserved?.energy)
    }

    @Test
    fun testSaveFutureDateRejected() = runTest {
        val tomorrow = LocalDate.now().plusDays(1)
        val checkIn = CheckIn(
            checkInDate = tomorrow,
            mood = 3,
            energy = 5,
            sleepDurationMinutes = 400
        )

        val result = saveCheckInUseCase(checkIn)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun testSaveExistingDateUpdatesInsteadOfFailing() = runTest {
        val today = LocalDate.now()
        val checkIn1 = CheckIn(checkInDate = today, mood = 2, energy = 3, sleepDurationMinutes = 300)
        saveCheckInUseCase(checkIn1)

        val checkIn2 = CheckIn(checkInDate = today, mood = 5, energy = 9, sleepDurationMinutes = 480, note = "Membaik di malam hari")
        val result = saveCheckInUseCase(checkIn2)
        assertTrue(result.isSuccess)

        val current = getTodayCheckInUseCase().first()
        assertEquals(5, current?.mood)
        assertEquals(9, current?.energy)
        assertEquals("Membaik di malam hari", current?.note)
    }

    @Test
    fun testWeeklyAnalysisCalculationsAndEligibility() = runTest {
        val today = LocalDate.now()

        // Empty analysis
        val emptyAnalysis = getWeeklyAnalysisUseCase.getOnce(today)
        assertEquals(0, emptyAnalysis.totalDaysRecorded)
        assertFalse(emptyAnalysis.hasData)
        assertFalse(emptyAnalysis.isEligibleForCounselorSummary)

        // Insert 4 days of check-in
        for (i in 0..3) {
            val date = today.minusDays(i.toLong())
            saveCheckInUseCase(
                CheckIn(
                    checkInDate = date,
                    mood = 4,
                    energy = 7,
                    sleepDurationMinutes = 420
                )
            )
        }

        val analysis = getWeeklyAnalysisUseCase.getOnce(today)
        assertEquals(4, analysis.totalDaysRecorded)
        assertTrue(analysis.hasData)
        assertEquals(4.0, analysis.averageMood, 0.01)
        assertEquals(7.0, analysis.averageEnergy, 0.01)
        assertEquals(420, analysis.averageSleepMinutes)
        assertEquals(7.0, analysis.averageSleepHours, 0.01)

        // Comply with PRD rule K02: >= 4 days is eligible for Counselor Summary
        assertTrue(analysis.isEligibleForCounselorSummary)
        assertTrue(analysis.insights.isNotEmpty())
    }
}
