package com.example.selaras.data.datasource.local

import com.example.selaras.core.database.dao.CheckInDao
import com.example.selaras.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class CheckInLocalDataSource(
    private val checkInDao: CheckInDao
) {
    suspend fun insertCheckIn(checkIn: CheckInEntity): Long = checkInDao.insertCheckIn(checkIn)

    suspend fun updateCheckIn(checkIn: CheckInEntity) = checkInDao.updateCheckIn(checkIn)

    suspend fun deleteCheckIn(checkIn: CheckInEntity) = checkInDao.deleteCheckIn(checkIn)

    suspend fun softDeleteCheckIn(id: Long, now: Instant = Instant.now()) =
        checkInDao.softDeleteCheckInById(id, now)

    suspend fun hardDeleteCheckIn(id: Long) = checkInDao.deleteCheckInById(id)

    suspend fun getCheckInById(id: Long): CheckInEntity? = checkInDao.getCheckInById(id)

    suspend fun getCheckInByUuid(uuid: String): CheckInEntity? = checkInDao.getCheckInByUuid(uuid)

    suspend fun getCheckInByDate(date: LocalDate): CheckInEntity? = checkInDao.getCheckInByDate(date)

    fun getCheckInByDateFlow(date: LocalDate): Flow<CheckInEntity?> = checkInDao.getCheckInByDateFlow(date)

    suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckInEntity> =
        checkInDao.getCheckInsBetween(startDate, endDate)

    fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>> =
        checkInDao.getCheckInsBetweenFlow(startDate, endDate)

    fun getAllCheckInsFlow(): Flow<List<CheckInEntity>> = checkInDao.getAllCheckInsFlow()

    suspend fun getLatestCheckIn(): CheckInEntity? = checkInDao.getLatestCheckIn()

    suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int =
        checkInDao.countCheckInsBetween(startDate, endDate)

    suspend fun getPendingSyncCheckIns(): List<CheckInEntity> = checkInDao.getPendingSyncCheckIns()

    suspend fun updateSyncStatus(id: Long, syncStatus: String, now: Instant = Instant.now()) =
        checkInDao.updateSyncStatus(id, syncStatus, now)
}
