package com.example.selaras.domain.usecase.onboarding

import com.example.selaras.domain.repository.AppSettingsRepository

class CompleteOnboardingUseCase(
    private val repository: AppSettingsRepository
) {
    suspend operator fun invoke(): Result<Unit> = repository.completeOnboarding()
}
