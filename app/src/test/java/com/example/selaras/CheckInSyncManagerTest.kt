package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.datasource.remote.FakeCheckInRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.CheckInNetworkDto
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.data.sync.CheckInSyncManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CheckInSyncManagerTest {

    private lateinit var database: SelarasDatabase
    private lateinit var localDataSource: CheckInLocalDataSource
    private lateinit var fakeRemoteDataSource: FakeCheckInRemoteDataSource
    private lateinit var syncManager: CheckInSyncManager

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        localDataSource = CheckInLocalDataSource(database.checkInDao())
        fakeRemoteDataSource = FakeCheckInRemoteDataSource()
        syncManager = CheckInSyncManager(localDataSource, fakeRemoteDataSource)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testPushPendingCreateSyncsToRemoteAndUpdatesLocalStatus() = runTest {
        // Given: Offline check-in saved locally with PENDING_CREATE
        val localCheckIn = CheckInEntity(
            uuid = "uuid-checkin-1",
            checkInDate = LocalDate.of(2026, 11, 21),
            mood = 4,
            energy = 8,
            sleepDurationMinutes = 480,
            note = "Check-in offline",
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertCheckIn(localCheckIn)

        // When: Sync is triggered
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.pushedCount)
        assertEquals(0, result.deletedCount)

        // Remote now has the item
        val remoteItems = fakeRemoteDataSource.getRemoteItems()
        assertEquals(1, remoteItems.size)
        assertEquals("uuid-checkin-1", remoteItems.first().uuid)

        // Local record is now SYNCED
        val updatedLocal = localDataSource.getCheckInById(id)
        assertNotNull(updatedLocal)
        assertEquals("SYNCED", updatedLocal?.syncStatus)
    }

    @Test
    fun testPushPendingDeleteSyncsToRemoteAndCleansUpLocalTombstone() = runTest {
        // Given: Record that was previously synced, then deleted offline (soft deleted)
        val localCheckIn = CheckInEntity(
            uuid = "uuid-checkin-to-delete",
            checkInDate = LocalDate.of(2026, 11, 20),
            mood = 3,
            energy = 5,
            sleepDurationMinutes = 420,
            syncStatus = "PENDING_DELETE",
            isDeleted = true,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertCheckIn(localCheckIn)
        // Also simulate that remote already had this record
        fakeRemoteDataSource.seedRemote(
            listOf(
                CheckInNetworkDto(
                    uuid = "uuid-checkin-to-delete",
                    checkInDate = "2026-11-20",
                    mood = 3,
                    energy = 5,
                    sleepDurationMinutes = 420,
                    createdAt = Instant.now().toString(),
                    updatedAt = Instant.now().toString()
                )
            )
        )

        // When: Sync is executed
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.deletedCount)

        // Remote item removed
        assertTrue(fakeRemoteDataSource.getRemoteItems().isEmpty())

        // Local tombstone cleaned up
        val localAfter = localDataSource.getCheckInById(id)
        assertNull(localAfter)
    }

    @Test
    fun testPullNewChangesFromRemoteInsertsToLocalAsSynced() = runTest {
        // Given: Remote server has a check-in created from another device/web
        val remoteItem = CheckInNetworkDto(
            uuid = "uuid-from-web",
            checkInDate = "2026-11-19",
            mood = 5,
            energy = 9,
            sleepDurationMinutes = 510,
            note = "Synced from web dashboard",
            createdAt = Instant.now().toString(),
            updatedAt = Instant.now().toString()
        )
        fakeRemoteDataSource.seedRemote(listOf(remoteItem))

        // When: Sync is executed
        val result = syncManager.sync()

        // Then:
        assertTrue(result.isSuccess)
        assertEquals(1, result.pulledCount)

        val localRecord = localDataSource.getCheckInByUuid("uuid-from-web")
        assertNotNull(localRecord)
        assertEquals("SYNCED", localRecord?.syncStatus)
        assertEquals(5, localRecord?.mood)
        assertEquals(9, localRecord?.energy)
        assertEquals(LocalDate.of(2026, 11, 19), localRecord?.checkInDate)
    }

    @Test
    fun testSyncFailsGracefullyOnNetworkErrorPreservingPendingLocalState() = runTest {
        // Given: Offline record and network error simulated
        val localCheckIn = CheckInEntity(
            uuid = "uuid-checkin-error",
            checkInDate = LocalDate.of(2026, 11, 21),
            mood = 4,
            energy = 7,
            sleepDurationMinutes = 450,
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        val id = localDataSource.insertCheckIn(localCheckIn)
        fakeRemoteDataSource.shouldFail = true

        // When: Sync is attempted during network outage
        val result = syncManager.sync()

        // Then:
        assertFalse(result.isSuccess)
        assertNotNull(result.errorMessage)

        // Local state is preserved as PENDING_CREATE for subsequent retry
        val localStillPending = localDataSource.getCheckInById(id)
        assertNotNull(localStillPending)
        assertEquals("PENDING_CREATE", localStillPending?.syncStatus)
    }
}
