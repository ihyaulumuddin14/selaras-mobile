package com.example.selaras.domain.repository

import com.example.selaras.domain.model.CheckIn
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface CheckInRepository {
    suspend fun saveCheckIn(checkIn: CheckIn): Result<Long>
    suspend fun updateCheckIn(checkIn: CheckIn): Result<Unit>
    suspend fun deleteCheckIn(checkIn: CheckIn): Result<Unit>
    suspend fun getCheckInByDate(date: LocalDate): CheckIn?
    fun getCheckInByDateFlow(date: LocalDate): Flow<CheckIn?>
    suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckIn>
    fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckIn>>
    fun getAllCheckInsFlow(): Flow<List<CheckIn>>
    suspend fun getLatestCheckIn(): CheckIn?
    suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int
}
