package com.example.selaras.data.datasource.remote

import com.example.selaras.core.security.AuthSessionManager
import com.example.selaras.data.datasource.remote.dto.*
import java.time.Instant

class HttpCheckInRemoteDataSource(
    private val apiClient: SelarasApiClient,
    private val sessionManager: AuthSessionManager
) : CheckInRemoteDataSource {

    override suspend fun pushBatch(request: SyncCheckInBatchRequestDto): Result<SyncCheckInBatchResponseDto> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pushCheckIns(token, request)
    }

    override suspend fun fetchChangesSince(since: Instant): Result<List<CheckInNetworkDto>> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pullCheckIns(token, since)
    }

    override suspend fun deleteCheckIn(uuid: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class HttpBreathingRemoteDataSource(
    private val apiClient: SelarasApiClient,
    private val sessionManager: AuthSessionManager
) : BreathingRemoteDataSource {

    override suspend fun pushBatch(request: SyncBreathingBatchRequestDto): Result<SyncBreathingBatchResponseDto> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pushBreathingPatterns(token, request)
    }

    override suspend fun fetchChangesSince(since: Instant): Result<List<BreathingPatternNetworkDto>> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pullBreathingPatterns(token, since)
    }

    override suspend fun deletePattern(uuid: String): Result<Unit> {
        return Result.success(Unit)
    }
}

class HttpScreeningRemoteDataSource(
    private val apiClient: SelarasApiClient,
    private val sessionManager: AuthSessionManager
) : ScreeningRemoteDataSource {

    override suspend fun pushBatch(request: SyncScreeningBatchRequestDto): Result<SyncScreeningBatchResponseDto> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pushScreenings(token, request)
    }

    override suspend fun fetchChangesSince(since: Instant): Result<List<ScreeningNetworkDto>> {
        val token = sessionManager.getToken()
            ?: return Result.failure(IllegalStateException("Sesi login tidak ditemukan. Harap login kembali."))
        return apiClient.pullScreenings(token, since)
    }
}
