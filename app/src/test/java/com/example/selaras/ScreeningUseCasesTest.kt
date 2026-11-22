
package com.example.selaras

import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningCategory
import com.example.selaras.domain.usecase.screening.CalculatePhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.ClassifyPhq9ScoreUseCase
import com.example.selaras.domain.usecase.screening.CheckCrisisIndicatorUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreeningUseCasesTest {

    private val calculateScore = CalculatePhq9ScoreUseCase()
    private val classifyScore = ClassifyPhq9ScoreUseCase()
    private val checkCrisis = CheckCrisisIndicatorUseCase()

    private fun createAnswers(
        values: List<Int> = List(9) { 0 }
    ): List<ScreeningAnswer> {
        return values.mapIndexed { index, value ->
            ScreeningAnswer(
                questionNumber = index + 1,
                answerValue = value
            )
        }
    }

    @Test
    fun calculateScore_returnsCorrectTotal() {
        val answers = createAnswers(
            listOf(0, 1, 2, 3, 0, 1, 2, 3, 0)
        )

        assertEquals(12, calculateScore(answers))
    }

    @Test
    fun calculateScore_returnsZeroWhenAllAnswersAreZero() {
        assertEquals(0, calculateScore(createAnswers()))
    }

    @Test
    fun calculateScore_returnsMaximumScore() {
        assertEquals(
            27,
            calculateScore(createAnswers(List(9) { 3 }))
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculateScore_rejectsIncompleteAnswers() {
        calculateScore(createAnswers(List(8) { 1 }))
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculateScore_rejectsDuplicateQuestionNumbers() {
        val answers = (1..9).map {
            ScreeningAnswer(
                questionNumber = if (it == 9) 8 else it,
                answerValue = 0
            )
        }

        calculateScore(answers)
    }

    @Test
    fun classifyScore_returnsCorrectCategories() {
        assertEquals(ScreeningCategory.MINIMAL, classifyScore(0))
        assertEquals(ScreeningCategory.MINIMAL, classifyScore(4))
        assertEquals(ScreeningCategory.RINGAN, classifyScore(5))
        assertEquals(ScreeningCategory.RINGAN, classifyScore(9))
        assertEquals(ScreeningCategory.SEDANG, classifyScore(10))
        assertEquals(ScreeningCategory.SEDANG, classifyScore(14))
        assertEquals(ScreeningCategory.SEDANG_BERAT, classifyScore(15))
        assertEquals(ScreeningCategory.SEDANG_BERAT, classifyScore(19))
        assertEquals(ScreeningCategory.BERAT, classifyScore(20))
        assertEquals(ScreeningCategory.BERAT, classifyScore(27))
    }

    @Test(expected = IllegalArgumentException::class)
    fun classifyScore_rejectsNegativeScore() {
        classifyScore(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun classifyScore_rejectsScoreAboveMaximum() {
        classifyScore(28)
    }

    @Test
    fun checkCrisis_returnsTrueWhenQuestionNineIsPositive() {
        val answers = createAnswers(
            listOf(0, 0, 0, 0, 0, 0, 0, 0, 1)
        )

        assertTrue(checkCrisis(answers, 1))
    }

    @Test
    fun checkCrisis_returnsTrueWhenTotalScoreIsAtLeastTwenty() {
        val answers = createAnswers(
            listOf(3, 3, 3, 3, 3, 2, 2, 1, 0)
        )

        assertTrue(checkCrisis(answers, 20))
    }

    @Test
    fun checkCrisis_returnsFalseWhenBothConditionsAreFalse() {
        val answers = createAnswers(
            listOf(2, 2, 2, 2, 2, 2, 2, 2, 0)
        )

        assertFalse(checkCrisis(answers, 16))
    }

    @Test(expected = IllegalArgumentException::class)
    fun checkCrisis_rejectsMissingQuestionNine() {
        val answers = (1..9).map {
            ScreeningAnswer(
                questionNumber = if (it == 9) 8 else it,
                answerValue = 0
            )
        }

        checkCrisis(answers, 0)
    }
}
