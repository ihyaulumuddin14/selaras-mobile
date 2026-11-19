package com.example.selaras.domain.model

import java.time.Instant
import java.time.LocalDate

data class CheckIn(
    val id: Long = 0,
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
    }
}
