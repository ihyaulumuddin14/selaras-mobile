package com.example.selaras.data.repository

import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate

class CheckInRepositoryImpl(
    private val localDataSource: CheckInLocalDataSource
) : CheckInRepository {

    override suspend fun saveCheckIn(checkIn: CheckIn): Result<CheckIn> = runCatching {
        val now = Instant.now()
        val entity = checkIn.toEntity().copy(
            syncStatus = "PENDING_CREATE",
            isDeleted = false,
            createdAt = now,
            updatedAt = now
        )
        val generatedId = localDataSource.insertCheckIn(entity)
        checkIn.copy(id = generatedId, createdAt = now, updatedAt = now)
    }

    override suspend fun updateCheckIn(checkIn: CheckIn): Result<Unit> = runCatching {
        val now = Instant.now()
        val existing = if (checkIn.id > 0) {
            localDataSource.getCheckInById(checkIn.id)
        } else {
            localDataSource.getCheckInByDate(checkIn.checkInDate)
        } ?: throw IllegalArgumentException("Check-in untuk tanggal ${checkIn.checkInDate} tidak ditemukan.")

        val entity = existing.copy(
            mood = checkIn.mood,
            energy = checkIn.energy,
            sleepDurationMinutes = checkIn.sleepDurationMinutes,
            note = checkIn.note,
            syncStatus = "PENDING_UPDATE",
            updatedAt = now
        )
        localDataSource.updateCheckIn(entity)
    }

    override suspend fun deleteCheckIn(checkIn: CheckIn): Result<Unit> = runCatching {
        deleteCheckInById(checkIn.id).getOrThrow()
    }

    override suspend fun deleteCheckInById(id: Long): Result<Unit> = runCatching {
        require(id > 0) { "ID check-in tidak valid untuk dihapus." }
        localDataSource.softDeleteCheckIn(id)
    }

    override suspend fun getCheckInByDate(date: LocalDate): CheckIn? {
        return localDataSource.getCheckInByDate(date)?.toDomain()
    }

    override fun getCheckInByDateFlow(date: LocalDate): Flow<CheckIn?> {
        return localDataSource.getCheckInByDateFlow(date).map { it?.toDomain() }
    }

    override suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckIn> {
        return localDataSource.getCheckInsBetween(startDate, endDate).map { it.toDomain() }
    }

    override fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckIn>> {
        return localDataSource.getCheckInsBetweenFlow(startDate, endDate).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllCheckInsFlow(): Flow<List<CheckIn>> {
        return localDataSource.getAllCheckInsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getLatestCheckIn(): CheckIn? {
        return localDataSource.getLatestCheckIn()?.toDomain()
    }

    override suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int {
        return localDataSource.countCheckInsBetween(startDate, endDate)
    }

    override suspend fun getCheckInByUuid(uuid: String): CheckIn? {
        return localDataSource.getCheckInByUuid(uuid)?.toDomain()
    }

    private fun CheckIn.toEntity(): CheckInEntity {
        return CheckInEntity(
            id = id,
            uuid = uuid,
            checkInDate = checkInDate,
            mood = mood,
            energy = energy,
            sleepDurationMinutes = sleepDurationMinutes,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun CheckInEntity.toDomain(): CheckIn {
        return CheckIn(
            id = id,
            uuid = uuid,
            checkInDate = checkInDate,
            mood = mood,
            energy = energy,
            sleepDurationMinutes = sleepDurationMinutes,
            note = note,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
