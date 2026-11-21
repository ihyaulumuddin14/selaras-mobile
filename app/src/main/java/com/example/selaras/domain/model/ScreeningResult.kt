package com.example.selaras.domain.model

import java.time.Instant
import java.time.LocalDate

data class ScreeningResult(
    val id: Long = 0,
    val screeningType: ScreeningType,
    val periodStartDate: LocalDate,
    val periodEndDate: LocalDate,
    val instrumentVersion: String,
    val completedAt: Instant = Instant.now(),
    val totalScore: Int,
    val category: ScreeningCategory,
    val helpIndicator: Boolean,
    val createdAt: Instant = Instant.now(),
    val answers: List<ScreeningAnswer> = emptyList()
) {
    init {
        require(totalScore in 0..27) { "PHQ-9 total score must be between 0 and 27" }
    }
}

data class ScreeningAnswer(
    val id: Long = 0,
    val screeningResultId: Long = 0,
    val questionNumber: Int,
    val answerValue: Int
) {
    init {
        require(questionNumber in 1..9) { "PHQ-9 question number must be between 1 and 9" }
        require(answerValue in 0..3) { "PHQ-9 answer value must be between 0 and 3" }
    }
}
