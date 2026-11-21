package com.example.selaras.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.checkin.GetTodayCheckInUseCase
import com.example.selaras.domain.usecase.checkin.GetWeeklyCheckInAnalysisUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * ViewModel managing data streams and presentation logic for Dashboard / Beranda Utama.
 */
class DashboardViewModel(
    private val getTodayCheckInUseCase: GetTodayCheckInUseCase,
    private val getWeeklyAnalysisUseCase: GetWeeklyCheckInAnalysisUseCase,
    private val screeningRepository: ScreeningRepository,
    private val promptManager: DailyCheckInPromptManager? = null,
    private val appSettingsRepository: AppSettingsRepository? = null,
    private val timeProvider: () -> LocalTime = { LocalTime.now() },
    private val dateProvider: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            greeting = computeGreeting(timeProvider()),
            formattedDate = formatDateIndonesian(dateProvider())
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        observeDashboardData()
    }

    /**
     * Re-evaluates contextual header info and observes continuous Flow updates.
     */
    fun refresh() {
        val nowTime = timeProvider()
        val nowDate = dateProvider()
        val isCheckedIn = _uiState.value.isTodayCheckedIn
        val alreadyPrompted = promptManager?.hasPromptedToday() ?: true
        val showDialog = !isCheckedIn && !alreadyPrompted
        val shouldPulse = !isCheckedIn && alreadyPrompted
        val name = _uiState.value.userName
        val baseGreeting = computeGreeting(nowTime)
        val greeting = if (!name.isNullOrBlank()) "$baseGreeting, $name" else baseGreeting
        _uiState.update {
            it.copy(
                greeting = greeting,
                formattedDate = formatDateIndonesian(nowDate),
                showDailyCheckInDialog = showDialog,
                shouldPulseCheckInCard = shouldPulse
            )
        }
    }

    /**
     * Dismisses the daily check-in popup dialog and activates subtle pulse reminder.
     */
    fun dismissDailyCheckInDialog() {
        promptManager?.markPromptedToday()
        _uiState.update {
            it.copy(
                showDailyCheckInDialog = false,
                shouldPulseCheckInCard = !it.isTodayCheckedIn
            )
        }
    }

    /**
     * Confirms daily check-in popup dialog and prepares for navigation.
     */
    fun onConfirmDailyCheckInDialog() {
        promptManager?.markPromptedToday()
        _uiState.update {
            it.copy(
                showDailyCheckInDialog = false,
                shouldPulseCheckInCard = !it.isTodayCheckedIn
            )
        }
    }

    private fun observeDashboardData() {
        val today = dateProvider()

        // 1. Observe today's Check-In
        viewModelScope.launch {
            getTodayCheckInUseCase(today)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { checkIn ->
                    val isCheckedIn = checkIn != null
                    val alreadyPrompted = promptManager?.hasPromptedToday() ?: true
                    val showDialog = !isCheckedIn && !alreadyPrompted
                    val shouldPulse = !isCheckedIn && alreadyPrompted
                    _uiState.update {
                        it.copy(
                            todayCheckIn = checkIn,
                            isTodayCheckedIn = isCheckedIn,
                            showDailyCheckInDialog = showDialog,
                            shouldPulseCheckInCard = shouldPulse
                        )
                    }
                }
        }

        // 2. Observe 7-day pattern analysis
        viewModelScope.launch {
            getWeeklyAnalysisUseCase(today)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                }
                .collect { analysis ->
                    val recordedCount = analysis.totalDaysRecorded
                    val isEligible = analysis.isEligibleForCounselorSummary
                    val needed = (4 - recordedCount).coerceAtLeast(0)

                    _uiState.update {
                        it.copy(
                            weeklyAnalysis = analysis,
                            daysRecordedThisWeek = recordedCount,
                            isEligibleForCounselorSummary = isEligible,
                            daysNeededForCounselorSummary = needed
                        )
                    }
                }
        }

        // 3. Observe latest PHQ-9 screening result
        viewModelScope.launch {
            screeningRepository.getLatestScreeningResultFlow()
                .catch { /* Non-critical if empty */ }
                .collect { latest ->
                    _uiState.update {
                        it.copy(latestScreening = latest)
                    }
                }
        }

        // 4. Observe AppSettings for User Name (Welcoming & Settings notification badge)
        appSettingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.getSettingsFlow()
                    .catch { /* Non-critical if empty */ }
                    .collect { settings ->
                        val name = settings?.userName?.trim()
                        val isSet = !name.isNullOrBlank()
                        val nowTime = timeProvider()
                        val baseGreeting = computeGreeting(nowTime)
                        val personalizedGreeting = if (isSet) "$baseGreeting, $name" else baseGreeting
                        _uiState.update { current ->
                            current.copy(
                                userName = name,
                                isNameSet = isSet,
                                greeting = personalizedGreeting
                            )
                        }
                    }
            }
        }
    }

    companion object {
        fun computeGreeting(time: LocalTime): String {
            val hour = time.hour
            val minute = time.minute
            return when {
                hour in 4..10 -> "Selamat Pagi"
                hour in 11..14 -> "Selamat Siang"
                hour in 15..18 && (hour < 18 || minute < 30) -> "Selamat Sore"
                else -> "Selamat Malam"
            }
        }

        fun formatDateIndonesian(date: LocalDate): String {
            val dayName = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Senin"
                DayOfWeek.TUESDAY -> "Selasa"
                DayOfWeek.WEDNESDAY -> "Rabu"
                DayOfWeek.THURSDAY -> "Kamis"
                DayOfWeek.FRIDAY -> "Jumat"
                DayOfWeek.SATURDAY -> "Sabtu"
                DayOfWeek.SUNDAY -> "Minggu"
                null -> ""
            }

            val monthName = when (date.monthValue) {
                1 -> "Januari"
                2 -> "Februari"
                3 -> "Maret"
                4 -> "April"
                5 -> "Mei"
                6 -> "Juni"
                7 -> "Juli"
                8 -> "Agustus"
                9 -> "September"
                10 -> "Oktober"
                11 -> "November"
                12 -> "Desember"
                else -> ""
            }

            return "$dayName, ${date.dayOfMonth} $monthName ${date.year}"
        }
    }

    class Factory(
        private val checkInRepository: CheckInRepository,
        private val screeningRepository: ScreeningRepository,
        private val promptManager: DailyCheckInPromptManager? = null,
        private val appSettingsRepository: AppSettingsRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(DashboardViewModel::class.java))
            val todayUseCase = GetTodayCheckInUseCase(checkInRepository)
            val weeklyUseCase = GetWeeklyCheckInAnalysisUseCase(checkInRepository)
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(
                getTodayCheckInUseCase = todayUseCase,
                getWeeklyAnalysisUseCase = weeklyUseCase,
                screeningRepository = screeningRepository,
                promptManager = promptManager,
                appSettingsRepository = appSettingsRepository
            ) as T
        }
    }
}
