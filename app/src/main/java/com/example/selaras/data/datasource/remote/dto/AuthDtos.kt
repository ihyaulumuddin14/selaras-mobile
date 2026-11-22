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

