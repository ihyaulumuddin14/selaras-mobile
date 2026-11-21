@file:Suppress("NewApi")

package com.example.selaras.domain.model

import java.time.Instant
import java.time.LocalDate

data class CounselorSummary(
    val id: Long = 0,
    val screeningResultId: Long? = null,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val includeCheckinAnalysis: Boolean = false,
    val checkinDaysCount: Int = 0,
    val ruleVersion: String? = null,
    val snapshotJson: String,
    val createdAt: Instant = Instant.now()
)

/**
 * Eligibility data for creating a counselor summary.
 * Business Rule K02: minimum 4 unique check-in days required.
 */
data class CounselorEligibility(
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val totalDaysInPeriod: Int,
    val recordedDays: Int,
    val recordedDates: List<LocalDate>,
    val missingDates: List<LocalDate>,
    val isEligible: Boolean
) {
    val daysNeeded: Int
        get() = (4 - recordedDays).coerceAtLeast(0)
}
