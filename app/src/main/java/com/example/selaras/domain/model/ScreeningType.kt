package com.example.selaras.domain.model

enum class ScreeningType(val value: String) {
    MANUAL("manual"),
    ROUTINE("routine");

    companion object {
        fun fromValue(value: String): ScreeningType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown ScreeningType value: $value")
        }
    }
}
