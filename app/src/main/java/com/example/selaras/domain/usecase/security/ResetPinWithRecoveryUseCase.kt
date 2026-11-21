package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase for resetting user's PIN via a valid emergency recovery code.
 */
class ResetPinWithRecoveryUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(recoveryCode: String, newPin: String): Result<Unit> {
        val sanitizedCode = recoveryCode.replace("-", "").replace(" ", "").trim()
        if (sanitizedCode.length != 16) {
            return Result.failure(IllegalArgumentException("Format kode pemulihan tidak valid"))
        }
        if (newPin.length != 6 || !newPin.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("PIN baru harus berupa 6 digit angka"))
        }
        return securityRepository.resetPinWithRecovery(recoveryCode, newPin)
    }
}
