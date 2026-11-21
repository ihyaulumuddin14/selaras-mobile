package com.example.selaras.domain.usecase.onboarding

import com.example.selaras.domain.repository.AppSettingsRepository

class AcceptConsentUseCase(
    private val repository: AppSettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.acceptConsent()
}
