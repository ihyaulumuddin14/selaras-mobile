package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase to check if App Lock is active.
 */
class CheckAppLockStatusUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(): Boolean {
        return securityRepository.isPinSet()
    }
}
