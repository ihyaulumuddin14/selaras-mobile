package com.example.selaras.domain.usecase.breathing

import com.example.selaras.domain.repository.BreathingRepository

/** Keeps the filename used by the team's initial skeleton for compatibility. */
class GetBrethingPatternUseCase(private val repository: BreathingRepository) {
    operator fun invoke() = repository.observeCustomPatterns()
}
