package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(
    tableName = "custom_breathing_patterns",
    indices = [
        Index(value = ["uuid"], unique = true)
    ]
)
data class CustomBreathingPatternEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "uuid")
    val uuid: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "user_id")
    val userId: String? = null,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "inhale_duration_seconds")
    val inhaleDurationSeconds: Int,

    @ColumnInfo(name = "exhale_duration_seconds")
    val exhaleDurationSeconds: Int,

    @ColumnInfo(name = "sync_status", defaultValue = "'SYNCED'")
    val syncStatus: String = "SYNCED",

    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
