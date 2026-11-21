@file:Suppress("NewApi")

package com.example.selaras.domain.usecase.counselor

import com.example.selaras.domain.model.CounselorSummary
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.repository.CheckInRepository
import com.example.selaras.domain.repository.CounselorRepository
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.Instant
import java.time.LocalDate

/**
 * Creates a counselor summary from check-in data and optional screening results and notes.
 * Enforces Business Rule K02: Minimum 4 unique check-in dates required.
 */
class CreateCounselorSummaryUseCase(
    private val checkInRepository: CheckInRepository,
    private val screeningRepository: ScreeningRepository,
    private val counselorRepository: CounselorRepository
) {
    suspend operator fun invoke(
        periodStartDate: LocalDate,
        periodEndDate: LocalDate,
        selectedScreeningIds: List<Long> = emptyList(),
        selectedNoteDates: List<LocalDate> = emptyList(),
        includeNotes: Boolean = false,
        ruleVersion: String = "T06"
    ): Result<CounselorSummary> = runCatching {
        val checkIns = checkInRepository.getCheckInsBetween(periodStartDate, periodEndDate)
        val recordedDates = checkIns.map { it.checkInDate }.distinct().sorted()

        if (recordedDates.size < 4) {
            throw IllegalStateException(
                "Minimal 4 tanggal check-in valid yang berbeda untuk membuat ringkasan. Saat ini hanya ${recordedDates.size} hari."
            )
        }

        // Calculate averages from valid days (not divided by 7)
        val avgMood = if (checkIns.isNotEmpty()) checkIns.map { it.mood }.average() else 0.0
        val avgSleepMinutes = if (checkIns.isNotEmpty()) checkIns.map { it.sleepDurationMinutes }.average() else 0.0
        val avgSleepHours = avgSleepMinutes / 60.0
        val avgEnergy = if (checkIns.isNotEmpty()) checkIns.map { it.energy }.average() else 0.0

        // Fetch selected screening results
        val selectedScreenings = mutableListOf<ScreeningResult>()
        for (id in selectedScreeningIds) {
            screeningRepository.getScreeningResultById(id)?.let {
                selectedScreenings.add(it)
            }
        }

        val dailyJson = checkIns.sortedBy { it.checkInDate }.joinToString(",") { ci ->
            val noteStr = if (includeNotes && (selectedNoteDates.isEmpty() || ci.checkInDate in selectedNoteDates)) {
                ci.note ?: ""
            } else ""
            """{"date":"${ci.checkInDate}","mood":${ci.mood},"sleepHours":${ci.sleepDurationMinutes / 60.0},"energy":${ci.energy},"note":"$noteStr"}"""
        }

        val screeningsJson = selectedScreenings.joinToString(",") { sc ->
            """{"id":${sc.id},"type":"${sc.screeningType.name}","date":"${sc.completedAt}","score":${sc.totalScore},"category":"${sc.category.name}","instrumentVersion":"${sc.instrumentVersion}"}"""
        }

        val recordedDatesJson = recordedDates.joinToString(",") { "\"$it\"" }

        val snapshotJson = """
        {
            "snapshotId": "S-${System.currentTimeMillis().toString().takeLast(4)}",
            "createdAt": "${Instant.now()}",
            "periodStartDate": "$periodStartDate",
            "periodEndDate": "$periodEndDate",
            "ruleVersion": "$ruleVersion",
            "recordedDaysCount": ${recordedDates.size},
            "recordedDates": [$recordedDatesJson],
            "avgMood": $avgMood,
            "avgSleepHours": $avgSleepHours,
            "avgEnergy": $avgEnergy,
            "dailyEntries": [$dailyJson],
            "screenings": [$screeningsJson],
            "includeNotes": $includeNotes
        }
        """.trimIndent()

        val summary = CounselorSummary(
            screeningResultId = selectedScreenings.firstOrNull()?.id,
            periodStartDate = periodStartDate,
            periodEndDate = periodEndDate,
            includeCheckinAnalysis = true,
            checkinDaysCount = recordedDates.size,
            ruleVersion = ruleVersion,
            snapshotJson = snapshotJson,
            createdAt = Instant.now()
        )

        counselorRepository.createSummary(summary).getOrThrow()
    }
}
