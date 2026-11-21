package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase for changing the user's PIN by validating current PIN and establishing a new 6-digit PIN.
 */
class ChangePinUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(currentPin: String, newPin: String): Result<Unit> {
        if (currentPin.length != 6 || !currentPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN saat ini harus berupa 6 digit angka"))
        }
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN baru harus berupa 6 digit angka"))
        }
        return securityRepository.changePin(currentPin, newPin)
    }
}
