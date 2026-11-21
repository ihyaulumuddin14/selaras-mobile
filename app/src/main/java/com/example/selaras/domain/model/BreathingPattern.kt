package com.example.selaras.domain.model

import java.time.Instant
import java.util.UUID

/**
 * Domain model for a breathing exercise pattern.
 *
 * Built-in patterns have stable keys (e.g. "default:relaksasi") and [id] == 0.
 * Custom patterns have a local database [id], a globally unique [uuid], and [key] == "custom:$id".
 */
data class BreathingPattern(
    val key: String,
    val name: String,
    val inhaleSeconds: Int,
    val exhaleSeconds: Int,
    val id: Long = 0L,
    val uuid: String = UUID.randomUUID().toString(),
    val holdAfterInhaleSeconds: Int = 0,
    val holdAfterExhaleSeconds: Int = 0,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    init {
        require(name.isNotBlank()) { "Nama pola tidak boleh kosong." }
        require(name.trim().length in 1..40) { "Nama pola harus 1–40 karakter." }
        require(inhaleSeconds in 1..60) { "Durasi tarik napas harus 1–60 detik." }
        require(exhaleSeconds in 1..60) { "Durasi embus napas harus 1–60 detik." }
        require(holdAfterInhaleSeconds in 0..60) { "Durasi jeda tarik napas harus 0–60 detik." }
        require(holdAfterExhaleSeconds in 0..60) { "Durasi jeda embus napas harus 0–60 detik." }
    }

    val isCustom: Boolean get() = !key.startsWith("default:")

    companion object {
        val defaults: List<BreathingPattern> = listOf(
            BreathingPattern("default:relaksasi", "Relaksasi Santai", 4, 6),
            BreathingPattern("default:seimbang", "Ritme Seimbang", 5, 5),
            BreathingPattern("default:lembut", "Napas Lembut", 3, 5),
            BreathingPattern("default:perlahan", "Ritme Perlahan", 5, 7)
        )
    }
}
