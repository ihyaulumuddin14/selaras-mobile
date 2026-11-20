package com.example.selaras.domain.model

import java.time.Instant
import java.time.LocalTime

data class AppSettings(
    val id: Long = 1L,
    val firstActivatedAt: Instant = Instant.now(),
    val onboardingCompleted: Boolean = false,
    val consentAcceptedAt: Instant? = null,
    val checkinReminderEnabled: Boolean = false,
    val checkinReminderTime: LocalTime? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isConsentAccepted: Boolean
        get() = consentAcceptedAt != null
}
