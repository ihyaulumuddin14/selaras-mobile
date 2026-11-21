package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "counselor_summaries",
    foreignKeys = [
        ForeignKey(
            entity = ScreeningResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["screening_result_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["screening_result_id"]),
        Index(value = ["created_at"])
    ]
)
data class CounselorSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "screening_result_id")
    val screeningResultId: Long? = null,

    @ColumnInfo(name = "period_start_date")
    val periodStartDate: LocalDate,

    @ColumnInfo(name = "period_end_date")
    val periodEndDate: LocalDate,

    @ColumnInfo(name = "include_checkin_analysis", defaultValue = "0")
    val includeCheckinAnalysis: Boolean = false,

    @ColumnInfo(name = "checkin_days_count", defaultValue = "0")
    val checkinDaysCount: Int = 0,

    @ColumnInfo(name = "rule_version")
    val ruleVersion: String? = null,

    @ColumnInfo(name = "snapshot_json")
    val snapshotJson: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant
)
