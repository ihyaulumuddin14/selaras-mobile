package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.ScreeningNetworkDto
import com.example.selaras.data.datasource.remote.dto.SyncScreeningBatchRequestDto
import com.example.selaras.data.datasource.remote.dto.SyncScreeningBatchResponseDto
import java.time.Instant

/**
 * Remote data source contract for PHQ-9 Depression Screening sync.
 */
interface ScreeningRemoteDataSource {

    suspend fun pushBatch(request: SyncScreeningBatchRequestDto): Result<SyncScreeningBatchResponseDto>

    suspend fun fetchChangesSince(since: Instant): Result<List<ScreeningNetworkDto>>
}
