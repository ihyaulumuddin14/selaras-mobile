package com.example.selaras.data.datasource.remote.dto

data class RegisterRequestDto(
    val email: String,
    val password: String,
    val userName: String? = null
)

data class LoginRequestDto(
    val email: String,
    val password: String
)

data class AuthUserDto(
    val id: String,
    val email: String,
    val userName: String? = null,
    val createdAt: String
)

data class AuthResponseDto(
    val token: String,
    val user: AuthUserDto
)

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: ApiErrorDto? = null
)

data class ApiErrorDto(
    val code: String? = null,
    val message: String? = null,
    val details: Any? = null
)

data class SyncPullResponseDto<T>(
    val serverTimestamp: String? = null,
    val items: List<T> = emptyList()
)

