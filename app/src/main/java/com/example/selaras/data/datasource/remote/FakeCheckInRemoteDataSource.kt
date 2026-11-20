package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.CheckInNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncCheckInBatchRequestDto
import com.example.selaras.data.datasource.remote.dto.SyncCheckInBatchResponseDto
import java.time.Instant

/**
 * In-memory Fake Remote Data Source for offline development, preview, and unit tests.
 * Simulates server storage without requiring an active network connection or backend instance.
 */
class FakeCheckInRemoteDataSource : CheckInRemoteDataSource {

    private val remoteStorage = mutableMapOf<String, CheckInNetworkDto>()
    var shouldFail: Boolean = false

    override suspend fun pushBatch(request: SyncCheckInBatchRequestDto): Result<SyncCheckInBatchResponseDto> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("Simulated network error: Remote server unreachable"))
        }

        val synced = mutableListOf<String>()
        for (item in request.items) {
            if (item.isDeleted) {
                remoteStorage.remove(item.uuid)
            } else {
                remoteStorage[item.uuid] = item
            }
            synced.add(item.uuid)
        }

        return Result.success(
            SyncCheckInBatchResponseDto(
                serverTimestamp = Instant.now().toString(),
                syncedUuids = synced
            )
        )
    }

    override suspend fun fetchChangesSince(since: Instant): Result<List<CheckInNetworkDto>> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("Simulated network error: Remote server unreachable"))
        }

        val changes = remoteStorage.values.filter {
            runCatching { Instant.parse(it.updatedAt).isAfter(since) }.getOrDefault(true)
        }
        return Result.success(changes)
    }

    override suspend fun deleteCheckIn(uuid: String): Result<Unit> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("Simulated network error: Remote server unreachable"))
        }
        remoteStorage.remove(uuid)
        return Result.success(Unit)
    }

    fun seedRemote(items: List<CheckInNetworkDto>) {
        items.forEach { remoteStorage[it.uuid] = it }
    }

    fun getRemoteItems(): List<CheckInNetworkDto> = remoteStorage.values.toList()

    fun clear() {
        remoteStorage.clear()
    }
}
