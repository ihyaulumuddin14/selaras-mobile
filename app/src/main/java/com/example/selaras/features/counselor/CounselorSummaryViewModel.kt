@file:Suppress("NewApi")

package com.example.selaras.features.counselor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.CounselorEligibility
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

enum class CounselorScreenState {
    SELECT_CONTENT,    // Figma 168:2333: Check-in 4/7, optional screening & notes
    INELIGIBLE,        // Figma 168:2334: < 4 days (3/7), alert warning
    PREPARING,         // Figma 168:2339: Menyiapkan ringkasanmu... (loading)
    PREVIEW,           // Figma 168:2336 / 168:2337: Snapshot ditinjau S-01 / S-02
    SOURCE_CHANGED,    // Figma 168:2338: Data berubah setelah ditinjau (r4 -> r5)
    GENERATING_PDF,    // Figma 168:2340: Sedang menyiapkan PDF...
    PDF_ERROR,         // Figma 168:2341: PDF belum berhasil disiapkan
    EXPORT_SUCCESS     // Figma 168:2342: PDF siap kamu simpan
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
    val isSimulatedMode: Boolean = true,
    val periodStartDate: LocalDate = LocalDate.of(2026, 10, 7),
    val periodEndDate: LocalDate = LocalDate.of(2026, 10, 13),
    val ruleVersion: String = "T06",
    val recordedDays: Int = 4,
    val totalDays: Int = 7,
    val recordedDates: List<LocalDate> = listOf(
        LocalDate.of(2026, 10, 7),
        LocalDate.of(2026, 10, 9),
        LocalDate.of(2026, 10, 11),
        LocalDate.of(2026, 10, 13)
    ),
    val missingDates: List<LocalDate> = listOf(
        LocalDate.of(2026, 10, 8),
        LocalDate.of(2026, 10, 10),
        LocalDate.of(2026, 10, 12)
    ),
    val avgMood: Double = 2.0,
    val avgSleepHours: Double = 6.0,
    val avgEnergy: Double = 2.5,
    val dailyItems: List<DailySummaryItem> = emptyList(),
    // Content selection
    val selectCheckIn: Boolean = true, // mandatory
    val selectScreeningManual: Boolean = false,
    val selectScreeningRoutine: Boolean = false,
    val includePersonalNotes: Boolean = false,
    val selectNoteN09: Boolean = false,
    val selectNoteN11: Boolean = false,
    // Snapshot info
    val snapshotId: String = "S-01",
    val sourceRevision: String = "r4",
    val createdTimeString: String = "13 Oktober 2026 · 14.30",
    val generatedPdfFile: File? = null,
    val errorMessage: String? = null
)

class CounselorSummaryViewModel(
    private val checkInRepository: CheckInRepository,
    private val screeningRepository: ScreeningRepository,
    private val counselorRepository: CounselorRepository,
    private val createSummaryUseCase: CreateCounselorSummaryUseCase,
    private val getEligibilityUseCase: GetCounselorEligibilityUseCase,
    private val exportPdfUseCase: ExportCounselorSummaryPdfUseCase = ExportCounselorSummaryPdfUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(buildDefaultInitialState())
    val uiState: StateFlow<CounselorSummaryUiState> = _uiState.asStateFlow()

    init {
        // Initialize default daily items matching Figma dummy design
        setupDefaultDailyItems()
    }

    private fun setupDefaultDailyItems() {
        val startDate = LocalDate.of(2026, 10, 7)
        val items = listOf(
            DailySummaryItem(7, startDate, true, mood = 2, sleepHours = 6.0, energy = 2),
            DailySummaryItem(8, startDate.plusDays(1), false),
            DailySummaryItem(9, startDate.plusDays(2), true, mood = 3, sleepHours = 7.0, energy = 3, note = "Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan."),
            DailySummaryItem(10, startDate.plusDays(3), false),
            DailySummaryItem(11, startDate.plusDays(4), true, mood = 2, sleepHours = 6.0, energy = 3),
            DailySummaryItem(12, startDate.plusDays(5), false),
            DailySummaryItem(13, startDate.plusDays(6), true, mood = 1, sleepHours = 5.0, energy = 2)
        )
        _uiState.update { it.copy(dailyItems = items) }
    }

    private fun buildDefaultInitialState(): CounselorSummaryUiState {
        return CounselorSummaryUiState(
            screenState = CounselorScreenState.SELECT_CONTENT,
            periodStartDate = LocalDate.of(2026, 10, 7),
            periodEndDate = LocalDate.of(2026, 10, 13),
            recordedDays = 4,
            totalDays = 7,
            avgMood = 2.0,
            avgSleepHours = 6.0,
            avgEnergy = 2.5
        )
    }

    // Toggle content selection
    fun toggleScreeningManual(selected: Boolean) {
        _uiState.update { it.copy(selectScreeningManual = selected) }
    }

    fun toggleScreeningRoutine(selected: Boolean) {
        _uiState.update { it.copy(selectScreeningRoutine = selected) }
    }

    fun toggleIncludeNotes(included: Boolean) {
        _uiState.update {
            it.copy(
                includePersonalNotes = included,
                // if turned off, unselect sub-notes
                selectNoteN09 = if (included) it.selectNoteN09 else false,
                selectNoteN11 = if (included) it.selectNoteN11 else false
            )
        }
    }

    fun toggleNoteN09(selected: Boolean) {
        _uiState.update { it.copy(selectNoteN09 = selected) }
    }

    fun toggleNoteN11(selected: Boolean) {
        _uiState.update { it.copy(selectNoteN11 = selected) }
    }

    // Workflow actions
    fun createSummary() {
        if (_uiState.value.recordedDays < 4) {
            _uiState.update { it.copy(screenState = CounselorScreenState.INELIGIBLE) }
            return
        }

        val hasScreenings = _uiState.value.selectScreeningManual || _uiState.value.selectScreeningRoutine
        val hasNotes = _uiState.value.includePersonalNotes && (_uiState.value.selectNoteN09 || _uiState.value.selectNoteN11)
        val snapshotId = if (hasScreenings || hasNotes) "S-02" else "S-01"

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    screenState = CounselorScreenState.PREPARING,
                    snapshotId = snapshotId
                )
            }

            // Preparing delay for realistic transition
            delay(1000)

            _uiState.update {
                it.copy(
                    screenState = CounselorScreenState.PREVIEW,
                    sourceRevision = "r4"
                )
            }
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.GENERATING_PDF) }
            delay(800)

            val currentState = _uiState.value
            val summary = CounselorSummary(
                screeningResultId = if (currentState.selectScreeningManual || currentState.selectScreeningRoutine) 1L else null,
                periodStartDate = currentState.periodStartDate,
                periodEndDate = currentState.periodEndDate,
                includeCheckinAnalysis = true,
                checkinDaysCount = currentState.recordedDays,
                ruleVersion = currentState.ruleVersion,
                snapshotJson = buildSnapshotJsonString(currentState)
            )

            val exportResult = exportPdfUseCase(context, summary)
            if (exportResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.EXPORT_SUCCESS,
                        generatedPdfFile = exportResult.getOrNull()
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.PDF_ERROR,
                        errorMessage = exportResult.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

    fun refreshPreview() {
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.PREPARING) }
            delay(800)
            _uiState.update {
                it.copy(
                    screenState = CounselorScreenState.PREVIEW,
                    sourceRevision = "r5"
                )
            }
        }
    }

    fun retryExport(context: Context) {
        exportPdf(context)
    }

    fun backToPreview() {
        _uiState.update { it.copy(screenState = CounselorScreenState.PREVIEW) }
    }

    fun backToSelectContent() {
        _uiState.update { it.copy(screenState = CounselorScreenState.SELECT_CONTENT) }
    }

    fun setScreenState(state: CounselorScreenState) {
        _uiState.update { it.copy(screenState = state) }
    }

    // Figma simulation helpers
    fun simulateIneligible() {
        val startDate = LocalDate.of(2026, 10, 7)
        val items = listOf(
            DailySummaryItem(7, startDate, true, mood = 2, sleepHours = 6.0, energy = 2),
            DailySummaryItem(8, startDate.plusDays(1), false),
            DailySummaryItem(9, startDate.plusDays(2), true, mood = 3, sleepHours = 7.0, energy = 3),
            DailySummaryItem(10, startDate.plusDays(3), false),
            DailySummaryItem(11, startDate.plusDays(4), true, mood = 2, sleepHours = 6.0, energy = 3),
            DailySummaryItem(12, startDate.plusDays(5), false),
            DailySummaryItem(13, startDate.plusDays(6), false) // 13 Okt not available!
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
            return CounselorSummaryViewModel(emptyCheckIn, emptyScreening, emptyCounselor, createUseCase, eligibilityUseCase)
        }
    }
}
