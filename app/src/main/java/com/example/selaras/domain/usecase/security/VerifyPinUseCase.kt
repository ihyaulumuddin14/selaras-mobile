package com.example.selaras.domain.usecase.security

import com.example.selaras.domain.repository.SecurityRepository

/**
 * UseCase for authenticating user against stored 6-digit App Lock PIN.
 */
class VerifyPinUseCase(
    private val securityRepository: SecurityRepository
) {
    operator fun invoke(pin: String): Boolean {
        if (pin.length != 6 || !pin.all { it.isDigit() }) {
            return false
        }
        return securityRepository.verifyPin(pin)
    }
}
