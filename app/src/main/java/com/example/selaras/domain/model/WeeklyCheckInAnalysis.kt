package com.example.selaras.domain.model

import java.time.LocalDate

enum class MoodTrend {
    IMPROVING,
    STABLE,
    DECLINING,
    INSUFFICIENT_DATA
}

data class WeeklyCheckInAnalysis(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val entries: List<CheckIn>,
    val totalDaysRecorded: Int,
    val averageMood: Double,
    val averageEnergy: Double,
    val averageSleepMinutes: Int,
    val moodTrend: MoodTrend,
    val isEligibleForCounselorSummary: Boolean,
    val insights: List<String>
) {
    val averageSleepHours: Double
        get() = (averageSleepMinutes / 60.0 * 10.0).toInt() / 10.0

    val hasData: Boolean
        get() = totalDaysRecorded > 0
}
