package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CheckInDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCheckIn(checkIn: CheckInEntity): Long

    @Update
    suspend fun updateCheckIn(checkIn: CheckInEntity)

    @Delete
    suspend fun deleteCheckIn(checkIn: CheckInEntity)

    @Query("DELETE FROM check_ins WHERE id = :id")
    suspend fun deleteCheckInById(id: Long)

    @Query("SELECT * FROM check_ins WHERE id = :id LIMIT 1")
    suspend fun getCheckInById(id: Long): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE check_in_date = :date LIMIT 1")
    suspend fun getCheckInByDate(date: LocalDate): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE check_in_date = :date LIMIT 1")
    fun getCheckInByDateFlow(date: LocalDate): Flow<CheckInEntity?>

    @Query("SELECT * FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate ORDER BY check_in_date ASC")
    suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate ORDER BY check_in_date ASC")
    fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY check_in_date DESC")
    fun getAllCheckInsFlow(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins ORDER BY check_in_date DESC LIMIT 1")
    suspend fun getLatestCheckIn(): CheckInEntity?

    @Query("SELECT COUNT(*) FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate")
    suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int
}
