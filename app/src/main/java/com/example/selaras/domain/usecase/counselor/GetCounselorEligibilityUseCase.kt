@file:Suppress("NewApi")

package com.example.selaras.domain.usecase.counselor

import com.example.selaras.domain.model.CounselorEligibility
import com.example.selaras.domain.repository.CheckInRepository
import java.time.LocalDate

/**
 * Checks whether the user has enough check-in data to create a counselor summary.
 * Business Rule K02: minimum 4 unique check-in days required in the period.
 */
class GetCounselorEligibilityUseCase(
    private val checkInRepository: CheckInRepository
) {
    suspend operator fun invoke(
        startDate: LocalDate,
        endDate: LocalDate
    ): CounselorEligibility {
        val checkIns = checkInRepository.getCheckInsBetween(startDate, endDate)
        val recordedDates = checkIns.map { it.checkInDate }.distinct().sorted()

        val allDates = mutableListOf<LocalDate>()
        var current = startDate
        while (!current.isAfter(endDate)) {
            allDates.add(current)
            current = current.plusDays(1)
        }

        val missingDates = allDates.filter { it !in recordedDates }

        return CounselorEligibility(
            periodStartDate = startDate,
            periodEndDate = endDate,
            totalDaysInPeriod = allDates.size,
            recordedDays = recordedDates.size,
            recordedDates = recordedDates,
            missingDates = missingDates,
            isEligible = recordedDates.size >= 4
        )
    }
}
