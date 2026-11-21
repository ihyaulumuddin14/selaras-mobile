package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.BreathingPatternNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncBreathingBatchRequestDto
import com.example.selaras.data.datasource.remote.dto.SyncBreathingBatchResponseDto
import java.time.Instant

/**
 * In-memory Fake Remote Data Source for Custom Breathing Patterns.
 * Used for unit testing and offline development before Retrofit backend is attached.
 */
class FakeBreathingRemoteDataSource : BreathingRemoteDataSource {

    private val remoteStorage = mutableMapOf<String, BreathingPatternNetworkDto>()
    var shouldFail: Boolean = false

    override suspend fun pushBatch(request: SyncBreathingBatchRequestDto): Result<SyncBreathingBatchResponseDto> {
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
            SyncBreathingBatchResponseDto(
                serverTimestamp = Instant.now().toString(),
                syncedUuids = synced
            )
        )
    }

    override suspend fun fetchChangesSince(since: Instant): Result<List<BreathingPatternNetworkDto>> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("Simulated network error: Remote server unreachable"))
        }

        val changes = remoteStorage.values.filter {
            runCatching { Instant.parse(it.updatedAt).isAfter(since) }.getOrDefault(true)
        }
        return Result.success(changes)
    }

    override suspend fun deletePattern(uuid: String): Result<Unit> {
        if (shouldFail) {
            return Result.failure(IllegalStateException("Simulated network error: Remote server unreachable"))
        }
        remoteStorage.remove(uuid)
        return Result.success(Unit)
    }

    fun seedRemote(items: List<BreathingPatternNetworkDto>) {
        items.forEach { remoteStorage[it.uuid] = it }
    }

    fun getRemoteItems(): List<BreathingPatternNetworkDto> = remoteStorage.values.toList()

    fun clear() {
        remoteStorage.clear()
    }
}
