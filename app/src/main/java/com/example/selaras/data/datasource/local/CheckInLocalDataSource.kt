package com.example.selaras.data.datasource.local

import com.example.selaras.core.database.dao.CheckInDao
import com.example.selaras.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class CheckInLocalDataSource(
    private val checkInDao: CheckInDao
) {
    suspend fun insertCheckIn(checkIn: CheckInEntity): Long = checkInDao.insertCheckIn(checkIn)

    suspend fun updateCheckIn(checkIn: CheckInEntity) = checkInDao.updateCheckIn(checkIn)

    suspend fun deleteCheckIn(checkIn: CheckInEntity) = checkInDao.deleteCheckIn(checkIn)

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
}
