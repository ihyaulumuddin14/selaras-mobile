
package com.example.selaras.domain.model

import java.time.LocalDate

data class ScreeningQuotaStatus(
    val weekStartDate: LocalDate,
    val weekEndDate: LocalDate,
    val manualUsed: Boolean,
    val routineUsed: Boolean
) {
    val manualAvailable: Boolean
        get() = !manualUsed

    val routineAvailable: Boolean
        get() = !routineUsed

    val manualRemaining: Int
        get() = if (manualUsed) 0 else 1

    val routineRemaining: Int
        get() = if (routineUsed) 0 else 1

    fun isAvailable(type: ScreeningType): Boolean {
        return when (type) {
            ScreeningType.MANUAL -> manualAvailable
            ScreeningType.ROUTINE -> routineAvailable
        }
    }
}
