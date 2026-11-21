package com.example.selaras.domain.usecase.onboarding

import com.example.selaras.domain.model.AppSettings
import com.example.selaras.domain.repository.AppSettingsRepository

class InitializeAppSettingsUseCase(
    private val repository: AppSettingsRepository
) {
    suspend operator fun invoke(): Result<AppSettings> = repository.initializeIfFirstLaunch()
}
