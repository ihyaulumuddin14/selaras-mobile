package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase for establishing a new 6-digit App Lock PIN and generating the emergency recovery code.
 */
class SetupPinAndRecoveryUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(pin: String): Result<String> {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN harus berupa 6 digit angka"))
        }
        return securityRepository.setupPinAndRecovery(pin)
    }
}
