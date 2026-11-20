package com.example.selaras.domain.usecase.checkin

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.repository.CheckInRepository
import java.time.LocalDate

class SaveCheckInUseCase(
    private val repository: CheckInRepository
) {
    suspend operator fun invoke(checkIn: CheckIn): Result<CheckIn> = runCatching {
        require(!checkIn.checkInDate.isAfter(LocalDate.now())) {
            "Tanggal check-in tidak boleh di masa depan"
        }

        val existing = repository.getCheckInByDate(checkIn.checkInDate)
        if (existing != null) {
            val updated = checkIn.copy(
                id = existing.id,
                uuid = existing.uuid,
                createdAt = existing.createdAt
            )
            repository.updateCheckIn(updated).getOrThrow()
            updated
        } else {
            repository.saveCheckIn(checkIn).getOrThrow()
        }
    }
}
