package com.example.selaras.domain.model

enum class ScreeningCategory(val value: String) {
    MINIMAL("minimal"),
    RINGAN("ringan"),
    SEDANG("sedang"),
    SEDANG_BERAT("sedang_berat"),
    BERAT("berat");

    companion object {
        fun fromValue(value: String): ScreeningCategory {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown ScreeningCategory value: $value")
        }
    }
}
