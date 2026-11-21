package com.example.selaras.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CheckIn(
    val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val checkInDate: LocalDate,
    val mood: Int,
    val energy: Int,
    val sleepDurationMinutes: Int,
    val note: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(mood in 1..5) { "Mood must be between 1 and 5" }
        require(energy in 1..10) { "Energy must be between 1 and 10" }
        require(sleepDurationMinutes >= 0) { "Sleep duration must be non-negative" }
        note?.let {
            require(it.length <= 500) { "Catatan tidak boleh lebih dari 500 karakter" }
        }
    }

    val moodLabel: String
        get() = when (mood) {
            1 -> "Sangat Kurang Baik"
            2 -> "Kurang Baik"
            3 -> "Biasa Saja"
            4 -> "Baik"
            5 -> "Sangat Baik"
            else -> "Biasa Saja"
        }

    val sleepHours: Int
        get() = sleepDurationMinutes / 60

    val sleepRemainingMinutes: Int
        get() = sleepDurationMinutes % 60
}
