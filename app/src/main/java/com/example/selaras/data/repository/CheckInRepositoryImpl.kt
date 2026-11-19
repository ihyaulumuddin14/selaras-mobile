package com.example.selaras.data.repository

import com.example.selaras.data.datasource.local.CheckInLocalDataSource
import com.example.selaras.data.local.entity.CheckInEntity
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class CheckInRepositoryImpl(
    private val localDataSource: CheckInLocalDataSource
) : CheckInRepository {

    override suspend fun saveCheckIn(checkIn: CheckIn): Result<Long> = runCatching {
        localDataSource.insertCheckIn(checkIn.toEntity())
    }

    override suspend fun updateCheckIn(checkIn: CheckIn): Result<Unit> = runCatching {
        localDataSource.updateCheckIn(checkIn.toEntity())
    }

    override suspend fun deleteCheckIn(checkIn: CheckIn): Result<Unit> = runCatching {
        localDataSource.deleteCheckIn(checkIn.toEntity())
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

    private fun CheckIn.toEntity(): CheckInEntity {
        return CheckInEntity(
            id = id,
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
