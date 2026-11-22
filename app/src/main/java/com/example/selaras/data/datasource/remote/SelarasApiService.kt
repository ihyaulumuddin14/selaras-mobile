package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit 2 REST API service contract for Selaras Cloud Backend.
 */
interface SelarasApiService {

    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto
    ): Response<ApiResponse<AuthResponseDto>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequestDto
    ): Response<ApiResponse<AuthResponseDto>>

    @GET("auth/me")
    suspend fun getMe(
        @Header("Authorization") token: String
    ): Response<ApiResponse<AuthUserDto>>

    @POST("sync/check-ins/push")
    suspend fun pushCheckIns(
        @Header("Authorization") token: String,
        @Body request: SyncCheckInBatchRequestDto
    ): Response<ApiResponse<SyncCheckInBatchResponseDto>>

    @GET("sync/check-ins/pull")
    suspend fun pullCheckIns(
        @Header("Authorization") token: String,
        @Query("since") since: String? = null
    ): Response<ApiResponse<SyncPullResponseDto<CheckInNetworkDto>>>

    @POST("sync/breathing-patterns/push")
    suspend fun pushBreathingPatterns(
        @Header("Authorization") token: String,
        @Body request: SyncBreathingBatchRequestDto
    ): Response<ApiResponse<SyncBreathingBatchResponseDto>>

    @GET("sync/breathing-patterns/pull")
    suspend fun pullBreathingPatterns(
        @Header("Authorization") token: String,
        @Query("since") since: String? = null
    ): Response<ApiResponse<SyncPullResponseDto<BreathingPatternNetworkDto>>>

    @POST("sync/screenings/push")
    suspend fun pushScreenings(
        @Header("Authorization") token: String,
        @Body request: SyncScreeningBatchRequestDto
    ): Response<ApiResponse<SyncScreeningBatchResponseDto>>

    @GET("sync/screenings/pull")
    suspend fun pullScreenings(
        @Header("Authorization") token: String,
        @Query("since") since: String? = null
    ): Response<ApiResponse<SyncPullResponseDto<ScreeningNetworkDto>>>
}
