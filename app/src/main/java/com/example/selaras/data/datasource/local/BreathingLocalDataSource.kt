package com.example.selaras.data.datasource.local

import com.example.selaras.core.database.dao.CustomBreathingPatternDao
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class BreathingLocalDataSource(
    private val dao: CustomBreathingPatternDao
) {
    fun observeActivePatterns(): Flow<List<CustomBreathingPatternEntity>> =
        dao.getAllPatternsFlow()

    suspend fun insertPattern(entity: CustomBreathingPatternEntity): Long =
        dao.insertPattern(entity)

    suspend fun updatePattern(entity: CustomBreathingPatternEntity) =
        dao.updatePattern(entity)

    suspend fun softDeletePattern(id: Long, now: Instant = Instant.now()) =
        dao.softDeletePatternById(id, now)

    suspend fun hardDeletePattern(id: Long) =
        dao.deletePatternById(id)

    suspend fun getPatternById(id: Long): CustomBreathingPatternEntity? =
        dao.getPatternById(id)

    suspend fun getPatternByUuid(uuid: String): CustomBreathingPatternEntity? =
        dao.getPatternByUuid(uuid)

    suspend fun getPendingSyncPatterns(): List<CustomBreathingPatternEntity> =
        dao.getPendingSyncPatterns()

    suspend fun updateSyncStatus(id: Long, syncStatus: String, now: Instant = Instant.now()) =
        dao.updateSyncStatus(id, syncStatus, now)
}
