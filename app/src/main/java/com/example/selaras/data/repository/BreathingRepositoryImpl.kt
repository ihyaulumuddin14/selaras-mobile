package com.example.selaras.data.repository

import com.example.selaras.core.database.dao.CustomBreathingPatternDao
import com.example.selaras.data.local.entity.CustomBreathingPatternEntity
import com.example.selaras.domain.model.BreathingPattern
import com.example.selaras.domain.repository.BreathingRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Uses the team's existing Room entity and DAO; no schema changes are needed. */
class BreathingRepositoryImpl(
    private val dao: CustomBreathingPatternDao
) : BreathingRepository {

    override fun observeCustomPatterns(): Flow<List<BreathingPattern>> =
        dao.getAllPatternsFlow().map { rows ->
            rows.map { entity -> entity.toDomain() }
        }

    override suspend fun saveCustomPattern(
        name: String,
        inhaleSeconds: Int,
        exhaleSeconds: Int
    ): Long {
        validate(name, inhaleSeconds, exhaleSeconds)
        val now = Instant.now()
        return dao.insertPattern(
            CustomBreathingPatternEntity(
                name = name.trim(),
                inhaleDurationSeconds = inhaleSeconds,
                exhaleDurationSeconds = exhaleSeconds,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    override suspend fun updateCustomPattern(
        id: Long,
        name: String,
        inhaleSeconds: Int,
        exhaleSeconds: Int
    ) {
        validate(name, inhaleSeconds, exhaleSeconds)
        val existing = dao.getPatternById(id)
            ?: throw IllegalArgumentException("Pola sudah tidak ditemukan.")
        dao.updatePattern(
            existing.copy(
                name = name.trim(),
                inhaleDurationSeconds = inhaleSeconds,
                exhaleDurationSeconds = exhaleSeconds,
                updatedAt = Instant.now()
            )
        )
    }

    override suspend fun deleteCustomPattern(id: Long) {
        require(id > 0) { "Hanya pola kustom yang boleh dihapus." }
        dao.deletePatternById(id)
    }

    private fun validate(name: String, inhale: Int, exhale: Int) {
        require(name.trim().length in 1..40) { "Nama pola harus 1–40 karakter." }
        require(inhale in 1..60) { "Durasi tarik napas harus 1–60 detik." }
        require(exhale in 1..60) { "Durasi embus napas harus 1–60 detik." }
    }

    private fun CustomBreathingPatternEntity.toDomain() = BreathingPattern(
        key = "custom:$id",
        id = id,
        name = name,
        inhaleSeconds = inhaleDurationSeconds,
        exhaleSeconds = exhaleDurationSeconds
    )
}
