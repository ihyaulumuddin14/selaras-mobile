package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.datasource.remote.FakeBreathingRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.BreathingPatternNetworkDto
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import com.example.selaras.data.sync.BreathingSyncManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class BreathingSyncManagerTest {

    private lateinit var database: SelarasDatabase
    private lateinit var localDataSource: BreathingLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeBreathingRemoteDataSource
    private lateinit var syncManager: BreathingSyncManager

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = BreathingLocalDataSource(database.customBreathingPatternDao())
        fakeRemoteDataSource = FakeBreathingRemoteDataSource()
        syncManager = BreathingSyncManager(localDataSource, fakeRemoteDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPushPendingPatternSyncsToRemoteAndUpdatesLocalStatus() = runTest {
        // Given: Offline custom pattern created locally with PENDING_CREATE
        val localPattern = CustomBreathingPatternEntity(
            uuid = "uuid-pattern-1",
            name = "Pola Relaksasi Malam",
            inhaleDurationSeconds = 4,
            exhaleDurationSeconds = 8,
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertPattern(localPattern)

        // When: Sync is executed
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.pushedCount)
        assertEquals(0, result.deletedCount)

        // Remote now has the pattern
        val remoteItems = fakeRemoteDataSource.getRemoteItems()
        assertEquals(1, remoteItems.size)
        assertEquals("uuid-pattern-1", remoteItems.first().uuid)
        assertEquals("Pola Relaksasi Malam", remoteItems.first().name)

        // Local status updated to SYNCED
        val updatedLocal = localDataSource.getPatternById(id)
        assertNotNull(updatedLocal)
        assertEquals("SYNCED", updatedLocal?.syncStatus)
    }

    @Test
    fun testPushPendingDeleteSyncsToRemoteAndCleansUpLocalTombstone() = runTest {
        // Given: Pattern soft-deleted locally
        val localPattern = CustomBreathingPatternEntity(
            uuid = "uuid-pattern-to-delete",
            name = "Pola Dihapus",
            inhaleDurationSeconds = 5,
            exhaleDurationSeconds = 5,
            syncStatus = "PENDING_DELETE",
            isDeleted = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertPattern(localPattern)
        fakeRemoteDataSource.seedRemote(
            listOf(
                BreathingPatternNetworkDto(
                    uuid = "uuid-pattern-to-delete",
                    name = "Pola Dihapus",
                    inhaleDurationSeconds = 5,
                    exhaleDurationSeconds = 5,
                    createdAt = Instant.now().toString(),
                    updatedAt = Instant.now().toString()
                )
            )
        )

        // When: Sync runs
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.deletedCount)

        // Removed from remote
        assertTrue(fakeRemoteDataSource.getRemoteItems().isEmpty())

        // Purged from local database
        val localAfter = localDataSource.getPatternById(id)
        assertNull(localAfter)
    }

    @Test
    fun testPullNewPatternsFromRemoteInsertsToLocalAsSynced() = runTest {
        // Given: New pattern saved on web/another device
        val remotePattern = BreathingPatternNetworkDto(
            uuid = "uuid-from-remote",
            name = "Pola Meditasi Pagi",
            inhaleDurationSeconds = 6,
            exhaleDurationSeconds = 6,
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        fakeRemoteDataSource.seedRemote(listOf(remotePattern))

        // When: Sync runs
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.pulledCount)

        val localRecord = localDataSource.getPatternByUuid("uuid-from-remote")
        assertNotNull(localRecord)
        assertEquals("SYNCED", localRecord?.syncStatus)
        assertEquals("Pola Meditasi Pagi", localRecord?.name)
        assertEquals(6, localRecord?.inhaleDurationSeconds)
        assertEquals(6, localRecord?.exhaleDurationSeconds)
    }

    @Test
    fun testSyncFailsGracefullyOnNetworkErrorPreservingPendingLocalState() = runTest {
        val localPattern = CustomBreathingPatternEntity(
            uuid = "uuid-pattern-offline",
            name = "Pola Offline",
            inhaleDurationSeconds = 4,
            exhaleDurationSeconds = 4,
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertPattern(localPattern)
        fakeRemoteDataSource.shouldFail = true

        val result = syncManager.sync()

        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)

        val localStillPending = localDataSource.getPatternById(id)
        assertNotNull(localStillPending)
        assertEquals("PENDING_CREATE", localStillPending?.syncStatus)
    }
}
