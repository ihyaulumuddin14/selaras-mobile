
package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningQuotaStatus
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

class GetScreeningQuotaUseCase(
    private val screeningRepository: ScreeningRepository,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    suspend operator fun invoke(
        referenceDate: LocalDate? = null
    ): ScreeningQuotaStatus {

        val today = referenceDate ?: LocalDate.now(zoneId)

        val weekStartDate = today.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        )

        val weekEndExclusive = weekStartDate.plusWeeks(1)

        val startInstant = weekStartDate
            .atStartOfDay(zoneId)
            .toInstant()

        val endInstant = weekEndExclusive
            .atStartOfDay(zoneId)
            .toInstant()

        val manualUsed =
            screeningRepository.countScreeningResultsByTypeBetween(
                ScreeningType.MANUAL,
                startInstant,
                endInstant
            ) > 0

        val routineUsed =
            screeningRepository.countScreeningResultsByTypeBetween(
                ScreeningType.ROUTINE,
                startInstant,
                endInstant
            ) > 0

        return ScreeningQuotaStatus(
            weekStartDate = weekStartDate,
            weekEndDate = weekEndExclusive.minusDays(1),
            manualUsed = manualUsed,
            routineUsed = routineUsed
        )
    }
}
