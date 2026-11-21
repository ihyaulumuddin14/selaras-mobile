package com.example.selaras.data.datasource.local

import com.example.selaras.core.database.dao.AppSettingsDao
import com.example.selaras.data.local.entity.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

class AppSettingsLocalDataSource(
    private val dao: AppSettingsDao
) {
    fun getSettingsFlow(): Flow<AppSettingsEntity?> = dao.getSettingsFlow()

    suspend fun getSettings(): AppSettingsEntity? = dao.getSettings()

    suspend fun upsertSettings(settings: AppSettingsEntity) = dao.upsertSettings(settings)

    suspend fun updateSettings(settings: AppSettingsEntity) = dao.updateSettings(settings)

    suspend fun countSettings(): Int = dao.countSettings()
}
