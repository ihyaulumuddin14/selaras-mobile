package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.repository.CheckInRepositoryImpl
import com.example.selaras.domain.usecase.checkin.GetTodayCheckInUseCase
import com.example.selaras.domain.usecase.checkin.GetWeeklyCheckInAnalysisUseCase
import com.example.selaras.domain.usecase.checkin.SaveCheckInUseCase
import com.example.selaras.features.checkin.CheckInStep
import com.example.selaras.features.checkin.CheckInTab
import com.example.selaras.features.checkin.CheckInViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CheckInViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: SelarasDatabase
    private lateinit var viewModel: CheckInViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val localDataSource = CheckInLocalDataSource(database.checkInDao())
        val repository = CheckInRepositoryImpl(localDataSource)

        val saveUseCase = SaveCheckInUseCase(repository)
        val todayUseCase = GetTodayCheckInUseCase(repository)
        val weeklyUseCase = GetWeeklyCheckInAnalysisUseCase(repository)

        viewModel = CheckInViewModel(saveUseCase, todayUseCase, weeklyUseCase, repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testStepProgressionAndFormInputs() {
        val state1 = viewModel.uiState.value
        assertEquals(CheckInStep.MOOD, state1.currentStep)

        // Set Mood and advance
        viewModel.setMood(4)
        viewModel.nextStep()
        assertEquals(CheckInStep.ENERGY, viewModel.uiState.value.currentStep)
        assertEquals(4, viewModel.uiState.value.formMood)

        // Set Energy and advance
        viewModel.setEnergy(8)
        viewModel.nextStep()
        assertEquals(CheckInStep.SLEEP, viewModel.uiState.value.currentStep)
        assertEquals(8, viewModel.uiState.value.formEnergy)

        // Set Sleep and advance
        viewModel.setSleepDuration(7, 30)
        viewModel.nextStep()
        assertEquals(CheckInStep.NOTE, viewModel.uiState.value.currentStep)
        assertEquals(450, viewModel.uiState.value.totalSleepMinutes)

        // Set Note and previous step
        viewModel.setNote("Catatan hari ini")
        assertEquals("Catatan hari ini", viewModel.uiState.value.formNote)

        viewModel.previousStep()
        assertEquals(CheckInStep.SLEEP, viewModel.uiState.value.currentStep)
    }

    @Test
    fun testSaveCheckInLifecycle() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAlreadyCheckedIn)

        viewModel.setMood(5)
        viewModel.setEnergy(9)
        viewModel.setSleepDuration(8, 0)
        viewModel.setNote("Sangat baik!")

        viewModel.saveCheckIn().join()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Error was: ${state.errorMessage}", CheckInStep.SUCCESS, state.currentStep)
        assertTrue(state.isAlreadyCheckedIn)
        assertNotNull(state.todayCheckIn)
        assertEquals(5, state.todayCheckIn?.mood)
        assertEquals(9, state.todayCheckIn?.energy)
        assertEquals(480, state.todayCheckIn?.sleepDurationMinutes)

        viewModel.dismissSuccess()
        assertEquals(CheckInTab.WEEKLY_ANALYSIS, viewModel.uiState.value.currentTab)
    }

    @Test
    fun testStartEditTodayPopulatesForm() = runTest(testDispatcher) {
        // Save initial check-in
        viewModel.setMood(2)
        viewModel.setEnergy(4)
        viewModel.setSleepDuration(6, 0)
        viewModel.saveCheckIn().join()
        advanceUntilIdle()

        // Start edit
        viewModel.startEditToday()
        val editState = viewModel.uiState.value
        assertTrue(editState.isEditingToday)
        assertEquals(2, editState.formMood)
        assertEquals(4, editState.formEnergy)
        assertEquals(6, editState.formSleepHours)

        // Change values and save
        viewModel.setMood(4)
        viewModel.setEnergy(7)
        viewModel.saveCheckIn().join()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditingToday)
        assertEquals(4, viewModel.uiState.value.todayCheckIn?.mood)
        assertEquals(7, viewModel.uiState.value.todayCheckIn?.energy)
    }
}
