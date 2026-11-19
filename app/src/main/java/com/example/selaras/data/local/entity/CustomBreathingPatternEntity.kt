package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "custom_breathing_patterns")
data class CustomBreathingPatternEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "inhale_duration_seconds")
    val inhaleDurationSeconds: Int,

    @ColumnInfo(name = "exhale_duration_seconds")
    val exhaleDurationSeconds: Int,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
)
