package com.example.selaras.features.screening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.model.ScreeningAnswer
import com.example.selaras.domain.model.ScreeningQuotaStatus
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import com.example.selaras.domain.usecase.screening.GetScreeningQuotaUseCase
import com.example.selaras.domain.usecase.screening.SubmitScreeningUseCase
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScreeningQuotaUiState(
    val quota: ScreeningQuotaStatus? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class ScreeningViewModel(
    private val submitScreeningUseCase: SubmitScreeningUseCase,
    private val screeningRepository: ScreeningRepository,
    private val instrumentVersion: String = DEMO_INSTRUMENT_VERSION,
    private val getScreeningQuotaUseCase: GetScreeningQuotaUseCase =
        GetScreeningQuotaUseCase(screeningRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreeningUiState())
    val uiState: StateFlow<ScreeningUiState> = _uiState.asStateFlow()

    private val _quotaUiState = MutableStateFlow(ScreeningQuotaUiState())
    val quotaUiState: StateFlow<ScreeningQuotaUiState> = _quotaUiState.asStateFlow()

    private var quotaRefreshJob: Job? = null
    private var submissionJob: Job? = null
    private var pendingSavedResultId: Long? = null

    init {
        refreshQuota()
    }

    fun refreshQuota() {
        quotaRefreshJob?.cancel()
        quotaRefreshJob = viewModelScope.launch {
            _quotaUiState.update {
                it.copy(isLoading = true, errorMessage = null)
            }

            try {
                val quota = getScreeningQuotaUseCase()
                _quotaUiState.value = ScreeningQuotaUiState(
                    quota = quota,
                    isLoading = false
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _quotaUiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = exception.message ?: "Gagal memuat kuota skrining"
                    )
                }
            }
        }
    }

    fun startScreening(isRoutine: Boolean) {
        val screeningType = if (isRoutine) ScreeningType.ROUTINE else ScreeningType.MANUAL
        val quotaState = _quotaUiState.value
        val quota = quotaState.quota

        if (quotaState.isLoading || quotaState.errorMessage != null || quota == null) {
            if (!quotaState.isLoading) refreshQuota()
            return
        }

        if (!quota.isAvailable(screeningType)) {
            refreshQuota()
            return
        }

        pendingSavedResultId = null
        _uiState.value = ScreeningUiState(
            step = ScreeningStep.INTRODUCTION,
            selectedType = screeningType
        )
    }

    fun beginQuestions() {
        _uiState.update { state ->
            if (state.step != ScreeningStep.INTRODUCTION || state.selectedType == null) {
                state
            } else {
                state.copy(step = ScreeningStep.QUESTIONS, errorMessage = null)
            }
        }
    }

    fun selectAnswer(questionNumber: Int, answerValue: Int) {
        _uiState.update { state ->
            if (
                state.step != ScreeningStep.QUESTIONS ||
                questionNumber != state.currentQuestionNumber ||
                answerValue !in MIN_ANSWER..MAX_ANSWER
            ) {
                state
            } else {
                state.copy(
                    answers = state.answers + (questionNumber to answerValue),
                    errorMessage = null
                )
            }
        }
    }

    fun nextQuestion() {
        _uiState.update { state ->
            if (state.step == ScreeningStep.QUESTIONS && state.canGoToNextQuestion) {
                state.copy(currentQuestionNumber = state.currentQuestionNumber + 1)
            } else {
                state
            }
        }
    }

    fun previousQuestion() {
        _uiState.update { state ->
            if (state.step == ScreeningStep.QUESTIONS && state.canGoToPreviousQuestion) {
                state.copy(currentQuestionNumber = state.currentQuestionNumber - 1)
            } else {
                state
            }
        }
    }

    fun submitScreening() {
        val currentState = _uiState.value
        if (currentState.step != ScreeningStep.QUESTIONS && currentState.step != ScreeningStep.ERROR) return
        if (!currentState.canSubmit) return

        val screeningType = currentState.selectedType ?: return
        val answers = currentState.answers.toSortedMap().map { (questionNumber, answerValue) ->
            ScreeningAnswer(
                questionNumber = questionNumber,
                answerValue = answerValue
            )
        }

        _uiState.update {
            it.copy(step = ScreeningStep.PROCESSING, errorMessage = null)
        }

        submissionJob = viewModelScope.launch {
            try {
                val latestQuota = getScreeningQuotaUseCase()
                _quotaUiState.value = ScreeningQuotaUiState(
                    quota = latestQuota,
                    isLoading = false
                )

                if (pendingSavedResultId == null && !latestQuota.isAvailable(screeningType)) {
                    _uiState.update {
                        it.copy(
                            step = ScreeningStep.ERROR,
                            errorMessage = quotaUnavailableMessage(screeningType)
                        )
                    }
                    return@launch
                }

                val today = LocalDate.now()
                val resultId = pendingSavedResultId ?: submitScreeningUseCase(
                    screeningType = screeningType,
                    periodStartDate = today.minusDays(13),
                    periodEndDate = today,
                    instrumentVersion = instrumentVersion,
                    answers = answers
                ).getOrThrow().also {
                    pendingSavedResultId = it
                }

                val result = screeningRepository.getScreeningResultById(resultId)
                    ?: throw IllegalStateException(
                        "Data berhasil dikirim, tetapi hasil belum dapat dibaca kembali. Tekan coba lagi untuk memuat hasil tanpa mengirim ulang."
                    )

                pendingSavedResultId = null
                refreshQuota()

                _uiState.update {
                    it.copy(
                        step = if (!isDemoMode && result.helpIndicator) {
                            ScreeningStep.CRISIS
                        } else {
                            ScreeningStep.RESULT
                        },
                        result = result,
                        errorMessage = null
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                refreshQuota()
                _uiState.update {
                    it.copy(
                        step = ScreeningStep.ERROR,
                        errorMessage = exception.message ?: "Gagal menyimpan hasil refleksi"
                    )
                }
            }
        }
    }

    fun returnToQuestions() {
        _uiState.update { state ->
            if (state.step == ScreeningStep.ERROR && pendingSavedResultId == null) {
                state.copy(step = ScreeningStep.QUESTIONS, errorMessage = null)
            } else {
                state
            }
        }
    }

    fun continueToResult() {
        _uiState.update { state ->
            if (state.step == ScreeningStep.CRISIS && state.result != null) {
                state.copy(step = ScreeningStep.RESULT)
            } else {
                state
            }
        }
    }

    fun resetScreening() {
        submissionJob?.cancel()
        submissionJob = null
        pendingSavedResultId = null
        _uiState.value = ScreeningUiState()
        refreshQuota()
    }

    private fun quotaUnavailableMessage(type: ScreeningType): String {
        return when (type) {
            ScreeningType.MANUAL -> "Kuota sesi manual minggu ini sudah digunakan. Coba kembali minggu depan."
            ScreeningType.ROUTINE -> "Kuota sesi rutin minggu ini sudah digunakan. Coba kembali minggu depan."
        }
    }

    private val isDemoMode: Boolean
        get() = instrumentVersion.startsWith(DEMO_INSTRUMENT_PREFIX, ignoreCase = true)

    companion object {
        const val DEMO_INSTRUMENT_VERSION = "DEMO-REFLECTION-v1"
        private const val DEMO_INSTRUMENT_PREFIX = "DEMO-"
        private const val MIN_ANSWER = 0
        private const val MAX_ANSWER = 3
    }
}
