package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.ScreeningQuotaStatus
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

class GetScreeningQuotaUseCase(
    private val screeningRepository: ScreeningRepository,
    private val appSettingsRepository: AppSettingsRepository? = null,
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {

    suspend operator fun invoke(
        referenceDate: LocalDate? = null
    ): ScreeningQuotaStatus {
        val today = referenceDate ?: LocalDate.now(zoneId)

        val settings = appSettingsRepository?.getSettings()
        val activationDate = settings?.firstActivatedAt
            ?.atZone(zoneId)
            ?.toLocalDate()
            ?: today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, today)
        val cycleStartDate: LocalDate
        val cycleEndDate: LocalDate

        if (daysSinceActivation >= 0) {
            val cycleIndex = daysSinceActivation / 7
            cycleStartDate = activationDate.plusDays(cycleIndex * 7)
            cycleEndDate = cycleStartDate.plusDays(6)
        } else {
            cycleStartDate = activationDate
            cycleEndDate = cycleStartDate.plusDays(6)
        }

        val startInstant = cycleStartDate
            .atStartOfDay(zoneId)
            .toInstant()

        val endExclusiveInstant = cycleEndDate
            .plusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()

        val manualUsed = screeningRepository.countScreeningResultsByTypeBetween(
            ScreeningType.MANUAL,
            startInstant,
            endExclusiveInstant
        ) > 0

        val routineUsed = screeningRepository.countScreeningResultsByTypeBetween(
            ScreeningType.ROUTINE,
            startInstant,
            endExclusiveInstant
        ) > 0

        // Routine window: akhir periode minggu (hari ke-6 dan ke-7 dari siklus 7 hari)
        val routineAvailableFrom = cycleStartDate.plusDays(5)
        val routineAvailableUntil = cycleEndDate

        val isRoutineWindowOpen = if (appSettingsRepository == null) {
            true
        } else {
            !today.isBefore(routineAvailableFrom) && !today.isAfter(routineAvailableUntil)
        }

        return ScreeningQuotaStatus(
            weekStartDate = cycleStartDate,
            weekEndDate = cycleEndDate,
            manualUsed = manualUsed,
            routineUsed = routineUsed,
            isRoutineWindowOpen = isRoutineWindowOpen,
            routineAvailableFrom = routineAvailableFrom,
            routineAvailableUntil = routineAvailableUntil
        )
    }
}
