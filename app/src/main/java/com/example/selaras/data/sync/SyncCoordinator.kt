package com.example.selaras.data.sync

import com.example.selaras.core.security.AuthSessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

data class SyncReport(
    val pushedCheckIns: Int = 0,
    val pulledCheckIns: Int = 0,
    val pushedBreathing: Int = 0,
    val pulledBreathing: Int = 0,
    val pushedScreenings: Int = 0,
    val pulledScreenings: Int = 0,
    val isSuccess: Boolean = true,
    val message: String? = null
) {
    val totalPushed: Int get() = pushedCheckIns + pushedBreathing + pushedScreenings
    val totalPulled: Int get() = pulledCheckIns + pulledBreathing + pulledScreenings
}

/**
 * Coordinates all data sync managers across Check-ins, Breathing Patterns, and PHQ-9 Screenings.
 * Enforces Room as Single Source of Truth (SSoT) and Smart Merge via Last-Write-Wins (LWW).
 */
class SyncCoordinator(
    private val checkInSyncManager: CheckInSyncManager,
    private val breathingSyncManager: BreathingSyncManager,
    private val screeningSyncManager: ScreeningSyncManager,
    private val authSessionManager: AuthSessionManager
) {

    /**
     * PUSH: Cadangkan seluruh perubahan data lokal ke server awan.
     */
    suspend fun backup(): Result<SyncReport> = withContext(Dispatchers.IO) {
        if (!authSessionManager.isLoggedIn()) {
            return@withContext Result.failure(IllegalStateException("Pengguna belum login. Silakan hubungkan akun terlebih dahulu."))
        }

        try {
            val checkInResult = checkInSyncManager.sync(lastSyncedAt = Instant.now())
            if (!checkInResult.isSuccess) {
                return@withContext Result.failure(Exception(checkInResult.errorMessage ?: "Gagal mencadangkan check-in"))
            }

            val breathingResult = breathingSyncManager.sync(lastSyncedAt = Instant.now())
            if (!breathingResult.isSuccess) {
                return@withContext Result.failure(Exception(breathingResult.errorMessage ?: "Gagal mencadangkan pola pernapasan"))
            }

            val screeningResult = screeningSyncManager.sync(lastSyncedAt = Instant.now())
            if (!screeningResult.isSuccess) {
                return@withContext Result.failure(Exception(screeningResult.errorMessage ?: "Gagal mencadangkan hasil skrining"))
            }

            val now = Instant.now()
            authSessionManager.updateLastSyncedAt(now)

            Result.success(
                SyncReport(
                    pushedCheckIns = checkInResult.pushedCount,
                    pushedBreathing = breathingResult.pushedCount,
                    pushedScreenings = screeningResult.pushedCount,
                    isSuccess = true,
                    message = "Pencadangan berhasil. Data tersimpan aman di server awan."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * PULL (Restore): Mengunduh seluruh riwayat data dari server dan menggabungkan secara cerdas
     * ke dalam Room lokal tanpa menghapus progress yang sudah ada (Smart Merge via LWW).
     */
    suspend fun restore(): Result<SyncReport> = withContext(Dispatchers.IO) {
        if (!authSessionManager.isLoggedIn()) {
            return@withContext Result.failure(IllegalStateException("Pengguna belum login. Silakan hubungkan akun terlebih dahulu."))
        }

        try {
            // Restore dari epoch untuk menarik seluruh riwayat
            val checkInResult = checkInSyncManager.sync(lastSyncedAt = Instant.EPOCH)
            if (!checkInResult.isSuccess) {
                return@withContext Result.failure(Exception(checkInResult.errorMessage ?: "Gagal memulihkan check-in"))
            }

            val breathingResult = breathingSyncManager.sync(lastSyncedAt = Instant.EPOCH)
            if (!breathingResult.isSuccess) {
                return@withContext Result.failure(Exception(breathingResult.errorMessage ?: "Gagal memulihkan pola pernapasan"))
            }

            val screeningResult = screeningSyncManager.sync(lastSyncedAt = Instant.EPOCH)
            if (!screeningResult.isSuccess) {
                return@withContext Result.failure(Exception(screeningResult.errorMessage ?: "Gagal memulihkan hasil skrining"))
            }

            val now = Instant.now()
            authSessionManager.updateLastSyncedAt(now)

            Result.success(
                SyncReport(
                    pulledCheckIns = checkInResult.pulledCount,
                    pulledBreathing = breathingResult.pulledCount,
                    pulledScreenings = screeningResult.pulledCount,
                    isSuccess = true,
                    message = "Pemulihan data berhasil digabungkan dengan database perangkat."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Dua arah: Push lokal yang belum dicadangkan, lalu Pull perubahan terbaru dari awan.
     */
    suspend fun fullSync(): Result<SyncReport> = withContext(Dispatchers.IO) {
        if (!authSessionManager.isLoggedIn()) {
            return@withContext Result.failure(IllegalStateException("Pengguna belum login."))
        }

        val lastSync = authSessionManager.getLastSyncedAt() ?: Instant.EPOCH
        try {
            val checkInResult = checkInSyncManager.sync(lastSyncedAt = lastSync)
            val breathingResult = breathingSyncManager.sync(lastSyncedAt = lastSync)
            val screeningResult = screeningSyncManager.sync(lastSyncedAt = lastSync)

            val now = Instant.now()
            authSessionManager.updateLastSyncedAt(now)

            Result.success(
                SyncReport(
                    pushedCheckIns = checkInResult.pushedCount,
                    pulledCheckIns = checkInResult.pulledCount,
                    pushedBreathing = breathingResult.pushedCount,
                    pulledBreathing = breathingResult.pulledCount,
                    pushedScreenings = screeningResult.pushedCount,
                    pulledScreenings = screeningResult.pulledCount,
                    isSuccess = true,
                    message = "Sinkronisasi dua arah selesai."
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
