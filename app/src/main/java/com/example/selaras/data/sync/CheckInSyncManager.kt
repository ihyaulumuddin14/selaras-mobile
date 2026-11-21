package com.example.selaras.data.sync

import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.datasource.remote.CheckInRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.CheckInNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncCheckInBatchRequestDto
import com.example.selaras.data.local.entity.CheckInEntity
import java.time.Instant
import java.time.LocalDate

data class SyncResult(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val deletedCount: Int = 0,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

/**
 * Orchestrates offline-first push and pull data synchronization between Room (SSoT)
 * and the remote backend server.
 */
class CheckInSyncManager(
    private val localDataSource: CheckInLocalDataSource,
    private val remoteDataSource: CheckInRemoteDataSource
) {

    /**
     * Performs a bidirectional sync:
     * 1. Push pending local changes (creations, updates, soft deletes) to remote.
     * 2. Pull updated delta changes from remote since [lastSyncedAt] and merge to local Room.
     */
    suspend fun sync(lastSyncedAt: Instant = Instant.EPOCH): SyncResult {
        var pushed = 0
        var deleted = 0
        var pulled = 0

        // ==========================================
        // 1. PUSH PHASE: Local -> Remote
        // ==========================================
        val pendingEntities = localDataSource.getPendingSyncCheckIns()
        if (pendingEntities.isNotEmpty()) {
            val dtoList = pendingEntities.map { it.toNetworkDto() }
            val pushResult = remoteDataSource.pushBatch(
                SyncCheckInBatchRequestDto(items = dtoList)
            )

            if (pushResult.isFailure) {
                return SyncResult(
                    isSuccess = false,
                    errorMessage = pushResult.exceptionOrNull()?.message ?: "Push synchronization failed"
                )
            }

            val response = pushResult.getOrThrow()
            val syncedUuids = response.syncedUuids.toSet()

            for (entity in pendingEntities) {
                if (entity.uuid in syncedUuids) {
                    if (entity.isDeleted) {
                        // Successfully deleted remotely, clean up local tombstone to keep database lean
                        localDataSource.hardDeleteCheckIn(entity.id)
                        deleted++
                    } else {
                        // Marked as SYNCED in local Room
                        localDataSource.updateSyncStatus(entity.id, "SYNCED")
                        pushed++
                    }
                }
            }
        }

        // ==========================================
        // 2. PULL PHASE: Remote -> Local
        // ==========================================
        val pullResult = remoteDataSource.fetchChangesSince(lastSyncedAt)
        if (pullResult.isFailure) {
            return SyncResult(
                pushedCount = pushed,
                deletedCount = deleted,
                isSuccess = false,
                errorMessage = pullResult.exceptionOrNull()?.message ?: "Pull synchronization failed"
            )
        }

        val remoteChanges = pullResult.getOrThrow()
        val now = Instant.now()

        for (remoteItem in remoteChanges) {
            val localEntity = localDataSource.getCheckInByUuid(remoteItem.uuid)

            if (localEntity != null) {
                // If local has un-synced edits, resolve conflict: Last-Write-Wins (LWW)
                val remoteUpdated = runCatching { Instant.parse(remoteItem.updatedAt) }.getOrDefault(now)
                val localUpdated = localEntity.updatedAt

                if (localEntity.syncStatus == "SYNCED" || remoteUpdated.isAfter(localUpdated)) {
                    val updatedEntity = localEntity.copy(
                        mood = remoteItem.mood,
                        energy = remoteItem.energy,
                        sleepDurationMinutes = remoteItem.sleepDurationMinutes,
                        note = remoteItem.note,
                        syncStatus = "SYNCED",
                        isDeleted = remoteItem.isDeleted,
                        updatedAt = remoteUpdated
                    )
                    localDataSource.updateCheckIn(updatedEntity)
                    pulled++
                }
            } else {
                // New record from server, insert to local Room
                if (!remoteItem.isDeleted) {
                    val newEntity = CheckInEntity(
                        id = 0L,
                        uuid = remoteItem.uuid,
                        userId = remoteItem.userId,
                        checkInDate = LocalDate.parse(remoteItem.checkInDate),
                        mood = remoteItem.mood,
                        energy = remoteItem.energy,
                        sleepDurationMinutes = remoteItem.sleepDurationMinutes,
                        note = remoteItem.note,
                        syncStatus = "SYNCED",
                        isDeleted = false,
                        createdAt = runCatching { Instant.parse(remoteItem.createdAt) }.getOrDefault(now),
                        updatedAt = runCatching { Instant.parse(remoteItem.updatedAt) }.getOrDefault(now)
                    )
                    localDataSource.insertCheckIn(newEntity)
                    pulled++
                }
            }
        }

        return SyncResult(
            pushedCount = pushed,
            pulledCount = pulled,
            deletedCount = deleted,
            isSuccess = true
        )
    }

    private fun CheckInEntity.toNetworkDto(): CheckInNetworkDto {
        return CheckInNetworkDto(
            uuid = uuid,
            userId = userId,
            checkInDate = checkInDate.toString(),
            mood = mood,
            energy = energy,
            sleepDurationMinutes = sleepDurationMinutes,
            note = note,
            isDeleted = isDeleted,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }
}
