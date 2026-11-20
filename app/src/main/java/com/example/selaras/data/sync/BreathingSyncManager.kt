package com.example.selaras.data.sync

import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.datasource.remote.BreathingRemoteDataSource
import com.example.selaras.data.datasource.remote.dto.BreathingPatternNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncBreathingBatchRequestDto
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import java.time.Instant

/**
 * Orchestrates offline-first push and pull data synchronization for Custom Breathing Patterns
 * between Room (SSoT) and the backend server.
 */
class BreathingSyncManager(
    private val localDataSource: BreathingLocalDataSource,
    private val remoteDataSource: BreathingRemoteDataSource
) {

    /**
     * Performs bidirectional synchronization:
     * 1. Push pending local patterns (creations, edits, soft deletions) to remote.
     * 2. Pull updated delta patterns from remote since [lastSyncedAt] and merge to local Room.
     */
    suspend fun sync(lastSyncedAt: Instant = Instant.EPOCH): SyncResult {
        var pushed = 0
        var deleted = 0
        var pulled = 0

        // ==========================================
        // 1. PUSH PHASE: Local -> Remote
        // ==========================================
        val pendingEntities = localDataSource.getPendingSyncPatterns()
        if (pendingEntities.isNotEmpty()) {
            val dtoList = pendingEntities.map { it.toNetworkDto() }
            val pushResult = remoteDataSource.pushBatch(
                SyncBreathingBatchRequestDto(items = dtoList)
            )

            if (pushResult.isFailure) {
                return SyncResult(
                    isSuccess = false,
                    errorMessage = pushResult.exceptionOrNull()?.message ?: "Push breathing pattern sync failed"
                )
            }

            val response = pushResult.getOrThrow()
            val syncedUuids = response.syncedUuids.toSet()

            for (entity in pendingEntities) {
                if (entity.uuid in syncedUuids) {
                    if (entity.isDeleted) {
                        // Purge local tombstone after confirmed remote deletion
                        localDataSource.hardDeletePattern(entity.id)
                        deleted++
                    } else {
                        // Mark as SYNCED in local Room
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
                errorMessage = pullResult.exceptionOrNull()?.message ?: "Pull breathing pattern sync failed"
            )
        }

        val remoteChanges = pullResult.getOrThrow()
        val now = Instant.now()

        for (remoteItem in remoteChanges) {
            val localEntity = localDataSource.getPatternByUuid(remoteItem.uuid)

            if (localEntity != null) {
                // Last-Write-Wins (LWW) conflict resolution based on updatedAt
                val remoteUpdated = runCatching { Instant.parse(remoteItem.updatedAt) }.getOrDefault(now)
                val localUpdated = localEntity.updatedAt

                if (localEntity.syncStatus == "SYNCED" || remoteUpdated.isAfter(localUpdated)) {
                    val updatedEntity = localEntity.copy(
                        name = remoteItem.name,
                        inhaleDurationSeconds = remoteItem.inhaleDurationSeconds,
                        exhaleDurationSeconds = remoteItem.exhaleDurationSeconds,
                        syncStatus = "SYNCED",
                        isDeleted = remoteItem.isDeleted,
                        updatedAt = remoteUpdated
                    )
                    localDataSource.updatePattern(updatedEntity)
                    pulled++
                }
            } else {
                // New pattern from server, insert to local Room
                if (!remoteItem.isDeleted) {
                    val newEntity = CustomBreathingPatternEntity(
                        id = 0L,
                        uuid = remoteItem.uuid,
                        userId = remoteItem.userId,
                        name = remoteItem.name,
                        inhaleDurationSeconds = remoteItem.inhaleDurationSeconds,
                        exhaleDurationSeconds = remoteItem.exhaleDurationSeconds,
                        syncStatus = "SYNCED",
                        isDeleted = false,
                        createdAt = runCatching { Instant.parse(remoteItem.createdAt) }.getOrDefault(now),
                        updatedAt = runCatching { Instant.parse(remoteItem.updatedAt) }.getOrDefault(now)
                    )
                    localDataSource.insertPattern(newEntity)
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

    private fun CustomBreathingPatternEntity.toNetworkDto(): BreathingPatternNetworkDto {
        return BreathingPatternNetworkDto(
            uuid = uuid,
            userId = userId,
            name = name,
            inhaleDurationSeconds = inhaleDurationSeconds,
            exhaleDurationSeconds = exhaleDurationSeconds,
            isDeleted = isDeleted,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )
    }
}
