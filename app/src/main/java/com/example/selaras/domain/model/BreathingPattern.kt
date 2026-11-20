package com.example.selaras.domain.model

/** A custom pattern has a database [id]; built-in patterns use a stable [key]. */
data class BreathingPattern(
    val key: String,
    val name: String,
    val inhaleSeconds: Int,
    val exhaleSeconds: Int,
    val id: Long? = null,
    val holdAfterInhaleSeconds: Int = 0,
    val holdAfterExhaleSeconds: Int = 0
) {
    init {
        require(name.isNotBlank()) { "Nama pola tidak boleh kosong." }
        require(inhaleSeconds in 1..60) { "Durasi tarik napas harus 1–60 detik." }
        require(exhaleSeconds in 1..60) { "Durasi embus napas harus 1–60 detik." }
        require(holdAfterInhaleSeconds in 0..60)
        require(holdAfterExhaleSeconds in 0..60)
    }

    val isCustom: Boolean get() = id != null

    companion object {
        val defaults: List<BreathingPattern> = listOf(
            BreathingPattern("default:relaksasi", "Relaksasi Santai", 4, 6),
            BreathingPattern("default:seimbang", "Ritme Seimbang", 5, 5),
            BreathingPattern("default:lembut", "Napas Lembut", 3, 5),
            BreathingPattern("default:perlahan", "Ritme Perlahan", 5, 7)
        )
    }
}
