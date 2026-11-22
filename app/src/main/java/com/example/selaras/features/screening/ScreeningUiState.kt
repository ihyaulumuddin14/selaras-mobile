
package com.example.selaras.features.screening

import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType

enum class ScreeningStep {
    SELECTION,
    INTRODUCTION,
    QUESTIONS,
    PROCESSING,
    CRISIS,
    RESULT,
    ERROR
}

data class ScreeningUiState(
    val step: ScreeningStep = ScreeningStep.SELECTION,
    val selectedType: ScreeningType? = null,
    val answers: Map<Int, Int> = emptyMap(),
    val currentQuestionNumber: Int = 1,
    val result: ScreeningResult? = null,
    val errorMessage: String? = null
) {
    val currentAnswer: Int?
        get() = answers[currentQuestionNumber]

    val canGoToPreviousQuestion: Boolean
        get() = currentQuestionNumber > 1

    val canGoToNextQuestion: Boolean
        get() = currentQuestionNumber < 9 && currentAnswer != null

    val hasAnsweredAllQuestions: Boolean
        get() = (1..9).all { answers.containsKey(it) }

    val canSubmit: Boolean
        get() = hasAnsweredAllQuestions

    val isProcessing: Boolean
        get() = step == ScreeningStep.PROCESSING
}
