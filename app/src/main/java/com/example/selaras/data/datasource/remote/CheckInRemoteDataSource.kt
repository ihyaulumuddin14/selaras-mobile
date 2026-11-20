package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.CheckInNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncCheckInBatchRequestDto
import com.example.selaras.data.datasource.remote.dto.SyncCheckInBatchResponseDto
import java.time.Instant

/**
 * Remote data source contract for Daily Check-in sync with backend.
 * Implementation will be powered by Retrofit once the Express.js endpoints land.
 */
interface CheckInRemoteDataSource {

    /**
     * Pushes a batch of check-in records (creations, updates, soft deletes) to the server.
     */
    suspend fun pushBatch(request: SyncCheckInBatchRequestDto): Result<SyncCheckInBatchResponseDto>

    /**
     * Fetches check-in records updated on the server since [since].
     */
    suspend fun fetchChangesSince(since: Instant): Result<List<CheckInNetworkDto>>

    /**
     * Explicit hard-delete endpoint by UUID if server supports single item purge.
     */
    suspend fun deleteCheckIn(uuid: String): Result<Unit>
}
