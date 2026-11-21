package com.example.selaras.data.repository

import com.example.selaras.data.datasource.local.AppSettingsLocalDataSource
import com.example.selaras.data.local.entity.AppSettingsEntity
import com.example.selaras.domain.model.AppSettings
import com.example.selaras.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalTime

class AppSettingsRepositoryImpl(
    private val localDataSource: AppSettingsLocalDataSource
) : AppSettingsRepository {

    override fun getSettingsFlow(): Flow<AppSettings?> {
        return localDataSource.getSettingsFlow().map { it?.toDomain() }
    }

    override suspend fun getSettings(): AppSettings? {
        return localDataSource.getSettings()?.toDomain()
    }

    override suspend fun initializeIfFirstLaunch(): Result<AppSettings> = runCatching {
        val existing = localDataSource.getSettings()
        if (existing != null) {
            existing.toDomain()
        } else {
            val now = Instant.now()
            val initial = AppSettingsEntity(
                id = AppSettingsEntity.SINGLETON_ID,
                firstActivatedAt = now,
                onboardingCompleted = false,
                consentAcceptedAt = null,
                checkinReminderEnabled = false,
                checkinReminderTime = LocalTime.of(21, 0),
                createdAt = now,
                updatedAt = now
            )
            localDataSource.upsertSettings(initial)
            initial.toDomain()
        }
    }

    override suspend fun acceptConsent(): Result<Unit> = runCatching {
        val now = Instant.now()
        val current = localDataSource.getSettings() ?: AppSettingsEntity(
            id = AppSettingsEntity.SINGLETON_ID,
            firstActivatedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val updated = current.copy(
            consentAcceptedAt = now,
            updatedAt = now
        )
        localDataSource.upsertSettings(updated)
    }

    override suspend fun completeOnboarding(): Result<Unit> = runCatching {
        val now = Instant.now()
        val current = localDataSource.getSettings() ?: AppSettingsEntity(
            id = AppSettingsEntity.SINGLETON_ID,
            firstActivatedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val updated = current.copy(
            onboardingCompleted = true,
            updatedAt = now
        )
        localDataSource.upsertSettings(updated)
    }

    override suspend fun setCheckInReminder(enabled: Boolean, time: LocalTime?): Result<Unit> = runCatching {
        val now = Instant.now()
        val current = localDataSource.getSettings() ?: AppSettingsEntity(
            id = AppSettingsEntity.SINGLETON_ID,
            firstActivatedAt = now,
            createdAt = now,
            updatedAt = now
        )
        val updated = current.copy(
            checkinReminderEnabled = enabled,
            checkinReminderTime = time,
            updatedAt = now
        )
        localDataSource.upsertSettings(updated)
    }

    private fun AppSettingsEntity.toDomain(): AppSettings = AppSettings(
        id = id,
        firstActivatedAt = firstActivatedAt,
        onboardingCompleted = onboardingCompleted,
        consentAcceptedAt = consentAcceptedAt,
        checkinReminderEnabled = checkinReminderEnabled,
        checkinReminderTime = checkinReminderTime,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
