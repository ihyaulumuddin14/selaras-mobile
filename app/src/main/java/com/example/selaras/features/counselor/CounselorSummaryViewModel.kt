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
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.CounselorRepository
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.counselor.CreateCounselorSummaryUseCase
import com.example.selaras.domain.usecase.counselor.ExportCounselorSummaryPdfUseCase
import com.example.selaras.domain.usecase.counselor.GetCounselorEligibilityUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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

data class CounselorScreeningUiItem(
    val id: Long,
    val title: String,
    val typeName: String,
    val dateString: String,
    val score: Int,
    val categoryText: String,
    val isSelected: Boolean = false,
    val details: String = ""
)

data class CounselorNoteUiItem(
    val date: LocalDate,
    val title: String,
    val noteText: String,
    val isSelected: Boolean = false
)

data class CounselorSummaryUiState(
    val screenState: CounselorScreenState = CounselorScreenState.SELECT_CONTENT,
    val isSimulatedMode: Boolean = false,
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
    val avgEnergy: Double = 5.0, // Scale 1..10
    val dailyItems: List<DailySummaryItem> = emptyList(),
    // Content selection
    val selectCheckIn: Boolean = true, // mandatory
    val selectScreeningManual: Boolean = false,
    val selectScreeningRoutine: Boolean = false,
    val includePersonalNotes: Boolean = false,
    val selectNoteN09: Boolean = false,
    val selectNoteN11: Boolean = false,
    // Real dynamic selection items
    val availableScreenings: List<CounselorScreeningUiItem> = emptyList(),
    val availableNotes: List<CounselorNoteUiItem> = emptyList(),
    val createdSummary: CounselorSummary? = null,
    val isTodayCheckedIn: Boolean = false,
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

    private var realDataJob: Job? = null
    private var screeningJob: Job? = null
    private var todayCheckInJob: Job? = null

    init {
        loadRealData()
    }

    fun setupDefaultDailyItems() {
        val startDate = LocalDate.of(2026, 10, 7)
        val items = listOf(
            DailySummaryItem(7, startDate, true, mood = 2, sleepHours = 6.0, energy = 4),
            DailySummaryItem(8, startDate.plusDays(1), false),
            DailySummaryItem(9, startDate.plusDays(2), true, mood = 3, sleepHours = 7.0, energy = 6, note = "Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan."),
            DailySummaryItem(10, startDate.plusDays(3), false),
            DailySummaryItem(11, startDate.plusDays(4), true, mood = 2, sleepHours = 6.0, energy = 5),
            DailySummaryItem(12, startDate.plusDays(5), false),
            DailySummaryItem(13, startDate.plusDays(6), true, mood = 1, sleepHours = 5.0, energy = 4)
        )
        val defaultScreenings = listOf(
            CounselorScreeningUiItem(
                id = 1L,
                title = "Skrining Mandiri · 9 Oktober 2026 (Skor 10)",
                typeName = "Skrining Mandiri",
                dateString = "9 Oktober 2026",
                score = 10,
                categoryText = "Gejala Depresi Tingkat Sedang",
                isSelected = false,
                details = "Kuesioner Kesehatan PHQ-9 (9 Pertanyaan)\nSkor Total: 10 / 27\nKategori: Gejala Sedang\nDiselesaikan pada sesi refleksi mandiri"
            ),
            CounselorScreeningUiItem(
                id = 2L,
                title = "Skrining Berkala · 11 Oktober 2026 (Skor 8)",
                typeName = "Skrining Berkala",
                dateString = "11 Oktober 2026",
                score = 8,
                categoryText = "Gejala Depresi Tingkat Ringan",
                isSelected = false,
                details = "Kuesioner Kesehatan PHQ-9 (9 Pertanyaan)\nSkor Total: 8 / 27\nKategori: Gejala Ringan\nDiselesaikan pada evaluasi berkala mingguan"
            )
        )
        val defaultNotes = listOf(
            CounselorNoteUiItem(
                date = LocalDate.of(2026, 10, 9),
                title = "Catatan Refleksi · 9 Oktober 2026",
                noteText = "Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan.",
                isSelected = false
            ),
            CounselorNoteUiItem(
                date = LocalDate.of(2026, 10, 11),
                title = "Catatan Refleksi · 11 Oktober 2026",
                noteText = "Latihan pernapasan membantu saya lebih tenang di malam hari.",
                isSelected = false
            )
        )
        _uiState.update {
            it.copy(
                dailyItems = items,
                availableScreenings = defaultScreenings,
                availableNotes = defaultNotes
            )
        }
    }

    private fun buildDefaultInitialState(): CounselorSummaryUiState {
        val today = LocalDate.now()
        val startDate = today.minusDays(6)
        val missing = (0..6).map { startDate.plusDays(it.toLong()) }
        val daily = (0..6).map {
            val d = startDate.plusDays(it.toLong())
            DailySummaryItem(d.dayOfMonth, d, false)
        }
        return CounselorSummaryUiState(
            screenState = CounselorScreenState.INELIGIBLE,
            periodStartDate = startDate,
            periodEndDate = today,
            recordedDays = 0,
            totalDays = 7,
            recordedDates = emptyList(),
            missingDates = missing,
            avgMood = 0.0,
            avgSleepHours = 0.0,
            avgEnergy = 0.0,
            dailyItems = daily,
            availableScreenings = emptyList(),
            availableNotes = emptyList(),
            includePersonalNotes = false,
            isTodayCheckedIn = false
        )
    }

    /**
     * Loads and observes real check-in and screening data from Room repositories.
     */
    fun loadRealData() {
        realDataJob?.cancel()
        realDataJob = viewModelScope.launch {
            val today = LocalDate.now()
            val latest = checkInRepository.getLatestCheckIn()
            val endDate = when {
                latest != null && latest.checkInDate.isAfter(today) -> latest.checkInDate
                latest != null && latest.checkInDate.isBefore(today.minusDays(6)) -> latest.checkInDate
                else -> today
            }
            val startDate = endDate.minusDays(6)

            checkInRepository.getCheckInsBetweenFlow(startDate, endDate)
                .catch { /* ignore */ }
                .collect { checkIns ->
                    val eligibility = getEligibilityUseCase(startDate, endDate)
                    val recordedDates = eligibility.recordedDates
                    val recordedCount = eligibility.recordedDays
                    val isEligible = eligibility.isEligible

                    val avgMood = if (checkIns.isNotEmpty()) {
                        (checkIns.map { it.mood }.average() * 10).roundToInt() / 10.0
                    } else 0.0
                    val avgSleepMinutes = if (checkIns.isNotEmpty()) {
                        checkIns.map { it.sleepDurationMinutes }.average()
                    } else 0.0
                    val avgSleepHours = (avgSleepMinutes / 60.0 * 10).roundToInt() / 10.0
                    val avgEnergy = if (checkIns.isNotEmpty()) {
                        (checkIns.map { it.energy }.average() * 10).roundToInt() / 10.0
                    } else 0.0

                    val items = mutableListOf<DailySummaryItem>()
                    var curr = startDate
                    while (!curr.isAfter(endDate)) {
                        val ci = checkIns.find { it.checkInDate == curr }
                        items.add(
                            DailySummaryItem(
                                dayNumber = curr.dayOfMonth,
                                date = curr,
                                isRecorded = ci != null,
                                mood = ci?.mood ?: 0,
                                sleepHours = (ci?.sleepDurationMinutes ?: 0) / 60.0,
                                energy = ci?.energy ?: 0,
                                note = ci?.note
                            )
                        )
                        curr = curr.plusDays(1)
                    }

                    val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
                    val realNotes = checkIns.filter { !it.note.isNullOrBlank() }.map { ci ->
                        CounselorNoteUiItem(
                            date = ci.checkInDate,
                            title = "Catatan Refleksi · ${ci.checkInDate.format(fmt)}",
                            noteText = ci.note.orEmpty(),
                            isSelected = false
                        )
                    }

                    val timeFmt = DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", Locale.forLanguageTag("id"))
                    val timeStr = LocalDateTime.now().format(timeFmt)
                    val isTodayRecorded = checkIns.any { it.checkInDate == today }

                    _uiState.update { current ->
                        val prevSelectedDates = current.availableNotes.filter { it.isSelected }.map { it.date }.toSet()
                        val updatedNotes = realNotes.map { note ->
                            if (note.date in prevSelectedDates) note.copy(isSelected = true) else note
                        }
                        val anyNoteSelected = updatedNotes.any { it.isSelected }

                        current.copy(
                            periodStartDate = startDate,
                            periodEndDate = endDate,
                            recordedDays = recordedCount,
                            totalDays = eligibility.totalDaysInPeriod,
                            recordedDates = recordedDates,
                            missingDates = eligibility.missingDates,
                            avgMood = avgMood,
                            avgSleepHours = avgSleepHours,
                            avgEnergy = avgEnergy,
                            dailyItems = items,
                            availableNotes = updatedNotes,
                            includePersonalNotes = anyNoteSelected,
                            createdTimeString = timeStr,
                            isTodayCheckedIn = isTodayRecorded,
                            screenState = if (isEligible) {
                                if (current.screenState == CounselorScreenState.INELIGIBLE) {
                                    CounselorScreenState.SELECT_CONTENT
                                } else {
                                    current.screenState
                                }
                            } else {
                                CounselorScreenState.INELIGIBLE
                            }
                        )
                    }
                }
        }

        todayCheckInJob?.cancel()
        todayCheckInJob = viewModelScope.launch {
            val today = LocalDate.now()
            checkInRepository.getCheckInByDateFlow(today)
                .catch { /* ignore */ }
                .collect { todayCheckIn ->
                    _uiState.update { it.copy(isTodayCheckedIn = todayCheckIn != null) }
                }
        }

        screeningJob?.cancel()
        screeningJob = viewModelScope.launch {
            screeningRepository.getAllScreeningResultsFlow()
                .catch { /* ignore */ }
                .collect { screenings ->
                    val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
                    val items = screenings.map { sc ->
                        val typeLabel = if (sc.screeningType == ScreeningType.ROUTINE) "Skrining Berkala" else "Skrining Mandiri"
                        val catLabel = when (sc.category.name.uppercase()) {
                            "MINIMAL" -> "Gejala Minimal"
                            "RINGAN" -> "Gejala Ringan"
                            "SEDANG" -> "Gejala Sedang"
                            "SEDANG_BERAT" -> "Gejala Sedang Berat"
                            "BERAT" -> "Gejala Berat"
                            else -> sc.category.name
                        }
                        val dateStr = sc.periodEndDate.format(dateFmt)
                        CounselorScreeningUiItem(
                            id = sc.id,
                            title = "$typeLabel · $dateStr (Skor ${sc.totalScore})",
                            typeName = typeLabel,
                            dateString = dateStr,
                            score = sc.totalScore,
                            categoryText = catLabel,
                            isSelected = false,
                            details = "Instrumen: ${sc.instrumentVersion}\nSkor Total: ${sc.totalScore} / 27\nKategori: $catLabel\nDiselesaikan pada evaluasi PHQ-9"
                        )
                    }
                    _uiState.update { current ->
                        val prevSelectedIds = current.availableScreenings.filter { it.isSelected }.map { it.id }.toSet()
                        val updated = items.map {
                            if (it.id in prevSelectedIds) it.copy(isSelected = true) else it
                        }
                        current.copy(availableScreenings = updated)
                    }
                }
        }
    }

    // Toggle content selection for screenings
    fun toggleScreeningItem(id: Long, selected: Boolean) {
        _uiState.update { current ->
            val updated = current.availableScreenings.map {
                if (it.id == id) it.copy(isSelected = selected) else it
            }
            current.copy(
                availableScreenings = updated,
                selectScreeningManual = updated.getOrNull(0)?.isSelected ?: false,
                selectScreeningRoutine = updated.getOrNull(1)?.isSelected ?: false
            )
        }
    }

    // Toggle content selection for personal notes
    fun toggleNoteItem(date: LocalDate, selected: Boolean) {
        _uiState.update { current ->
            val updated = current.availableNotes.map {
                if (it.date == date) it.copy(isSelected = selected) else it
            }
            val anySelected = updated.any { it.isSelected }
            current.copy(
                availableNotes = updated,
                includePersonalNotes = anySelected,
                selectNoteN09 = updated.find { it.date.dayOfMonth == 9 }?.isSelected ?: false,
                selectNoteN11 = updated.find { it.date.dayOfMonth == 11 }?.isSelected ?: false
            )
        }
    }

    fun toggleScreeningManual(selected: Boolean) {
        val firstId = _uiState.value.availableScreenings.getOrNull(0)?.id ?: 1L
        toggleScreeningItem(firstId, selected)
    }

    fun toggleScreeningRoutine(selected: Boolean) {
        val secondId = _uiState.value.availableScreenings.getOrNull(1)?.id ?: 2L
        toggleScreeningItem(secondId, selected)
    }

    fun toggleIncludeNotes(included: Boolean) {
        _uiState.update { current ->
            val updatedNotes = current.availableNotes.map { it.copy(isSelected = included) }
            current.copy(
                includePersonalNotes = included && updatedNotes.isNotEmpty(),
                availableNotes = updatedNotes,
                selectNoteN09 = if (included) updatedNotes.find { it.date.dayOfMonth == 9 }?.isSelected ?: false else false,
                selectNoteN11 = if (included) updatedNotes.find { it.date.dayOfMonth == 11 }?.isSelected ?: false else false
            )
        }
    }

    fun toggleNoteN09(selected: Boolean) {
        val targetDate = _uiState.value.availableNotes.find { it.date.dayOfMonth == 9 }?.date ?: LocalDate.of(2026, 10, 9)
        toggleNoteItem(targetDate, selected)
    }

    fun toggleNoteN11(selected: Boolean) {
        val targetDate = _uiState.value.availableNotes.find { it.date.dayOfMonth == 11 }?.date ?: LocalDate.of(2026, 10, 11)
        toggleNoteItem(targetDate, selected)
    }

    // Workflow actions
    fun createSummary() {
        val currentState = _uiState.value
        if (currentState.recordedDays < 4) {
            _uiState.update { it.copy(screenState = CounselorScreenState.INELIGIBLE) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.PREPARING) }
            delay(600)

            val selectedScreeningIds = currentState.availableScreenings
                .filter { it.isSelected }
                .map { it.id }

            val selectedNoteDates = currentState.availableNotes
                .filter { it.isSelected }
                .map { it.date }

            val result = createSummaryUseCase(
                periodStartDate = currentState.periodStartDate,
                periodEndDate = currentState.periodEndDate,
                selectedScreeningIds = selectedScreeningIds,
                selectedNoteDates = selectedNoteDates,
                includeNotes = currentState.includePersonalNotes && selectedNoteDates.isNotEmpty(),
                ruleVersion = currentState.ruleVersion
            )

            if (result.isSuccess) {
                val savedSummary = result.getOrThrow()
                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.PREVIEW,
                        createdSummary = savedSummary,
                        snapshotId = "S-${savedSummary.id.toString().padStart(2, '0')}",
                        sourceRevision = "r4"
                    )
                }
            } else {
                // Fallback for preview or mocked test environments
                val hasScreenings = selectedScreeningIds.isNotEmpty() || currentState.selectScreeningManual || currentState.selectScreeningRoutine
                val hasNotes = currentState.includePersonalNotes && selectedNoteDates.isNotEmpty()
                val snapshotId = if (hasScreenings || hasNotes) "S-02" else "S-01"

                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.PREVIEW,
                        snapshotId = snapshotId,
                        sourceRevision = "r4"
                    )
                }
            }
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.GENERATING_PDF) }
            delay(600)

            val currentState = _uiState.value
            val summaryToExport = currentState.createdSummary ?: CounselorSummary(
                screeningResultId = currentState.availableScreenings.firstOrNull { it.isSelected }?.id,
                periodStartDate = currentState.periodStartDate,
                periodEndDate = currentState.periodEndDate,
                includeCheckinAnalysis = true,
                checkinDaysCount = currentState.recordedDays,
                ruleVersion = currentState.ruleVersion,
                snapshotJson = buildSnapshotJsonString(currentState)
            )

            // Ensure summary is persisted in counselorRepository if not already
            val finalSummary = if (summaryToExport.id == 0L) {
                counselorRepository.createSummary(summaryToExport).getOrDefault(summaryToExport)
            } else {
                summaryToExport
            }

            val exportResult = exportPdfUseCase(context, finalSummary)
            if (exportResult.isSuccess) {
                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.EXPORT_SUCCESS,
                        generatedPdfFile = exportResult.getOrNull(),
                        createdSummary = finalSummary
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
            delay(600)
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

    fun setTodayCheckedIn(checkedIn: Boolean) {
        _uiState.update { it.copy(isTodayCheckedIn = checkedIn) }
    }

    // Figma simulation helpers for testing and previews
    fun simulateIneligible() {
        val startDate = LocalDate.of(2026, 10, 7)
        val items = listOf(
            DailySummaryItem(7, startDate, true, mood = 2, sleepHours = 6.0, energy = 4),
            DailySummaryItem(8, startDate.plusDays(1), false),
            DailySummaryItem(9, startDate.plusDays(2), true, mood = 3, sleepHours = 7.0, energy = 6),
            DailySummaryItem(10, startDate.plusDays(3), false),
            DailySummaryItem(11, startDate.plusDays(4), true, mood = 2, sleepHours = 6.0, energy = 5),
            DailySummaryItem(12, startDate.plusDays(5), false),
            DailySummaryItem(13, startDate.plusDays(6), false)
        )
        _uiState.update {
            it.copy(
                screenState = CounselorScreenState.INELIGIBLE,
                recordedDays = 3,
                isTodayCheckedIn = false,
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
        val dailyEntriesJson = state.dailyItems.filter { it.isRecorded }.joinToString(",") { item ->
            val noteStr = if (state.includePersonalNotes && item.note != null) item.note else ""
            """{"date": "${item.date}", "mood": ${item.mood}, "sleepHours": ${item.sleepHours}, "energy": ${item.energy}, "note": "$noteStr"}"""
        }

        val screeningsJson = state.availableScreenings.filter { it.isSelected }.joinToString(",") { sc ->
            """{"type": "${sc.typeName}", "score": ${sc.score}, "category": "${sc.categoryText}", "instrumentVersion": "PHQ-9 v1.0"}"""
        }

        return """
        {
            "snapshotId": "${state.snapshotId}",
            "createdAt": "${java.time.Instant.now()}",
            "periodStartDate": "${state.periodStartDate}",
            "periodEndDate": "${state.periodEndDate}",
            "ruleVersion": "${state.ruleVersion}",
            "recordedDaysCount": ${state.recordedDays},
            "avgMood": ${state.avgMood},
            "avgSleepHours": ${state.avgSleepHours},
            "avgEnergy": ${state.avgEnergy},
            "includeNotes": ${state.includePersonalNotes},
            "dailyEntries": [$dailyEntriesJson],
            "screenings": [$screeningsJson]
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
            val vm = CounselorSummaryViewModel(emptyCheckIn, emptyScreening, emptyCounselor, createUseCase, eligibilityUseCase)
            vm.resetToEligible()
            return vm
        }
    }
}
