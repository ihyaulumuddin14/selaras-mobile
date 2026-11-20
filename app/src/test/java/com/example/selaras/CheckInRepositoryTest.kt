package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.repository.CheckInRepositoryImpl
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CheckInRepositoryTest {

    private lateinit var database: SelarasDatabase
    private lateinit var localDataSource: CheckInLocalDataSource
    private lateinit var repository: CheckInRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = CheckInLocalDataSource(database.checkInDao())
        repository = CheckInRepositoryImpl(localDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndGetCheckInByDate() = runTest {
        val today = LocalDate.of(2026, 11, 21)
        val checkIn = CheckIn(
            checkInDate = today,
            mood = 4,
            energy = 8,
            sleepDurationMinutes = 480,
            note = "Hari ini sangat berenergi"
        )

        val result = repository.saveCheckIn(checkIn)
        assertTrue(result.isSuccess)
        val saved = result.getOrThrow()
        assertTrue(saved.id > 0)
        assertTrue(saved.uuid.isNotBlank())

        val retrieved = repository.getCheckInByDate(today)
        assertNotNull(retrieved)
        assertEquals(4, retrieved?.mood)
        assertEquals(8, retrieved?.energy)
        assertEquals(480, retrieved?.sleepDurationMinutes)
        assertEquals("Hari ini sangat berenergi", retrieved?.note)
        assertEquals("Baik", retrieved?.moodLabel)
        assertEquals(8, retrieved?.sleepHours)

        // Verify offline-first metadata in local entity
        val rawEntity = localDataSource.getCheckInById(saved.id)
        assertNotNull(rawEntity)
        assertEquals("PENDING_CREATE", rawEntity?.syncStatus)
        assertFalse(rawEntity?.isDeleted ?: true)
    }

    @Test
    fun testUpdateCheckInSetsPendingUpdate() = runTest {
        val date = LocalDate.of(2026, 11, 20)
        val saved = repository.saveCheckIn(
            CheckIn(checkInDate = date, mood = 3, energy = 5, sleepDurationMinutes = 420)
        ).getOrThrow()

        val updated = saved.copy(mood = 5, energy = 9, note = "Pembaruan sore")
        val updateResult = repository.updateCheckIn(updated)
        assertTrue(updateResult.isSuccess)

        val retrieved = repository.getCheckInByDate(date)
        assertEquals(5, retrieved?.mood)
        assertEquals(9, retrieved?.energy)
        assertEquals("Pembaruan sore", retrieved?.note)

        val rawEntity = localDataSource.getCheckInById(saved.id)
        assertEquals("PENDING_UPDATE", rawEntity?.syncStatus)
    }

    @Test
    fun testSoftDeletePreservesRowForSyncEngine() = runTest {
        val date = LocalDate.of(2026, 11, 19)
        val saved = repository.saveCheckIn(
            CheckIn(checkInDate = date, mood = 2, energy = 4, sleepDurationMinutes = 360)
        ).getOrThrow()

        val deleteResult = repository.deleteCheckInById(saved.id)
        assertTrue(deleteResult.isSuccess)

        // Active queries return null / empty
        assertNull(repository.getCheckInByDate(date))
        val allFlow = repository.getAllCheckInsFlow().first()
        assertTrue(allFlow.none { it.id == saved.id })

        // Raw local datasource retains row with is_deleted=true and PENDING_DELETE
        val pendingSync = localDataSource.getPendingSyncCheckIns()
        assertEquals(1, pendingSync.size)
        assertEquals(saved.id, pendingSync[0].id)
        assertTrue(pendingSync[0].isDeleted)
        assertEquals("PENDING_DELETE", pendingSync[0].syncStatus)
    }

    @Test
    fun testGetCheckInsBetweenRange() = runTest {
        val start = LocalDate.of(2026, 11, 10)
        val end = LocalDate.of(2026, 11, 16)

        for (i in 0..6) {
            val date = start.plusDays(i.toLong())
            repository.saveCheckIn(
                CheckIn(checkInDate = date, mood = 3 + (i % 3), energy = 6 + (i % 4), sleepDurationMinutes = 420 + i * 10)
            )
        }

        val list = repository.getCheckInsBetween(start, end)
        assertEquals(7, list.size)
        val count = repository.countCheckInsBetween(start, end)
        assertEquals(7, count)
    }
}
