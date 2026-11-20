package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.BreathingPatternNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncBreathingBatchRequestDto
import com.example.selaras.data.datasource.remote.dto.SyncBreathingBatchResponseDto
import java.time.Instant

/**
 * Remote data source contract for Custom Breathing Patterns synchronization.
 */
interface BreathingRemoteDataSource {

    /**
     * Pushes a batch of custom patterns (creations, edits, soft deletions) to the server.
     */
    suspend fun pushBatch(request: SyncBreathingBatchRequestDto): Result<SyncBreathingBatchResponseDto>

    /**
     * Fetches pattern changes updated on the server since [since].
     */
    suspend fun fetchChangesSince(since: Instant): Result<List<BreathingPatternNetworkDto>>

    /**
     * Deletes a pattern by UUID on the server.
     */
    suspend fun deletePattern(uuid: String): Result<Unit>
}
