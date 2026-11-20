package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.repository.BreathingRepositoryImpl
import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BreathingRepositoryTest {

    private lateinit var database: SelarasDatabase
    private lateinit var localDataSource: BreathingLocalDataSource
    private lateinit var repository: BreathingRepository

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = BreathingLocalDataSource(database.customBreathingPatternDao())
        repository = BreathingRepositoryImpl(localDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testSaveAndObserveCustomPattern() = runTest {
        val pattern = BreathingPattern(
            key = "custom:0",
            name = "Fokus Dalam",
            inhaleSeconds = 4,
            exhaleSeconds = 6
        )

        val saveResult = repository.saveCustomPattern(pattern)
        assertTrue(saveResult.isSuccess)
        val saved = saveResult.getOrThrow()
        assertTrue(saved.id > 0)
        assertEquals("custom:${saved.id}", saved.key)
        assertNotNull(saved.uuid)

        val list = repository.observeCustomPatterns().first()
        assertEquals(1, list.size)
        assertEquals("Fokus Dalam", list[0].name)
        assertEquals(4, list[0].inhaleSeconds)
        assertEquals(6, list[0].exhaleSeconds)

        // Verify entity in local database has offline-first sync metadata
        val rawEntity = localDataSource.getPatternById(saved.id)
        assertNotNull(rawEntity)
        assertEquals("PENDING_CREATE", rawEntity?.syncStatus)
        assertFalse(rawEntity?.isDeleted ?: true)
    }

    @Test
    fun testUpdateCustomPatternSetsPendingUpdate() = runTest {
        val saved = repository.saveCustomPattern(
            BreathingPattern(key = "custom:0", name = "Pola Awal", inhaleSeconds = 4, exhaleSeconds = 4)
        ).getOrThrow()

        val toUpdate = saved.copy(name = "Pola Baru", inhaleSeconds = 5, exhaleSeconds = 7)
        val updateResult = repository.updateCustomPattern(toUpdate)
        assertTrue(updateResult.isSuccess)

        val retrieved = repository.getPatternById(saved.id)
        assertEquals("Pola Baru", retrieved?.name)
        assertEquals(5, retrieved?.inhaleSeconds)
        assertEquals(7, retrieved?.exhaleSeconds)

        val rawEntity = localDataSource.getPatternById(saved.id)
        assertEquals("PENDING_UPDATE", rawEntity?.syncStatus)
    }

    @Test
    fun testSoftDeletePreservesRowForSyncEngine() = runTest {
        val saved = repository.saveCustomPattern(
            BreathingPattern(key = "custom:0", name = "Akan Dihapus", inhaleSeconds = 3, exhaleSeconds = 5)
        ).getOrThrow()

        // Soft delete via repository
        val deleteResult = repository.deleteCustomPatternById(saved.id)
        assertTrue(deleteResult.isSuccess)

        // Active query should not return the deleted pattern
        val activePatterns = repository.observeCustomPatterns().first()
        assertTrue(activePatterns.isEmpty())
        assertNull(repository.getPatternById(saved.id))

        // Raw local datasource still retains record marked with is_deleted=1 and PENDING_DELETE
        val pendingSync = localDataSource.getPendingSyncPatterns()
        assertEquals(1, pendingSync.size)
        assertEquals(saved.id, pendingSync[0].id)
        assertTrue(pendingSync[0].isDeleted)
        assertEquals("PENDING_DELETE", pendingSync[0].syncStatus)
    }
}
