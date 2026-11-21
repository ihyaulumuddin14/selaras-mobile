package com.example.selaras.features.checkin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.WeeklyCheckInAnalysis
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.usecase.checkin.GetTodayCheckInUseCase
import com.example.selaras.domain.usecase.checkin.GetWeeklyCheckInAnalysisUseCase
import com.example.selaras.domain.usecase.checkin.SaveCheckInUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

enum class CheckInStep { MOOD, ENERGY, SLEEP, NOTE, SUCCESS }
enum class CheckInTab { TODAY, WEEKLY_ANALYSIS, HISTORY }

data class CheckInUiState(
    val currentTab: CheckInTab = CheckInTab.TODAY,
    val currentStep: CheckInStep = CheckInStep.MOOD,
    val todayCheckIn: CheckIn? = null,
    val isEditingToday: Boolean = false,
    val isAlreadyCheckedIn: Boolean = false,

    // Form inputs:
    val formMood: Int = 3, // 1..5
    val formEnergy: Int = 5, // 1..10
    val formSleepHours: Int = 7, // 0..16
    val formSleepMinutes: Int = 0, // 0..59
    val formNote: String = "",

    val isSaving: Boolean = false,
    val errorMessage: String? = null,

    // 7-day pattern analysis:
    val weeklyAnalysis: WeeklyCheckInAnalysis? = null,
    val historyEntries: List<CheckIn> = emptyList(),
    val isLoadingAnalysis: Boolean = false
) {
    val totalSleepMinutes: Int
        get() = (formSleepHours * 60) + formSleepMinutes
}

class CheckInViewModel(
    private val saveCheckInUseCase: SaveCheckInUseCase,
    private val getTodayCheckInUseCase: GetTodayCheckInUseCase,
    private val getWeeklyAnalysisUseCase: GetWeeklyCheckInAnalysisUseCase,
    private val repository: CheckInRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckInUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        // Observe today's check-in
        viewModelScope.launch {
            getTodayCheckInUseCase().collect { today ->
                _uiState.update { state ->
                    val shouldUpdateForm = state.isEditingToday && state.todayCheckIn == null && today != null
                    state.copy(
                        todayCheckIn = today,
                        isAlreadyCheckedIn = today != null,
                        formMood = if (shouldUpdateForm) today!!.mood else state.formMood,
                        formEnergy = if (shouldUpdateForm) today!!.energy else state.formEnergy,
                        formSleepHours = if (shouldUpdateForm) today!!.sleepHours else state.formSleepHours,
                        formSleepMinutes = if (shouldUpdateForm) today!!.sleepRemainingMinutes else state.formSleepMinutes,
                        formNote = if (shouldUpdateForm) today!!.note.orEmpty() else state.formNote
                    )
                }
            }
        }

        // Observe 7-day weekly analysis
        viewModelScope.launch {
            getWeeklyAnalysisUseCase().collect { analysis ->
                _uiState.update { state ->
                    state.copy(
                        weeklyAnalysis = analysis,
                        isLoadingAnalysis = false
                    )
                }
            }
        }

        // Observe all check-ins for history
        viewModelScope.launch {
            repository.getAllCheckInsFlow().collect { all ->
                _uiState.update { state ->
                    state.copy(historyEntries = all)
                }
            }
        }
    }

    fun setTab(tab: CheckInTab) {
        _uiState.update { it.copy(currentTab = tab, errorMessage = null) }
    }

    fun setMood(mood: Int) {
        if (mood in 1..5) {
            _uiState.update { it.copy(formMood = mood, errorMessage = null) }
        }
    }

    fun setEnergy(energy: Int) {
        if (energy in 1..10) {
            _uiState.update { it.copy(formEnergy = energy, errorMessage = null) }
        }
    }

    fun setSleepDuration(hours: Int, minutes: Int) {
        _uiState.update {
            it.copy(
                formSleepHours = hours.coerceIn(0, 16),
                formSleepMinutes = minutes.coerceIn(0, 59),
                errorMessage = null
            )
        }
    }

    fun setNote(note: String) {
        _uiState.update {
            it.copy(formNote = note.take(500), errorMessage = null)
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        val next = when (current) {
            CheckInStep.MOOD -> CheckInStep.ENERGY
            CheckInStep.ENERGY -> CheckInStep.SLEEP
            CheckInStep.SLEEP -> CheckInStep.NOTE
            CheckInStep.NOTE -> CheckInStep.NOTE
            CheckInStep.SUCCESS -> CheckInStep.SUCCESS
        }
        _uiState.update { it.copy(currentStep = next, errorMessage = null) }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        val prev = when (current) {
            CheckInStep.MOOD -> CheckInStep.MOOD
            CheckInStep.ENERGY -> CheckInStep.MOOD
            CheckInStep.SLEEP -> CheckInStep.ENERGY
            CheckInStep.NOTE -> CheckInStep.SLEEP
            CheckInStep.SUCCESS -> CheckInStep.NOTE
        }
        _uiState.update { it.copy(currentStep = prev, errorMessage = null) }
    }

    fun startEditToday() {
        val today = _uiState.value.todayCheckIn
        _uiState.update {
            it.copy(
                currentTab = CheckInTab.TODAY,
                isEditingToday = true,
                currentStep = CheckInStep.MOOD,
                formMood = today?.mood ?: it.formMood,
                formEnergy = today?.energy ?: it.formEnergy,
                formSleepHours = today?.sleepHours ?: it.formSleepHours,
                formSleepMinutes = today?.sleepRemainingMinutes ?: it.formSleepMinutes,
                formNote = today?.note.orEmpty(),
                errorMessage = null
            )
        }
    }

    /**
     * Direct navigation shortcut to the 7-day weekly analysis tab from Dashboard.
     */
    fun navigateToWeeklyAnalysis() {
        _uiState.update {
            it.copy(
                currentTab = CheckInTab.WEEKLY_ANALYSIS,
                isEditingToday = false,
                errorMessage = null
            )
        }
    }

    /**
     * Direct navigation shortcut to start a new check-in wizard from Dashboard.
     */
    fun navigateToNewCheckIn() {
        _uiState.update {
            it.copy(
                currentTab = CheckInTab.TODAY,
                isEditingToday = false,
                currentStep = CheckInStep.MOOD,
                errorMessage = null
            )
        }
    }

    fun cancelEditToday() {
        _uiState.update {
            it.copy(
                isEditingToday = false,
                currentStep = CheckInStep.MOOD,
                errorMessage = null
            )
        }
    }

    fun saveCheckIn(): Job {
        val state = _uiState.value
        if (state.isSaving) return Job().apply { complete() }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        return viewModelScope.launch {
            val checkIn = CheckIn(
                id = state.todayCheckIn?.id ?: 0L,
                uuid = state.todayCheckIn?.uuid ?: java.util.UUID.randomUUID().toString(),
                checkInDate = LocalDate.now(),
                mood = state.formMood,
                energy = state.formEnergy,
                sleepDurationMinutes = state.totalSleepMinutes,
                note = state.formNote.trim().ifEmpty { null }
            )

            saveCheckInUseCase(checkIn).fold(
                onSuccess = { saved ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditingToday = false,
                            currentStep = CheckInStep.SUCCESS,
                            todayCheckIn = saved,
                            isAlreadyCheckedIn = true,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Gagal menyimpan check-in: ${error.message}"
                        )
                    }
                }
            )
        }
    }

    fun dismissSuccess() {
        _uiState.update {
            it.copy(
                currentStep = CheckInStep.MOOD,
                currentTab = CheckInTab.WEEKLY_ANALYSIS
            )
        }
    }

    class Factory(
        private val repository: CheckInRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CheckInViewModel::class.java))
            val save = SaveCheckInUseCase(repository)
            val today = GetTodayCheckInUseCase(repository)
            val weekly = GetWeeklyCheckInAnalysisUseCase(repository)
            @Suppress("UNCHECKED_CAST")
            return CheckInViewModel(save, today, weekly, repository) as T
        }
    }
}
