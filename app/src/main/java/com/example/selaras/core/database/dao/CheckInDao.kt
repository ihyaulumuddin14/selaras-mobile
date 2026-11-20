package com.example.selaras.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.selaras.data.local.entity.CheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
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

    @Query("UPDATE check_ins SET is_deleted = 1, sync_status = 'PENDING_DELETE', updated_at = :now WHERE id = :id")
    suspend fun softDeleteCheckInById(id: Long, now: Instant)

    @Query("UPDATE check_ins SET sync_status = :syncStatus, updated_at = :now WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String, now: Instant)

    @Query("SELECT * FROM check_ins WHERE id = :id AND is_deleted = 0 LIMIT 1")
    suspend fun getCheckInById(id: Long): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE uuid = :uuid AND is_deleted = 0 LIMIT 1")
    suspend fun getCheckInByUuid(uuid: String): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE check_in_date = :date AND is_deleted = 0 LIMIT 1")
    suspend fun getCheckInByDate(date: LocalDate): CheckInEntity?

    @Query("SELECT * FROM check_ins WHERE check_in_date = :date AND is_deleted = 0 LIMIT 1")
    fun getCheckInByDateFlow(date: LocalDate): Flow<CheckInEntity?>

    @Query("SELECT * FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY check_in_date ASC")
    suspend fun getCheckInsBetween(startDate: LocalDate, endDate: LocalDate): List<CheckInEntity>

    @Query("SELECT * FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY check_in_date ASC")
    fun getCheckInsBetweenFlow(startDate: LocalDate, endDate: LocalDate): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE is_deleted = 0 ORDER BY check_in_date DESC")
    fun getAllCheckInsFlow(): Flow<List<CheckInEntity>>

    @Query("SELECT * FROM check_ins WHERE is_deleted = 0 ORDER BY check_in_date DESC LIMIT 1")
    suspend fun getLatestCheckIn(): CheckInEntity?

    @Query("SELECT COUNT(*) FROM check_ins WHERE check_in_date BETWEEN :startDate AND :endDate AND is_deleted = 0")
    suspend fun countCheckInsBetween(startDate: LocalDate, endDate: LocalDate): Int

    @Query("SELECT * FROM check_ins WHERE sync_status != 'SYNCED'")
    suspend fun getPendingSyncCheckIns(): List<CheckInEntity>
}
