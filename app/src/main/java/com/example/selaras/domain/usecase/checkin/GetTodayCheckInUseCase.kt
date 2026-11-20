package com.example.selaras.domain.usecase.checkin

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.repository.CheckInRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class GetTodayCheckInUseCase(
    private val repository: CheckInRepository
) {
    operator fun invoke(date: LocalDate = LocalDate.now()): Flow<CheckIn?> {
        return repository.getCheckInByDateFlow(date)
    }

    suspend fun getOnce(date: LocalDate = LocalDate.now()): CheckIn? {
        return repository.getCheckInByDate(date)
    }
}
