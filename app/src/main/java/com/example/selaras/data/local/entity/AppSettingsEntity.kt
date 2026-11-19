package com.example.selaras.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalTime

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Long = SINGLETON_ID,

    @ColumnInfo(name = "first_activated_at")
    val firstActivatedAt: Instant,

    @ColumnInfo(name = "onboarding_completed", defaultValue = "0")
    val onboardingCompleted: Boolean = false,

    @ColumnInfo(name = "consent_accepted_at")
    val consentAcceptedAt: Instant? = null,

    @ColumnInfo(name = "checkin_reminder_enabled", defaultValue = "0")
    val checkinReminderEnabled: Boolean = false,

    @ColumnInfo(name = "checkin_reminder_time")
    val checkinReminderTime: LocalTime? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Instant,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant
) {
    companion object {
        const val SINGLETON_ID: Long = 1L
    }
}
