@file:Suppress("NewApi")

package com.example.selaras.features.counselor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.CheckIn
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

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
    val isLoading: Boolean = false,
    val periodStartDate: LocalDate = LocalDate.of(2026, 10, 7),
    val periodEndDate: LocalDate = LocalDate.of(2026, 10, 13),
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
    val availableScreenings: List<CounselorScreeningUiItem> = emptyList(),
    val availableNotes: List<CounselorNoteUiItem> = emptyList(),
    val availableManualScreening: ScreeningResult? = null,
    val availableRoutineScreening: ScreeningResult? = null,
    val handoffResultId: Long? = null,
    val previewScreenings: List<ScreeningResult> = emptyList(),
    val previewSummary: CounselorSummary? = null,
    val createdSummary: CounselorSummary? = null,
    val isTodayCheckedIn: Boolean = false,
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
            setupDefaultDailyItems()
            loadRealData()
        }
    }

    private fun buildDefaultInitialState(): CounselorSummaryUiState = CounselorSummaryUiState(
        isSimulatedMode = true,
        isLoading = false,
        periodStartDate = LocalDate.of(2026, 10, 7),
        periodEndDate = LocalDate.of(2026, 10, 13),
        recordedDays = 4,
        totalDays = 7,
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
        ),
        avgMood = 2.0,
        avgSleepHours = 6.0,
        avgEnergy = 5.0,
        createdTimeString = "13 Oktober 2026 · 14.30"
    )

    private fun setupDefaultDailyItems() {
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

    /**
     * Loads and observes real check-in and screening data from Room repositories.
     */
    fun loadRealData() {
        if (previewMode) return
        viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val latest = checkInRepository.getLatestCheckIn()
                val endDate = if (latest != null && latest.checkInDate.isAfter(today)) {
                    latest.checkInDate
                } else {
                    today
                }
                val startDate = endDate.minusDays(6)

                checkInRepository.getCheckInsBetweenFlow(startDate, endDate)
                    .catch { e ->
                        _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                    }
                    .collect { checkIns ->
                        val eligibility = getEligibilityUseCase(startDate, endDate)
                        val recordedDates = eligibility.recordedDates
                        val recordedCount = eligibility.recordedDays
                        val isEligible = eligibility.isEligible

                        val recordedCheckIns = checkIns.filter { it.checkInDate in startDate..endDate }
                        val avgMood = if (recordedCheckIns.isNotEmpty()) {
                            (recordedCheckIns.map { it.mood }.average() * 10).roundToInt() / 10.0
                        } else 0.0

                        val avgSleepMinutes = if (recordedCheckIns.isNotEmpty()) {
                            recordedCheckIns.map { it.sleepDurationMinutes }.average()
                        } else 0.0
                        val avgSleepHours = (avgSleepMinutes / 60.0 * 10).roundToInt() / 10.0

                        val avgEnergy = if (recordedCheckIns.isNotEmpty()) {
                            (recordedCheckIns.map { it.energy }.average() * 10).roundToInt() / 10.0
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

                        val notes = checkIns.filter { !it.note.isNullOrBlank() }.map { ci ->
                            val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
                            CounselorNoteUiItem(
                                date = ci.checkInDate,
                                title = "Catatan Refleksi · ${ci.checkInDate.format(fmt)}",
                                noteText = ci.note.orEmpty(),
                                isSelected = true
                            )
                        }

                        val timeFmt = DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", Locale.forLanguageTag("id"))
                        val timeStr = LocalDateTime.now().format(timeFmt)
                        val isTodayRecorded = checkIns.any { it.checkInDate == today }

                        _uiState.update { current ->
                            current.copy(
                                isLoading = false,
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
                                availableNotes = if (notes.isNotEmpty()) notes else current.availableNotes,
                                selectedNoteDates = notes.map { it.date }.toSet(),
                                createdTimeString = timeStr,
                                isTodayCheckedIn = isTodayRecorded,
                                screenState = if (isEligible) {
                                    if (current.screenState == CounselorScreenState.INELIGIBLE) CounselorScreenState.SELECT_CONTENT else current.screenState
                                } else {
                                    CounselorScreenState.INELIGIBLE
                                }
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Gagal memuat data check-in") }
            }
        }

        viewModelScope.launch {
            val today = LocalDate.now()
            checkInRepository.getCheckInByDateFlow(today)
                .catch { /* ignore */ }
                .collect { todayCheckIn ->
                    _uiState.update { it.copy(isTodayCheckedIn = todayCheckIn != null) }
                }
        }

        viewModelScope.launch {
            screeningRepository.getAllScreeningResultsFlow()
                .catch { /* ignore */ }
                .collect { screenings ->
                    if (screenings.isNotEmpty()) {
                        val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
                        _uiState.update { current ->
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
                                val wasSelected = current.availableScreenings.find { it.id == sc.id }?.isSelected == true || sc.id == current.handoffResultId
                                CounselorScreeningUiItem(
                                    id = sc.id,
                                    title = "$typeLabel · $dateStr (Skor ${sc.totalScore})",
                                    typeName = typeLabel,
                                    dateString = dateStr,
                                    score = sc.totalScore,
                                    categoryText = catLabel,
                                    isSelected = wasSelected,
                                    details = "Instrumen: ${sc.instrumentVersion}\nSkor Total: ${sc.totalScore} / 27\nKategori: $catLabel\nDiselesaikan pada evaluasi PHQ-9"
                                )
                            }
                            val manual = screenings.sortedByDescending { it.completedAt }.firstOrNull { it.screeningType == ScreeningType.MANUAL }
                            val routine = screenings.sortedByDescending { it.completedAt }.firstOrNull { it.screeningType == ScreeningType.ROUTINE }
                            current.copy(
                                availableScreenings = items,
                                availableManualScreening = manual,
                                availableRoutineScreening = routine
                            )
                        }
                    }
                }
        }
    }

    fun refreshData() = loadRealData()

    fun acceptHandoffResultId(id: Long) {
        if (previewMode || id <= 0L || id == lastHandoffId) return
        lastHandoffId = id
        viewModelScope.launch {
            try {
                val result = screeningRepository.getScreeningResultById(id)
                    ?: throw IllegalStateException("Hasil skrining #$id tidak ditemukan di penyimpanan perangkat.")
                _uiState.update { state ->
                    val isManual = result.screeningType == ScreeningType.MANUAL
                    val existing = state.availableScreenings.find { it.id == id }
                    val updatedScreenings = if (existing != null) {
                        state.availableScreenings.map {
                            if (it.id == id) it.copy(isSelected = true) else it
                        }
                    } else {
                        val typeLabel = if (isManual) "Skrining Mandiri" else "Skrining Berkala"
                        val catLabel = when (result.category.name.uppercase()) {
                            "MINIMAL" -> "Gejala Minimal"
                            "RINGAN" -> "Gejala Ringan"
                            "SEDANG" -> "Gejala Sedang"
                            "SEDANG_BERAT" -> "Gejala Sedang Berat"
                            "BERAT" -> "Gejala Berat"
                            else -> result.category.name
                        }
                        val dateFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
                        val dateStr = result.periodEndDate.format(dateFmt)
                        listOf(
                            CounselorScreeningUiItem(
                                id = result.id,
                                title = "$typeLabel · $dateStr (Skor ${result.totalScore})",
                                typeName = typeLabel,
                                dateString = dateStr,
                                score = result.totalScore,
                                categoryText = catLabel,
                                isSelected = true,
                                details = "Instrumen: ${result.instrumentVersion}\nSkor Total: ${result.totalScore} / 27\nKategori: $catLabel\nDiselesaikan pada evaluasi PHQ-9"
                            )
                        ) + state.availableScreenings
                    }

                    state.copy(
                        handoffResultId = id,
                        availableScreenings = updatedScreenings,
                        availableManualScreening = if (isManual) result else state.availableManualScreening,
                        availableRoutineScreening = if (!isManual) result else state.availableRoutineScreening,
                        selectScreeningManual = if (isManual) true else state.selectScreeningManual,
                        selectScreeningRoutine = if (!isManual) true else state.selectScreeningRoutine,
                        screenState = CounselorScreenState.SELECT_CONTENT,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(errorMessage = exception.message ?: "Gagal membuka hasil skrining") }
                lastHandoffId = null
            }
        }
    }

    // Toggle content selection for screenings by ID
    fun toggleScreeningItem(id: Long, selected: Boolean) {
        _uiState.update { current ->
            val updated = current.availableScreenings.map {
                if (it.id == id) it.copy(isSelected = selected) else it
            }
            current.copy(
                availableScreenings = updated,
                selectScreeningManual = updated.find { it.typeName.contains("Mandiri", ignoreCase = true) }?.isSelected ?: false,
                selectScreeningRoutine = updated.find { it.typeName.contains("Berkala", ignoreCase = true) }?.isSelected ?: false
            )
        }
    }

    // Toggle content selection for personal notes by Date
    fun toggleNoteItem(date: LocalDate, selected: Boolean) {
        _uiState.update { current ->
            val updated = current.availableNotes.map {
                if (it.date == date) it.copy(isSelected = selected) else it
            }
            val updatedDates = if (selected) current.selectedNoteDates + date else current.selectedNoteDates - date
            current.copy(
                availableNotes = updated,
                selectedNoteDates = updatedDates,
                selectNoteN09 = updated.find { it.date.dayOfMonth == 9 }?.isSelected ?: false,
                selectNoteN11 = updated.find { it.date.dayOfMonth == 11 }?.isSelected ?: false
            )
        }
    }

    fun toggleScreeningManual(selected: Boolean) {
        val manual = _uiState.value.availableScreenings.find { it.typeName.contains("Mandiri", ignoreCase = true) }
        if (manual != null) {
            toggleScreeningItem(manual.id, selected)
        } else {
            _uiState.update { it.copy(selectScreeningManual = selected) }
        }
    }

    fun toggleScreeningRoutine(selected: Boolean) {
        val routine = _uiState.value.availableScreenings.find { it.typeName.contains("Berkala", ignoreCase = true) }
        if (routine != null) {
            toggleScreeningItem(routine.id, selected)
        } else {
            _uiState.update { it.copy(selectScreeningRoutine = selected) }
        }
    }

    fun toggleIncludeNotes(included: Boolean) {
        _uiState.update { current ->
            current.copy(
                includePersonalNotes = included,
                availableNotes = current.availableNotes.map { it.copy(isSelected = included) },
                selectedNoteDates = if (included) current.availableNotes.map { it.date }.toSet() else emptySet(),
                selectNoteN09 = if (included) current.selectNoteN09 else false,
                selectNoteN11 = if (included) current.selectNoteN11 else false
            )
        }
    }

    fun toggleNoteDate(date: LocalDate, selected: Boolean) {
        toggleNoteItem(date, selected)
    }

    fun toggleNoteN09(selected: Boolean) {
        val targetDate = _uiState.value.availableNotes.find { it.date.dayOfMonth == 9 }?.date ?: LocalDate.of(2026, 10, 9)
        toggleNoteItem(targetDate, selected)
    }

    fun toggleNoteN11(selected: Boolean) {
        val targetDate = _uiState.value.availableNotes.find { it.date.dayOfMonth == 11 }?.date ?: LocalDate.of(2026, 10, 11)
        toggleNoteItem(targetDate, selected)
    }

    fun createSummary() {
        val currentState = _uiState.value
        if (currentState.recordedDays < 4) {
            _uiState.update { it.copy(screenState = CounselorScreenState.INELIGIBLE) }
            return
        }

        if (previewMode) {
            val hasScreenings = currentState.availableScreenings.any { it.isSelected } ||
                currentState.selectScreeningManual || currentState.selectScreeningRoutine
            val hasNotes = currentState.includePersonalNotes &&
                (currentState.availableNotes.any { it.isSelected } || currentState.selectedNoteDates.isNotEmpty())
            val snapshotId = if (hasScreenings || hasNotes) "S-02" else "S-01"
            _uiState.update {
                it.copy(
                    screenState = CounselorScreenState.PREVIEW,
                    snapshotId = snapshotId,
                    sourceRevision = "r4"
                )
            }
            return
        }

        if (currentState.isLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.PREPARING, errorMessage = null) }
            delay(600)

            try {
                val selectedScreeningIds = currentState.availableScreenings
                    .filter { it.isSelected }
                    .map { it.id }
                    .ifEmpty {
                        listOfNotNull(
                            if (currentState.selectScreeningManual) currentState.availableManualScreening?.id else null,
                            if (currentState.selectScreeningRoutine) currentState.availableRoutineScreening?.id else null
                        )
                    }
                    .distinct()

                val selectedNoteDates = if (currentState.includePersonalNotes) {
                    val fromItems = currentState.availableNotes.filter { it.isSelected }.map { it.date }
                    if (fromItems.isNotEmpty()) fromItems else currentState.selectedNoteDates.toList()
                } else {
                    emptyList()
                }

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
                    val timeStr = LocalDateTime.now().format(
                        DateTimeFormatter.ofPattern("d MMMM yyyy · HH.mm", Locale.forLanguageTag("id"))
                    )
                    _uiState.update {
                        it.copy(
                            screenState = CounselorScreenState.PREVIEW,
                            createdSummary = savedSummary,
                            previewSummary = savedSummary,
                            snapshotId = "S-${savedSummary.id.toString().padStart(2, '0')}",
                            sourceRevision = "r4",
                            createdTimeString = timeStr
                        )
                    }
                } else {
                    val ex = result.exceptionOrNull()
                    val message = ex?.message ?: "Gagal membuat ringkasan"
                    val insufficient = message.contains("Minimal 4", ignoreCase = true)
                    _uiState.update {
                        it.copy(
                            screenState = if (insufficient) CounselorScreenState.INELIGIBLE else CounselorScreenState.SELECT_CONTENT,
                            errorMessage = message
                        )
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val message = exception.message ?: "Gagal membuat ringkasan"
                val insufficient = message.contains("Minimal 4", ignoreCase = true)
                _uiState.update {
                    it.copy(
                        screenState = if (insufficient) CounselorScreenState.INELIGIBLE else CounselorScreenState.SELECT_CONTENT,
                        errorMessage = message
                    )
                }
            }
        }
    }

    fun exportPdf(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(screenState = CounselorScreenState.GENERATING_PDF, errorMessage = null) }
            delay(600)

            try {
                val currentState = _uiState.value
                val summaryToExport = currentState.createdSummary ?: currentState.previewSummary ?: CounselorSummary(
                    screeningResultId = currentState.availableScreenings.firstOrNull { it.isSelected }?.id,
                    periodStartDate = currentState.periodStartDate,
                    periodEndDate = currentState.periodEndDate,
                    includeCheckinAnalysis = true,
                    checkinDaysCount = currentState.recordedDays,
                    ruleVersion = currentState.ruleVersion,
                    snapshotJson = buildSnapshotJsonString(currentState)
                )

                val finalSummary = if (summaryToExport.id == 0L) {
                    counselorRepository.createSummary(summaryToExport).getOrDefault(summaryToExport)
                } else {
                    summaryToExport
                }

                val fileName = "Selaras-Ringkasan-${System.currentTimeMillis()}.pdf"
                val file = withContext(Dispatchers.IO) {
                    exportPdfUseCase(context, finalSummary, fileName).getOrThrow()
                }

                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.EXPORT_SUCCESS,
                        generatedPdfFile = file,
                        createdSummary = finalSummary,
                        previewSummary = finalSummary
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        screenState = CounselorScreenState.PDF_ERROR,
                        errorMessage = exception.message ?: "Gagal mengekspor PDF"
                    )
                }
            }
        }
    }

    fun refreshPreview() = createSummary()

    fun retryExport(context: Context) = exportPdf(context)

    fun backToPreview() {
        _uiState.update {
            it.copy(
                screenState = if (it.createdSummary != null || it.previewSummary != null || previewMode)
                    CounselorScreenState.PREVIEW
                else
                    CounselorScreenState.SELECT_CONTENT
            )
        }
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
            "createdAt": "${Instant.now()}",
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
                    getEligibilityUseCase = eligibilityUseCase,
                    autoLoad = true
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
