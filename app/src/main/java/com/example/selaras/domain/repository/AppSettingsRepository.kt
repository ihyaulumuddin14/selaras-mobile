package com.example.selaras.domain.repository

import com.example.selaras.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

interface AppSettingsRepository {
    fun getSettingsFlow(): Flow<AppSettings?>
    suspend fun getSettings(): AppSettings?
    suspend fun initializeIfFirstLaunch(): Result<AppSettings>
    suspend fun acceptConsent(): Result<Unit>
    suspend fun completeOnboarding(): Result<Unit>
    suspend fun setCheckInReminder(enabled: Boolean, time: LocalTime?): Result<Unit>
}
