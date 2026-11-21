package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase for validating a 16-character emergency recovery code.
 */
class VerifyRecoveryCodeUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(code: String): Boolean {
        val sanitized = code.replace("-", "").replace(" ", "").trim()
        if (sanitized.length != 16) {
            return false
        }
        return securityRepository.verifyRecoveryCode(code)
    }
}
