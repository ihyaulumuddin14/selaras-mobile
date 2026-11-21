package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity(
    tableName = "check_ins",
    indices = [
        Index(value = ["check_in_date"], unique = true),
        Index(value = ["uuid"], unique = true)
    ]
)
data class CheckInEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "check_in_date")
    val checkInDate: LocalDate,

    @ColumnInfo(name = "mood")
    val mood: Int,

    @ColumnInfo(name = "energy")
    val energy: Int,

    @ColumnInfo(name = "sleep_duration_minutes")
    val sleepDurationMinutes: Int,

    @ColumnInfo(name = "note")
    val note: String? = null,

    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
