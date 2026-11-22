package com.example.selaras.domain.usecase.screening

import com.example.selaras.domain.model.AppSettings
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GetScreeningQuotaUseCaseTest {

    private val zoneId = ZoneId.of("Asia/Jakarta")

    @Test
    fun `routine screening is disabled before ready window at end of week`() = runBlocking {
        val activationDate = LocalDate.of(2026, 11, 1)
        val activationInstant = activationDate.atStartOfDay(zoneId).toInstant()

        val fakeSettings = FakeAppSettingsRepo(
            AppSettings(firstActivatedAt = activationInstant)
        )
        val fakeScreening = FakeScreeningRepo()

        val useCase = GetScreeningQuotaUseCase(
            screeningRepository = fakeScreening,
            appSettingsRepository = fakeSettings,
            zoneId = zoneId
        )

        // Day 2 since activation (Nov 3): Before ready window (Nov 6 - Nov 7)
        val quota = useCase(referenceDate = LocalDate.of(2026, 11, 3))

        assertEquals(LocalDate.of(2026, 11, 1), quota.weekStartDate)
        assertEquals(LocalDate.of(2026, 11, 7), quota.weekEndDate)
        assertEquals(LocalDate.of(2026, 11, 6), quota.routineAvailableFrom)
        assertEquals(LocalDate.of(2026, 11, 7), quota.routineAvailableUntil)

        assertFalse("Routine should not be available before ready window", quota.routineAvailable)
        assertFalse("Routine window should be closed", quota.isRoutineWindowOpen)
        assertEquals(0, quota.routineRemaining)
        assertTrue("Manual should be available", quota.manualAvailable)
    }

    @Test
    fun `routine screening is enabled during ready window at end of week`() = runBlocking {
        val activationDate = LocalDate.of(2026, 11, 1)
        val activationInstant = activationDate.atStartOfDay(zoneId).toInstant()

        val fakeSettings = FakeAppSettingsRepo(
            AppSettings(firstActivatedAt = activationInstant)
        )
        val fakeScreening = FakeScreeningRepo()

        val useCase = GetScreeningQuotaUseCase(
            screeningRepository = fakeScreening,
            appSettingsRepository = fakeSettings,
            zoneId = zoneId
        )

        // Day 5 since activation (Nov 6): Start of ready window
        val quotaDay5 = useCase(referenceDate = LocalDate.of(2026, 11, 6))
        assertTrue("Routine should be available on day 5", quotaDay5.routineAvailable)
        assertTrue("Routine window should be open", quotaDay5.isRoutineWindowOpen)
        assertEquals(1, quotaDay5.routineRemaining)

        // Day 6 since activation (Nov 7): End of ready window
        val quotaDay6 = useCase(referenceDate = LocalDate.of(2026, 11, 7))
        assertTrue("Routine should be available on day 6", quotaDay6.routineAvailable)
        assertTrue("Routine window should be open", quotaDay6.isRoutineWindowOpen)
        assertEquals(1, quotaDay6.routineRemaining)
    }

    @Test
    fun `routine screening is disabled once used in that period`() = runBlocking {
        val activationDate = LocalDate.of(2026, 11, 1)
        val activationInstant = activationDate.atStartOfDay(zoneId).toInstant()

        val fakeSettings = FakeAppSettingsRepo(
            AppSettings(firstActivatedAt = activationInstant)
        )
        val fakeScreening = FakeScreeningRepo()

        val useCase = GetScreeningQuotaUseCase(
            screeningRepository = fakeScreening,
            appSettingsRepository = fakeSettings,
            zoneId = zoneId
        )

        // Seed a routine screening on Nov 6
        fakeScreening.submitScreening(
            ScreeningResult(
                id = 1L,
                screeningType = ScreeningType.ROUTINE,
                periodStartDate = LocalDate.of(2026, 11, 1),
                periodEndDate = LocalDate.of(2026, 11, 7),
                instrumentVersion = "PHQ-9",
                completedAt = LocalDate.of(2026, 11, 6).atTime(10, 0).atZone(zoneId).toInstant(),
                totalScore = 5,
                category = ScreeningCategory.MINIMAL,
                helpIndicator = false
            ),
            emptyList()
        )

        val quota = useCase(referenceDate = LocalDate.of(2026, 11, 6))
        assertTrue("Routine should be marked as used", quota.routineUsed)
        assertFalse("Routine should not be available after being used", quota.routineAvailable)
        assertEquals(0, quota.routineRemaining)
        assertTrue("Manual should still be available", quota.manualAvailable)
    }

    @Test
    fun `subsequent cycle calculates new period correctly`() = runBlocking {
        val activationDate = LocalDate.of(2026, 11, 1)
        val activationInstant = activationDate.atStartOfDay(zoneId).toInstant()

        val fakeSettings = FakeAppSettingsRepo(
            AppSettings(firstActivatedAt = activationInstant)
        )
        val fakeScreening = FakeScreeningRepo()

        val useCase = GetScreeningQuotaUseCase(
            screeningRepository = fakeScreening,
            appSettingsRepository = fakeSettings,
            zoneId = zoneId
        )

        // Day 10 since activation (Nov 11) -> Cycle 2: Nov 8 to Nov 14
        val quota = useCase(referenceDate = LocalDate.of(2026, 11, 11))

        assertEquals(LocalDate.of(2026, 11, 8), quota.weekStartDate)
        assertEquals(LocalDate.of(2026, 11, 14), quota.weekEndDate)
        assertEquals(LocalDate.of(2026, 11, 13), quota.routineAvailableFrom)
        assertEquals(LocalDate.of(2026, 11, 14), quota.routineAvailableUntil)

        assertFalse("Routine should be disabled before window in cycle 2", quota.routineAvailable)
    }

    private class FakeAppSettingsRepo(var settings: AppSettings? = null) : AppSettingsRepository {
        override fun getSettingsFlow(): Flow<AppSettings?> = flowOf(settings)
        override suspend fun getSettings(): AppSettings? = settings
        override suspend fun initializeIfFirstLaunch(): Result<AppSettings> = Result.success(settings!!)
        override suspend fun acceptConsent(): Result<Unit> = Result.success(Unit)
        override suspend fun completeOnboarding(): Result<Unit> = Result.success(Unit)
        override suspend fun setCheckInReminder(enabled: Boolean, time: LocalTime?): Result<Unit> = Result.success(Unit)
        override suspend fun updateUserName(name: String?): Result<Unit> = Result.success(Unit)
    }

    private class FakeScreeningRepo : ScreeningRepository {
        val results = mutableListOf<ScreeningResult>()

        override suspend fun submitScreening(
            screeningResult: ScreeningResult,
            answers: List<ScreeningAnswer>
        ): Result<Long> {
            results.add(screeningResult)
            return Result.success(screeningResult.id)
        }

        override suspend fun getScreeningResultById(id: Long): ScreeningResult? = results.find { it.id == id }
        override suspend fun getScreeningResultByPeriod(
            screeningType: ScreeningType,
            startDate: LocalDate,
            endDate: LocalDate
        ): ScreeningResult? = null

        override fun getAllScreeningResultsFlow(): Flow<List<ScreeningResult>> = flowOf(results)
        override suspend fun getLatestScreeningResult(): ScreeningResult? = results.lastOrNull()
        override fun getLatestScreeningResultFlow(): Flow<ScreeningResult?> = flowOf(results.lastOrNull())
        override suspend fun getLatestRoutineScreeningResult(): ScreeningResult? =
            results.lastOrNull { it.screeningType == ScreeningType.ROUTINE }

        override suspend fun getAnswersByScreeningResultId(screeningResultId: Long) = emptyList<ScreeningAnswer>()
        override suspend fun getScreeningTypesUsedThisWeek(referenceDate: LocalDate) = emptySet<ScreeningType>()

        override suspend fun countScreeningResultsByTypeBetween(
            screeningType: ScreeningType,
            startInclusive: Instant,
            endExclusive: Instant
        ): Int {
            return results.count {
                it.screeningType == screeningType &&
                    !it.completedAt.isBefore(startInclusive) &&
                    it.completedAt.isBefore(endExclusive)
            }
        }
    }
}
