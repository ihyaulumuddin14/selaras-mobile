package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.local.entity.AppSettingsEntity
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.data.local.entity.CounselorSummaryEntity
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import com.example.selaras.data.local.entity.ScreeningAnswerEntity
import com.example.selaras.data.local.entity.ScreeningResultEntity
import com.example.selaras.data.repository.ScreeningRepositoryImpl
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SelarasDatabaseTest {

    private lateinit var database: SelarasDatabase
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testAllSixTablesExistInDatabase() {
        val cursor = database.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table'"
        )
        val tables = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            tables.add(cursor.getString(0))
        }
        cursor.close()

        val expectedTables = setOf(
            "app_settings",
            "check_ins",
            "screening_results",
            "screening_answers",
            "custom_breathing_patterns",
            "counselor_summaries"
        )

        for (expected in expectedTables) {
            assertTrue("Table $expected must exist in database", tables.contains(expected))
        }
    }

    @Test
    fun testAppSettingsSingletonRule() = runTest {
        val dao = database.appSettingsDao()
        val now = Instant.now()

        val initialSettings = AppSettingsEntity(
            id = AppSettingsEntity.SINGLETON_ID,
            firstActivatedAt = now,
            onboardingCompleted = false,
            consentAcceptedAt = now,
            checkinReminderEnabled = true,
            checkinReminderTime = LocalTime.of(20, 0),
            createdAt = now,
            updatedAt = now
        )

        dao.upsertSettings(initialSettings)
        assertEquals(1, dao.countSettings())

        val retrieved = dao.getSettings()
        assertNotNull(retrieved)
        assertEquals(false, retrieved?.onboardingCompleted)

        // Upsert updated settings with same singleton ID
        val updatedSettings = retrieved!!.copy(
            onboardingCompleted = true,
            updatedAt = Instant.now()
        )
        dao.upsertSettings(updatedSettings)

        // Verify count remains exactly 1
        assertEquals(1, dao.countSettings())
        val finalSettings = dao.getSettings()
        assertEquals(true, finalSettings?.onboardingCompleted)
    }

    @Test
    fun testCheckInUniqueConstraintOnDate() = runTest {
        val dao = database.checkInDao()
        val today = LocalDate.of(2026, 11, 19)
        val now = Instant.now()

        val checkIn1 = CheckInEntity(
            checkInDate = today,
            mood = 4,
            energy = 7,
            sleepDurationMinutes = 450,
            note = "Hari ini produktif",
            createdAt = now,
            updatedAt = now
        )

        val id1 = dao.insertCheckIn(checkIn1)
        assertTrue(id1 > 0)

        val retrieved = dao.getCheckInByDate(today)
        assertNotNull(retrieved)
        assertEquals(4, retrieved?.mood)
        assertEquals("Hari ini produktif", retrieved?.note)

        // Attempt to insert another check-in with the exact same date
        val checkIn2 = CheckInEntity(
            checkInDate = today,
            mood = 2,
            energy = 3,
            sleepDurationMinutes = 300,
            note = "Duplikat",
            createdAt = now,
            updatedAt = now
        )

        try {
            dao.insertCheckIn(checkIn2)
            fail("Expected SQLite constraint exception for duplicate check_in_date")
        } catch (e: Exception) {
            // Expected unique constraint violation
            assertTrue(e.message?.contains("UNIQUE constraint failed") == true || e is android.database.sqlite.SQLiteConstraintException)
        }
    }

    @Test
    fun testScreeningResultCompositeUniqueIndex() = runTest {
        val dao = database.screeningResultDao()
        val startDate = LocalDate.of(2026, 11, 1)
        val endDate = LocalDate.of(2026, 11, 14)
        val now = Instant.now()

        val result1 = ScreeningResultEntity(
            screeningType = ScreeningType.ROUTINE,
            periodStartDate = startDate,
            periodEndDate = endDate,
            instrumentVersion = "PHQ-9 v1.0",
            completedAt = now,
            totalScore = 8,
            category = ScreeningCategory.RINGAN,
            helpIndicator = false,
            createdAt = now
        )

        val id1 = dao.insertScreeningResult(result1)
        assertTrue(id1 > 0)

        // Second result with same type, startDate, and endDate must fail unique constraint
        val resultDuplicate = ScreeningResultEntity(
            screeningType = ScreeningType.ROUTINE,
            periodStartDate = startDate,
            periodEndDate = endDate,
            instrumentVersion = "PHQ-9 v1.0",
            completedAt = now,
            totalScore = 12,
            category = ScreeningCategory.SEDANG,
            helpIndicator = true,
            createdAt = now
        )

        try {
            dao.insertScreeningResult(resultDuplicate)
            fail("Expected SQLite constraint exception for duplicate screening composite index")
        } catch (e: Exception) {
            assertTrue(e.message?.contains("UNIQUE constraint failed") == true || e is android.database.sqlite.SQLiteConstraintException)
        }
    }

    @Test
    fun testScreeningAnswersForeignKeyAndCascadeDelete() = runTest {
        val resultDao = database.screeningResultDao()
        val answerDao = database.screeningAnswerDao()
        val now = Instant.now()

        val screeningResult = ScreeningResultEntity(
            screeningType = ScreeningType.MANUAL,
            periodStartDate = LocalDate.of(2026, 11, 1),
            periodEndDate = LocalDate.of(2026, 11, 14),
            instrumentVersion = "PHQ-9 v1.0",
            completedAt = now,
            totalScore = 5,
            category = ScreeningCategory.RINGAN,
            helpIndicator = false,
            createdAt = now
        )

        val resultId = resultDao.insertScreeningResult(screeningResult)

        // Insert 9 answers
        val answers = (1..9).map { qNum ->
            ScreeningAnswerEntity(
                screeningResultId = resultId,
                questionNumber = qNum,
                answerValue = if (qNum == 1) 2 else 0
            )
        }
        val insertedAnswerIds = answerDao.insertAnswers(answers)
        assertEquals(9, insertedAnswerIds.size)

        val savedAnswers = answerDao.getAnswersByScreeningResultId(resultId)
        assertEquals(9, savedAnswers.size)

        // Cascade delete: deleting the parent screening result should delete all answers
        resultDao.deleteScreeningResultById(resultId)

        val remainingAnswers = answerDao.getAnswersByScreeningResultId(resultId)
        assertEquals(0, remainingAnswers.size)
    }

    @Test
    fun testAtomicScreeningSubmissionWithRepository() = runTest {
        val repository = ScreeningRepositoryImpl(database)
        val now = Instant.now()

        val screeningResult = ScreeningResult(
            screeningType = ScreeningType.ROUTINE,
            periodStartDate = LocalDate.of(2026, 10, 15),
            periodEndDate = LocalDate.of(2026, 10, 28),
            instrumentVersion = "PHQ-9 v1.0",
            completedAt = now,
            totalScore = 17,
            category = ScreeningCategory.SEDANG_BERAT,
            helpIndicator = true,
            createdAt = now
        )

        val validAnswers = (1..9).map { qNum ->
            ScreeningAnswer(
                questionNumber = qNum,
                answerValue = if (qNum == 9) 1 else 2
            )
        }

        // 1. Successful atomic submission
        val submitResult = repository.submitScreening(screeningResult, validAnswers)
        assertTrue(submitResult.isSuccess)
        val resultId = submitResult.getOrThrow()
        assertTrue(resultId > 0)

        val fetched = repository.getScreeningResultById(resultId)
        assertNotNull(fetched)
        assertEquals(17, fetched?.totalScore)
        assertEquals(9, fetched?.answers?.size)

        // 2. Atomic rollback when submission fails (e.g. duplicate period)
        val duplicateResult = screeningResult.copy(id = 0)
        val secondSubmit = repository.submitScreening(duplicateResult, validAnswers)
        assertTrue(secondSubmit.isFailure)

        // Total count in database should still be only 1
        val allResults = repository.getAllScreeningResultsFlow().first()
        assertEquals(1, allResults.size)
    }

    @Test
    fun testCustomBreathingPatternCrud() = runTest {
        val dao = database.customBreathingPatternDao()
        val now = Instant.now()

        val pattern = CustomBreathingPatternEntity(
            name = "Fokus Belajar",
            inhaleDurationSeconds = 4,
            exhaleDurationSeconds = 6,
            createdAt = now,
            updatedAt = now
        )

        val id = dao.insertPattern(pattern)
        assertTrue(id > 0)

        val retrieved = dao.getPatternById(id)
        assertNotNull(retrieved)
        assertEquals("Fokus Belajar", retrieved?.name)
        assertEquals(4, retrieved?.inhaleDurationSeconds)
        assertEquals(6, retrieved?.exhaleDurationSeconds)

        // Update
        val updated = retrieved!!.copy(name = "Ketenangan Ekstrem", exhaleDurationSeconds = 8)
        dao.updatePattern(updated)
        val finalRetrieved = dao.getPatternById(id)
        assertEquals("Ketenangan Ekstrem", finalRetrieved?.name)
        assertEquals(8, finalRetrieved?.exhaleDurationSeconds)

        // Delete
        dao.deletePatternById(id)
        assertNull(dao.getPatternById(id))
    }

    @Test
    fun testCounselorSummaryStorageAndCascade() = runTest {
        val resultDao = database.screeningResultDao()
        val summaryDao = database.counselorSummaryDao()
        val now = Instant.now()

        val screeningResult = ScreeningResultEntity(
            screeningType = ScreeningType.ROUTINE,
            periodStartDate = LocalDate.of(2026, 9, 1),
            periodEndDate = LocalDate.of(2026, 9, 14),
            instrumentVersion = "PHQ-9 v1.0",
            completedAt = now,
            totalScore = 19,
            category = ScreeningCategory.BERAT,
            helpIndicator = true,
            createdAt = now
        )

        val screeningId = resultDao.insertScreeningResult(screeningResult)

        val summaryJson = """
            {
               "summary_version": "1.0",
               "score": 19,
               "category": "berat",
               "help_indicator": true,
               "recommendations": ["Konseling segera", "Hubungi layanan krisis"]
            }
        """.trimIndent()

        val summary = CounselorSummaryEntity(
            screeningResultId = screeningId,
            periodStartDate = LocalDate.of(2026, 9, 1),
            periodEndDate = LocalDate.of(2026, 9, 14),
            includeCheckinAnalysis = true,
            checkinDaysCount = 12,
            ruleVersion = "1.0",
            snapshotJson = summaryJson,
            createdAt = now
        )

        val summaryId = summaryDao.insertSummary(summary)
        assertTrue(summaryId > 0)

        val fetchedSummary = summaryDao.getSummaryByScreeningResultId(screeningId)
        assertNotNull(fetchedSummary)
        assertEquals(12, fetchedSummary?.checkinDaysCount)
        assertEquals(summaryJson, fetchedSummary?.snapshotJson)

        // Cascade delete parent screening
        resultDao.deleteScreeningResultById(screeningId)
        assertNull(summaryDao.getSummaryById(summaryId))
    }
}