package com.example.selaras.domain.usecase.onboarding

import com.example.selaras.domain.model.AppSettings
import com.example.selaras.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow

class GetAppSettingsUseCase(
    private val repository: AppSettingsRepository
) {
    operator fun invoke(): Flow<AppSettings?> = repository.getSettingsFlow()

    suspend fun getOnce(): AppSettings? = repository.getSettings()
}
