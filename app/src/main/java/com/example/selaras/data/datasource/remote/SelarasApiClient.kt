package com.example.selaras.data.datasource.remote

import com.example.selaras.data.datasource.remote.dto.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Standard REST API Client for Selaras, powered by Retrofit 2 and OkHttp 3.
 */
class SelarasApiClient(
    baseUrl: String = DEFAULT_BASE_URL,
    customOkHttpClient: OkHttpClient? = null,
    val apiService: SelarasApiService = createRetrofitService(baseUrl, customOkHttpClient)
) {

    suspend fun register(
        request: RegisterRequestDto
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.register(request) }
    }

    suspend fun login(
        request: LoginRequestDto
    ): Result<AuthResponseDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.login(request) }
    }

    suspend fun getMe(
        token: String
    ): Result<AuthUserDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.getMe(formatBearer(token)) }
    }

    suspend fun pushCheckIns(
        token: String,
        request: SyncCheckInBatchRequestDto
    ): Result<SyncCheckInBatchResponseDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.pushCheckIns(formatBearer(token), request) }
    }

    suspend fun pullCheckIns(
        token: String,
        since: Instant
    ): Result<List<CheckInNetworkDto>> = withContext(Dispatchers.IO) {
        val queryParam = if (since != Instant.EPOCH) since.toString() else null
        safeApiCall { apiService.pullCheckIns(formatBearer(token), queryParam) }
            .map { it.items }
    }

    suspend fun pushBreathingPatterns(
        token: String,
        request: SyncBreathingBatchRequestDto
    ): Result<SyncBreathingBatchResponseDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.pushBreathingPatterns(formatBearer(token), request) }
    }

    suspend fun pullBreathingPatterns(
        token: String,
        since: Instant
    ): Result<List<BreathingPatternNetworkDto>> = withContext(Dispatchers.IO) {
        val queryParam = if (since != Instant.EPOCH) since.toString() else null
        safeApiCall { apiService.pullBreathingPatterns(formatBearer(token), queryParam) }
            .map { it.items }
    }

    suspend fun pushScreenings(
        token: String,
        request: SyncScreeningBatchRequestDto
    ): Result<SyncScreeningBatchResponseDto> = withContext(Dispatchers.IO) {
        safeApiCall { apiService.pushScreenings(formatBearer(token), request) }
    }

    suspend fun pullScreenings(
        token: String,
        since: Instant
    ): Result<List<ScreeningNetworkDto>> = withContext(Dispatchers.IO) {
        val queryParam = if (since != Instant.EPOCH) since.toString() else null
        safeApiCall { apiService.pullScreenings(formatBearer(token), queryParam) }
            .map { it.items }
    }

    private fun formatBearer(token: String): String {
        return if (token.startsWith("Bearer ")) token else "Bearer $token"
    }

    private suspend fun <T> safeApiCall(
        apiCall: suspend () -> Response<ApiResponse<T>>
    ): Result<T> {
        return try {
            val response = apiCall()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success && body.data != null) {
                    Result.success(body.data)
                } else {
                    val errMsg = body?.error?.message ?: body?.message ?: "Respons server tidak valid"
                    Result.failure(Exception(errMsg))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                val parsedError = errorBody?.let {
                    runCatching {
                        val gson = Gson()
                        val apiErr = gson.fromJson(it, ApiResponse::class.java)
                        apiErr.error?.message ?: apiErr.message
                    }.getOrNull()
                }
                val finalMessage = parsedError ?: "Permintaan gagal dengan kode HTTP ${response.code()}"
                Result.failure(Exception(finalMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://192.168.100.10:3000/api/v1/"

        fun createRetrofitService(
            baseUrl: String = DEFAULT_BASE_URL,
            customOkHttpClient: OkHttpClient? = null
        ): SelarasApiService {
            val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val okHttp = customOkHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(normalizedBaseUrl)
                .client(okHttp)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(SelarasApiService::class.java)
        }
    }
}
