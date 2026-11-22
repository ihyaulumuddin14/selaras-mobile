package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.core.security.AuthSessionManager
import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.datasource.remote.BreathingRemoteDataSource
import com.example.selaras.data.datasource.remote.CheckInRemoteDataSource
import com.example.selaras.data.datasource.remote.ScreeningRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.*
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.data.sync.BreathingSyncManager
import com.example.selaras.data.sync.CheckInSyncManager
import com.example.selaras.data.sync.ScreeningSyncManager
import com.example.selaras.data.sync.SyncCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SyncCoordinatorTest {

    private lateinit var context: Context
    private lateinit var database: SelarasDatabase
    private lateinit var authSessionManager: AuthSessionManager
    private lateinit var syncCoordinator: SyncCoordinator

    private var fakeCheckInPushedCount = 0
    private var fakeCheckInPulledCount = 0

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        authSessionManager = AuthSessionManager(context)
        authSessionManager.clearSession()

        val checkInLocal = CheckInLocalDataSource(database.checkInDao())
        val fakeCheckInRemote = object : CheckInRemoteDataSource {
            override suspend fun pushBatch(request: SyncCheckInBatchRequestDto): Result<SyncCheckInBatchResponseDto> {
                fakeCheckInPushedCount += request.items.size
                return Result.success(
                    SyncCheckInBatchResponseDto(
                        serverTimestamp = Instant.now().toString(),
                        syncedUuids = request.items.map { it.uuid }
                    )
                )
            }

            override suspend fun fetchChangesSince(since: Instant): Result<List<CheckInNetworkDto>> {
                fakeCheckInPulledCount++
                return Result.success(emptyList())
            }

            override suspend fun deleteCheckIn(uuid: String): Result<Unit> = Result.success(Unit)
        }
        val checkInSync = CheckInSyncManager(checkInLocal, fakeCheckInRemote)

        val breathingLocal = BreathingLocalDataSource(database.customBreathingPatternDao())
        val fakeBreathingRemote = object : BreathingRemoteDataSource {
            override suspend fun pushBatch(request: SyncBreathingBatchRequestDto): Result<SyncBreathingBatchResponseDto> {
                return Result.success(SyncBreathingBatchResponseDto(serverTimestamp = Instant.now().toString()))
            }

            override suspend fun fetchChangesSince(since: Instant): Result<List<BreathingPatternNetworkDto>> {
                return Result.success(emptyList())
            }

            override suspend fun deletePattern(uuid: String): Result<Unit> = Result.success(Unit)
        }
        val breathingSync = BreathingSyncManager(breathingLocal, fakeBreathingRemote)

        val fakeScreeningRemote = object : ScreeningRemoteDataSource {
            override suspend fun pushBatch(request: SyncScreeningBatchRequestDto): Result<SyncScreeningBatchResponseDto> {
                return Result.success(SyncScreeningBatchResponseDto(serverTimestamp = Instant.now().toString()))
            }

            override suspend fun fetchChangesSince(since: Instant): Result<List<ScreeningNetworkDto>> {
                return Result.success(emptyList())
            }
        }
        val screeningSync = ScreeningSyncManager(
            database.screeningResultDao(),
            database.screeningAnswerDao(),
            fakeScreeningRemote
        )

        syncCoordinator = SyncCoordinator(
            checkInSyncManager = checkInSync,
            breathingSyncManager = breathingSync,
            screeningSyncManager = screeningSync,
            authSessionManager = authSessionManager
        )
    }

    @After
    fun tearDown() {
        database.close()
        authSessionManager.clearSession()
    }

    @Test
    fun testBackupFailsWhenNotLoggedIn() = runTest {
        val result = syncCoordinator.backup()
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("belum login") == true)
    }

    @Test
    fun testAuthSessionManagerLifecycle() {
        assertFalse(authSessionManager.isLoggedIn())
        assertNull(authSessionManager.getToken())

        authSessionManager.saveSession(
            token = "jwt_token_123",
            email = "user@selaras.com",
            userId = "user_uuid_1",
            userName = "Ahmad"
        )

        assertTrue(authSessionManager.isLoggedIn())
        assertEquals("jwt_token_123", authSessionManager.getToken())
        assertEquals("user@selaras.com", authSessionManager.getUserEmail())
        assertEquals("Ahmad", authSessionManager.getUserName())

        val now = Instant.now()
        authSessionManager.updateLastSyncedAt(now)
        assertEquals(now, authSessionManager.getLastSyncedAt())

        authSessionManager.clearSession()
        assertFalse(authSessionManager.isLoggedIn())
        assertNull(authSessionManager.getToken())
    }

    @Test
    fun testBackupAndRestoreWhenLoggedIn() = runTest {
        authSessionManager.saveSession(
            token = "valid_jwt",
            email = "test@selaras.com",
            userId = "user_abc",
            userName = "Test User"
        )

        // Insert pending local check-in
        database.checkInDao().insertCheckIn(
            CheckInEntity(
                id = 0L,
                uuid = "uuid-123",
                checkInDate = LocalDate.now(),
                mood = 4,
                energy = 8,
                sleepDurationMinutes = 480,
                syncStatus = "PENDING_CREATE",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )

        val backupResult = syncCoordinator.backup()
        assertTrue(backupResult.isSuccess)
        val report = backupResult.getOrThrow()
        assertTrue(report.isSuccess)
        assertEquals(1, report.pushedCheckIns)
        assertNotNull(authSessionManager.getLastSyncedAt())

        val restoreResult = syncCoordinator.restore()
        assertTrue(restoreResult.isSuccess)
    }
}
