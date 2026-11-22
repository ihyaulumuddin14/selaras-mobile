@file:Suppress("NewApi")

package com.example.selaras.features.counselor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.CounselorSummary
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.CounselorRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.counselor.CreateCounselorSummaryUseCase
import com.example.selaras.domain.usecase.counselor.ExportCounselorSummaryPdfUseCase
import com.example.selaras.domain.usecase.counselor.GetCounselorEligibilityUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale


enum class CounselorScreenState {
    SELECT_CONTENT, INELIGIBLE, PREPARING, PREVIEW, SOURCE_CHANGED,
    GENERATING_PDF, PDF_ERROR, EXPORT_SUCCESS
}

data class DailySummaryItem(
    val dayNumber: Int,
    val date: LocalDate,
    val isRecorded: Boolean,
    val mood: Int = 0,
    val sleepHours: Double = 0.0,
    val energy: Int = 0,
    val note: String? = null
)

data class CounselorSummaryUiState(
    val screenState: CounselorScreenState = CounselorScreenState.SELECT_CONTENT,
    val isSimulatedMode: Boolean = false,
    val isLoading: Boolean = true,
    val periodStartDate: LocalDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
    val periodEndDate: LocalDate = LocalDate.now(),
    val ruleVersion: String = "T06",
    val recordedDays: Int = 0,
    val totalDays: Int = 7,
    val recordedDates: List<LocalDate> = emptyList(),
    val missingDates: List<LocalDate> = emptyList(),
    val avgMood: Double = 0.0,
    val avgSleepHours: Double = 0.0,
    val avgEnergy: Double = 0.0,
    val dailyItems: List<DailySummaryItem> = emptyList(),
    val selectCheckIn: Boolean = true,
    val selectScreeningManual: Boolean = false,
    val selectScreeningRoutine: Boolean = false,
    val includePersonalNotes: Boolean = false,
    val selectNoteN09: Boolean = false,
    val selectNoteN11: Boolean = false,
    val selectedNoteDates: Set<LocalDate> = emptySet(),
    val availableManualScreening: ScreeningResult? = null,
    val availableRoutineScreening: ScreeningResult? = null,
    val handoffResultId: Long? = null,
    val previewScreenings: List<ScreeningResult> = emptyList(),
    val previewSummary: CounselorSummary? = null,
    val snapshotId: String = "S-01",
    val sourceRevision: String = "r4",
    val createdTimeString: String = "",
    val generatedPdfFile: File? = null,
    val errorMessage: String? = null
)

class CounselorSummaryViewModel(
    private val checkInRepository: CheckInRepository,
    private val screeningRepository: ScreeningRepository,
    private val counselorRepository: CounselorRepository,
    private val createSummaryUseCase: CreateCounselorSummaryUseCase,
    private val getEligibilityUseCase: GetCounselorEligibilityUseCase,
    private val exportPdfUseCase: ExportCounselorSummaryPdfUseCase = ExportCounselorSummaryPdfUseCase(),
    private val previewMode: Boolean = false,
    private val autoLoad: Boolean = false
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        if (previewMode) buildDefaultInitialState() else CounselorSummaryUiState()
    )
    val uiState: StateFlow<CounselorSummaryUiState> = _uiState.asStateFlow()
    private var lastHandoffId: Long? = null

    init {
        if (previewMode) {
            setupDefaultDailyItems()
        } else if (autoLoad) {
            refreshData()
            viewModelScope.launch {
                try {
                    screeningRepository.getAllScreeningResultsFlow().collect { screenings ->
                        val newest = screenings.sortedByDescending { it.completedAt }
                        _uiState.update { state ->
                            val preferred = newest.firstOrNull { it.id == state.handoffResultId }
                            val manual = if (preferred?.screeningType == ScreeningType.MANUAL) preferred
                                else newest.firstOrNull { it.screeningType == ScreeningType.MANUAL }
                            val routine = if (preferred?.screeningType == ScreeningType.ROUTINE) preferred
                                else newest.firstOrNull { it.screeningType == ScreeningType.ROUTINE }
                            state.copy(
                                availableManualScreening = manual,
                                availableRoutineScreening = routine,
                                selectScreeningManual = state.selectScreeningManual && manual != null,
                                selectScreeningRoutine = state.selectScreeningRoutine && routine != null
                            )
                        }
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update { it.copy(errorMessage = exception.message ?: "Gagal membaca hasil skrining") }
                }
            }
            viewModelScope.launch {
                try {
                    checkInRepository.getAllCheckInsFlow().collect { refreshData() }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = exception.message ?: "Gagal memuat data check-in") }
                }
            }
        }
    }

    private fun buildDefaultInitialState(): CounselorSummaryUiState = CounselorSummaryUiState(
        isSimulatedMode = true, isLoading = false,
        periodStartDate = LocalDate.of(2026, 10, 7),
        periodEndDate = LocalDate.of(2026, 10, 13),
        recordedDays = 4, totalDays = 7,
        recordedDates = listOf(LocalDate.of(2026, 10, 7), LocalDate.of(2026, 10, 9), LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 13)),
        missingDates = listOf(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 12)),
        avgMood = 2.0, avgSleepHours = 6.0, avgEnergy = 2.5,
        createdTimeString = "13 Oktober 2026 · 14.30"
    )

    private fun setupDefaultDailyItems() {
        val startDate = LocalDate.of(2026, 10, 7)
        _uiState.update {
            it.copy(dailyItems = listOf(
                DailySummaryItem(7, startDate, true, 2, 6.0, 2),
                DailySummaryItem(8, startDate.plusDays(1), false),
                DailySummaryItem(9, startDate.plusDays(2), true, 3, 7.0, 3, "Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan."),
                DailySummaryItem(10, startDate.plusDays(3), false),
                DailySummaryItem(11, startDate.plusDays(4), true, 2, 6.0, 3),
                DailySummaryItem(12, startDate.plusDays(5), false),
                DailySummaryItem(13, startDate.plusDays(6), true, 1, 5.0, 2)
            ))
        }
    }

    fun refreshData() {
        if (previewMode) return
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val eligibility = getEligibilityUseCase(start, today)
                val checkIns = checkInRepository.getCheckInsBetween(start, today)
                val byDate = checkIns.associateBy { it.checkInDate }
                val days = generateSequence(start) { if (it.isBefore(today)) it.plusDays(1) else null }.toList()
                val recorded = checkIns.distinctBy { it.checkInDate }
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        periodStartDate = start,
                        periodEndDate = today,
                        recordedDays = eligibility.recordedDays,
                        totalDays = eligibility.totalDaysInPeriod,
                        recordedDates = eligibility.recordedDates,
                        missingDates = eligibility.missingDates,
                        avgMood = if (recorded.isEmpty()) 0.0 else recorded.map { it.mood }.average(),
                        avgSleepHours = if (recorded.isEmpty()) 0.0 else recorded.map { it.sleepDurationMinutes }.average() / 60.0,
                        avgEnergy = if (recorded.isEmpty()) 0.0 else recorded.map { it.energy }.average(),
                        dailyItems = days.map { date ->
                            val entry = byDate[date]
                            DailySummaryItem(
                                dayNumber = date.dayOfMonth,
                                date = date,
                                isRecorded = entry != null,
                                mood = entry?.mood ?: 0,
                                sleepHours = (entry?.sleepDurationMinutes ?: 0) / 60.0,
                                energy = entry?.energy ?: 0,
                                note = entry?.note
                            )
                        },
                        selectedNoteDates = state.selectedNoteDates.filterTo(mutableSetOf()) { date ->
                            byDate[date]?.note?.isNotBlank() == true
                        },
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = exception.message ?: "Gagal membaca check-in") }
            }
        }
    }

    fun acceptHandoffResultId(id: Long) {
        if (previewMode || id <= 0L || id == lastHandoffId) return
        lastHandoffId = id
        viewModelScope.launch {
            try {
                val result = screeningRepository.getScreeningResultById(id)
                    ?: throw IllegalStateException("Hasil skrining #$id tidak ditemukan di penyimpanan perangkat.")
                _uiState.update { state ->
                    if (result.screeningType == ScreeningType.MANUAL) {
                        state.copy(
                            handoffResultId = id,
                            availableManualScreening = result,
                            selectScreeningManual = true,
                            screenState = CounselorScreenState.SELECT_CONTENT,
                            errorMessage = null
                        )
                    } else {
                        state.copy(
                            handoffResultId = id,
                            availableRoutineScreening = result,
                            selectScreeningRoutine = true,
                            screenState = CounselorScreenState.SELECT_CONTENT,
                            errorMessage = null
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: "Gagal membuka hasil skrining") }
                lastHandoffId = null
            }
        }
    }

    fun toggleScreeningManual(selected: Boolean) {
        _uiState.update { it.copy(selectScreeningManual = selected && it.availableManualScreening != null) }
    }

    fun toggleScreeningRoutine(selected: Boolean) {
        _uiState.update { it.copy(selectScreeningRoutine = selected && it.availableRoutineScreening != null) }
    }

    fun toggleIncludeNotes(included: Boolean) {
        _uiState.update {
            it.copy(
                includePersonalNotes = included,
                selectedNoteDates = if (included) it.selectedNoteDates else emptySet(),
                selectNoteN09 = if (included) it.selectNoteN09 else false,
                selectNoteN11 = if (included) it.selectNoteN11 else false
            )
        }
    }

    fun toggleNoteDate(date: LocalDate, selected: Boolean) {
        _uiState.update {
            it.copy(selectedNoteDates = if (selected) it.selectedNoteDates + date else it.selectedNoteDates - date)
        }
    }

    fun toggleNoteN09(selected: Boolean) {
        _uiState.update { it.copy(selectNoteN09 = selected) }
    }

    fun toggleNoteN11(selected: Boolean) {
        _uiState.update { it.copy(selectNoteN11 = selected) }
    }

    fun createSummary() {
        if (previewMode) {
            val state = _uiState.value
            if (state.recordedDays < 4) {
                _uiState.update { it.copy(screenState = CounselorScreenState.INELIGIBLE) }
            } else {
                _uiState.update { it.copy(screenState = CounselorScreenState.PREVIEW,
                    snapshotId = if (state.selectScreeningManual || state.selectScreeningRoutine || state.includePersonalNotes) "S-02" else "S-01") }
            }
            return
        }
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.PREPARING, errorMessage = null) }
            try {
                val initial = _uiState.value
                val ids = listOfNotNull(
                    if (initial.selectScreeningManual) initial.availableManualScreening?.id else null,
                    if (initial.selectScreeningRoutine) initial.availableRoutineScreening?.id else null
                ).distinct()
                val result = createSummaryUseCase(
                    periodStartDate = initial.periodStartDate,
                    periodEndDate = initial.periodEndDate,
                    selectedScreeningIds = ids,
                    selectedNoteDates = if (initial.includePersonalNotes) initial.selectedNoteDates.toList() else emptyList(),
                    includeNotes = initial.includePersonalNotes && initial.selectedNoteDates.isNotEmpty(),
                    ruleVersion = initial.ruleVersion
                ).getOrThrow()
                _uiState.update { it.copy(
                    screenState = CounselorScreenState.PREVIEW,
                    previewSummary = result,
                    previewScreenings = listOfNotNull(
                        if (initial.selectScreeningManual) initial.availableManualScreening else null,
                        if (initial.selectScreeningRoutine) initial.availableRoutineScreening else null
                    ),
                    snapshotId = "S-${result.id}",
                    createdTimeString = result.createdAt.atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", Locale.forLanguageTag("id")))
                ) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val message = exception.message ?: "Gagal membuat ringkasan"
                val insufficient = message.contains("Minimal 4", ignoreCase = true)
                _uiState.update { it.copy(
                    screenState = if (insufficient) CounselorScreenState.INELIGIBLE else CounselorScreenState.SELECT_CONTENT,
                    errorMessage = message
                ) }
                refreshData()
            }
        }
    }

    fun exportPdf(context: Context) {
        if (!previewMode && _uiState.value.previewSummary == null) {
            _uiState.update { it.copy(screenState = CounselorScreenState.SELECT_CONTENT, errorMessage = "Buat ringkasan terlebih dahulu.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.GENERATING_PDF, errorMessage = null) }
            try {
                val state = _uiState.value
                val summary = state.previewSummary ?: CounselorSummary(
                    periodStartDate = state.periodStartDate,
                    periodEndDate = state.periodEndDate,
                    screeningResultId = null,
                    includeCheckinAnalysis = true,
                    checkinDaysCount = state.recordedDays,
                    ruleVersion = state.ruleVersion,
                    snapshotJson = buildSnapshotJsonString(state)
                )
                val fileName = "Selaras-Ringkasan-${System.currentTimeMillis()}.pdf"
                val file = withContext(Dispatchers.IO) {
                    exportPdfUseCase(context, summary, fileName).getOrThrow()
                }
                _uiState.update { it.copy(screenState = CounselorScreenState.EXPORT_SUCCESS, generatedPdfFile = file) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(screenState = CounselorScreenState.PDF_ERROR,
                    errorMessage = exception.message ?: "Gagal mengekspor PDF") }
            }
        }
    }

    fun refreshPreview() = createSummary()

    fun retryExport(context: Context) = exportPdf(context)

    fun backToPreview() {
        _uiState.update { it.copy(screenState = if (it.previewSummary != null || previewMode) CounselorScreenState.PREVIEW else CounselorScreenState.SELECT_CONTENT) }
    }

    fun backToSelectContent() {
        _uiState.update { it.copy(screenState = CounselorScreenState.SELECT_CONTENT) }
    }

    fun setScreenState(state: CounselorScreenState) {
        _uiState.update { it.copy(screenState = state) }
    }

    fun simulateIneligible() {
        val startDate = LocalDate.of(2026, 10, 7)
        val items = listOf(
            DailySummaryItem(7, startDate, true, mood = 2, sleepHours = 6.0, energy = 2),
            DailySummaryItem(8, startDate.plusDays(1), false),
            DailySummaryItem(9, startDate.plusDays(2), true, mood = 3, sleepHours = 7.0, energy = 3),
            DailySummaryItem(10, startDate.plusDays(3), false),
            DailySummaryItem(11, startDate.plusDays(4), true, mood = 2, sleepHours = 6.0, energy = 3),
            DailySummaryItem(12, startDate.plusDays(5), false),
            DailySummaryItem(13, startDate.plusDays(6), false)
        )
        _uiState.update {
            it.copy(
                screenState = CounselorScreenState.INELIGIBLE,
                recordedDays = 3,
                dailyItems = items,
                recordedDates = listOf(
                    LocalDate.of(2026, 10, 7),
                    LocalDate.of(2026, 10, 9),
                    LocalDate.of(2026, 10, 11)
                ),
                missingDates = listOf(
                    LocalDate.of(2026, 10, 8),
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 12),
                    LocalDate.of(2026, 10, 13)
                )
            )
        }
    }

    fun resetToEligible() {
        setupDefaultDailyItems()
        _uiState.update {
            it.copy(
                screenState = CounselorScreenState.SELECT_CONTENT,
                recordedDays = 4,
                recordedDates = listOf(
                    LocalDate.of(2026, 10, 7),
                    LocalDate.of(2026, 10, 9),
                    LocalDate.of(2026, 10, 11),
                    LocalDate.of(2026, 10, 13)
                ),
                missingDates = listOf(
                    LocalDate.of(2026, 10, 8),
                    LocalDate.of(2026, 10, 10),
                    LocalDate.of(2026, 10, 12)
                )
            )
        }
    }

    fun simulateSourceChanged() {
        _uiState.update {
            it.copy(
                screenState = CounselorScreenState.SOURCE_CHANGED,
                sourceRevision = "r5"
            )
        }
    }

    fun simulatePdfError() {
        _uiState.update { it.copy(screenState = CounselorScreenState.PDF_ERROR) }
    }

    fun simulateSuccess() {
        _uiState.update { it.copy(screenState = CounselorScreenState.EXPORT_SUCCESS) }
    }

    private fun buildSnapshotJsonString(state: CounselorSummaryUiState): String {
        return """
        {
            "snapshotId": "${state.snapshotId}",
            "createdAt": "2026-10-13T14:30:00Z",
            "periodStartDate": "${state.periodStartDate}",
            "periodEndDate": "${state.periodEndDate}",
            "ruleVersion": "${state.ruleVersion}",
            "recordedDaysCount": ${state.recordedDays},
            "avgMood": ${state.avgMood},
            "avgSleepHours": ${state.avgSleepHours},
            "avgEnergy": ${state.avgEnergy},
            "includeNotes": ${state.includePersonalNotes},
            "dailyEntries": [
                {"date": "2026-10-07", "mood": 2, "sleepHours": 6.0, "energy": 2},
                {"date": "2026-10-09", "mood": 3, "sleepHours": 7.0, "energy": 3, "note": "${if (state.selectNoteN09) "Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan." else ""}"},
                {"date": "2026-10-11", "mood": 2, "sleepHours": 6.0, "energy": 3},
                {"date": "2026-10-13", "mood": 1, "sleepHours": 5.0, "energy": 2}
            ],
            "screenings": [
                ${if (state.selectScreeningManual) """{"type": "MANUAL", "score": 10, "category": "SEDANG", "instrumentVersion": "PHQ-9 v1.0"}""" else ""}${if (state.selectScreeningManual && state.selectScreeningRoutine) "," else ""}${if (state.selectScreeningRoutine) """{"type": "ROUTINE", "score": 8, "category": "RINGAN", "instrumentVersion": "PHQ-9 v1.0"}""" else ""}
            ]
        }
        """.trimIndent()
    }

    companion object {
        fun provideFactory(
            checkInRepository: CheckInRepository,
            screeningRepository: ScreeningRepository,
            counselorRepository: CounselorRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val createUseCase = CreateCounselorSummaryUseCase(checkInRepository, screeningRepository, counselorRepository)
                val eligibilityUseCase = GetCounselorEligibilityUseCase(checkInRepository)
                return CounselorSummaryViewModel(
                    checkInRepository = checkInRepository,
                    screeningRepository = screeningRepository,
                    counselorRepository = counselorRepository,
                    createSummaryUseCase = createUseCase,
                    getEligibilityUseCase = eligibilityUseCase
                ) as T
            }
        }

        fun createPreviewInstance(): CounselorSummaryViewModel {
            val emptyCheckIn = object : CheckInRepository {
                override suspend fun saveCheckIn(checkIn: CheckIn) = Result.success(checkIn)
                override suspend fun updateCheckIn(checkIn: CheckIn) = Result.success(Unit)
                override suspend fun deleteCheckIn(checkIn: CheckIn) = Result.success(Unit)
                override suspend fun deleteCheckInById(id: Long) = Result.success(Unit)
                override suspend fun getCheckInByDate(date: LocalDate): CheckIn? = null
                override fun getCheckInByDateFlow(date: LocalDate): Flow<CheckIn?> = flowOf(null)
                override suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate) = emptyList<CheckIn>()
                override fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate) = flowOf(emptyList<CheckIn>())
                override fun getAllCheckInsFlow() = flowOf(emptyList<CheckIn>())
                override suspend fun getLatestCheckIn(): CheckIn? = null
                override suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate) = 0
                override suspend fun getCheckInByUuid(uuid: String): CheckIn? = null
            }
            val emptyScreening = object : ScreeningRepository {
                override suspend fun submitScreening(screeningResult: ScreeningResult, answers: List<ScreeningAnswer>) = Result.success(1L)
                override suspend fun getScreeningResultById(id: Long): ScreeningResult? = null
                override suspend fun getScreeningResultByPeriod(screeningType: ScreeningType, startDate: LocalDate, endDate: LocalDate): ScreeningResult? = null
                override fun getAllScreeningResultsFlow() = flowOf(emptyList<ScreeningResult>())
                override suspend fun getLatestScreeningResult(): ScreeningResult? = null
                override fun getLatestScreeningResultFlow() = flowOf(null)
                override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? = null
                override suspend fun getAnswersByScreeningResultId(screeningResultId: Long) = emptyList<ScreeningAnswer>()
                override suspend fun getScreeningTypesUsedThisWeek(referenceDate: LocalDate): Set<ScreeningType> = emptySet()
                override suspend fun countScreeningResultsByTypeBetween(screeningType: ScreeningType, startInclusive: Instant, endExclusive: Instant): Int = 0
            }
            val emptyCounselor = object : CounselorRepository {
                override suspend fun createSummary(summary: CounselorSummary) = Result.success(summary)
                override suspend fun getSummaryById(id: Long): CounselorSummary? = null
                override suspend fun getSummaryByScreeningResultId(screeningResultId: Long): CounselorSummary? = null
                override fun getAllSummariesFlow() = flowOf(emptyList<CounselorSummary>())
                override suspend fun deleteSummary(id: Long) = Result.success(Unit)
            }
            val createUseCase = CreateCounselorSummaryUseCase(emptyCheckIn, emptyScreening, emptyCounselor)
            val eligibilityUseCase = GetCounselorEligibilityUseCase(emptyCheckIn)
            return CounselorSummaryViewModel(emptyCheckIn, emptyScreening, emptyCounselor, createUseCase, eligibilityUseCase, previewMode = true)
        }
    }
}
