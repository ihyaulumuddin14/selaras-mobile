package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.model.ScreeningType
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "screening_results",
    indices = [
        Index(
            value = ["screening_type", "period_start_date", "period_end_date"],
            unique = true
        ),
        Index(value = ["completed_at"])
    ]
)
data class ScreeningResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "screening_type")
    val screeningType: ScreeningType,

    @ColumnInfo(name = "period_start_date")
    val periodStartDate: LocalDate,

    @ColumnInfo(name = "period_end_date")
    val periodEndDate: LocalDate,

    @ColumnInfo(name = "instrument_version")
    val instrumentVersion: String,

    @ColumnInfo(name = "completed_at")
    val completedAt: Instant,

    @ColumnInfo(name = "total_score")
    val totalScore: Int,

    @ColumnInfo(name = "category")
    val category: ScreeningCategory,

    @ColumnInfo(name = "help_indicator")
    val helpIndicator: Boolean,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
