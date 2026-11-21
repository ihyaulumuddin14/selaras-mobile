package com.example.selaras.data.repository

import com.example.selaras.data.datasource.local.BreathingLocalDataSource
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class BreathingRepositoryImpl(
    private val localDataSource: BreathingLocalDataSource
) : BreathingRepository {

    override fun observeCustomPatterns(): Flow<List<BreathingPattern>> =
        localDataSource.observeActivePatterns().map { rows ->
            rows.map { entity -> entity.toDomain() }
        }

    override suspend fun saveCustomPattern(pattern: BreathingPattern): Result<BreathingPattern> = runCatching {
        val now = Instant.now()
        val entity = pattern.toEntity().copy(
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = now,
            updatedAt = now
        )
        val generatedId = localDataSource.insertPattern(entity)
        pattern.copy(
            id = generatedId,
            key = "custom:$generatedId",
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun updateCustomPattern(pattern: BreathingPattern): Result<Unit> = runCatching {
        require(pattern.id > 0) { "Pola kustom harus memiliki ID yang valid untuk diperbarui." }
        val existing = localDataSource.getPatternById(pattern.id)
            ?: throw IllegalArgumentException("Pola kustom dengan ID ${pattern.id} tidak ditemukan.")

        val now = Instant.now()
        val updatedEntity = existing.copy(
            name = pattern.name.trim(),
            inhaleDurationSeconds = pattern.inhaleSeconds,
            exhaleDurationSeconds = pattern.exhaleSeconds,
            syncStatus = "PENDING_UPDATE",
            updatedAt = now
        )
        localDataSource.updatePattern(updatedEntity)
    }

    override suspend fun deleteCustomPattern(pattern: BreathingPattern): Result<Unit> = runCatching {
        deleteCustomPatternById(pattern.id).getOrThrow()
    }

    override suspend fun deleteCustomPatternById(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "Hanya pola kustom yang boleh dihapus." }
        localDataSource.softDeletePattern(id)
    }

    override suspend fun getPatternById(id: Long): BreathingPattern? {
        return localDataSource.getPatternById(id)?.toDomain()
    }

    override suspend fun getPatternByUuid(uuid: String): BreathingPattern? {
        return localDataSource.getPatternByUuid(uuid)?.toDomain()
    }

    private fun CustomBreathingPatternEntity.toDomain() = BreathingPattern(
        key = "custom:$id",
        id = id,
        uuid = uuid,
        name = name,
        inhaleSeconds = inhaleDurationSeconds,
        exhaleSeconds = exhaleDurationSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun BreathingPattern.toEntity() = CustomBreathingPatternEntity(
        id = id,
        uuid = uuid,
        name = name.trim(),
        inhaleDurationSeconds = inhaleSeconds,
        exhaleDurationSeconds = exhaleSeconds,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
