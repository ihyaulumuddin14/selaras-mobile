package com.example.selaras.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.domain.repository.AppSettingsRepository
import com.example.selaras.domain.usecase.onboarding.AcceptConsentUseCase
import com.example.selaras.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.example.selaras.domain.usecase.onboarding.GetAppSettingsUseCase
import com.example.selaras.domain.usecase.onboarding.InitializeAppSettingsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingDestination {
    SPLASH,
    ONBOARDING_CAROUSEL,
    INFORMED_CONSENT,
    NAVIGATE_TO_DASHBOARD,
    NAVIGATE_TO_APP_LOCK
}

data class OnboardingSlideData(
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val iconDescription: String
)

data class OnboardingUiState(
    val currentDestination: OnboardingDestination = OnboardingDestination.SPLASH,
    val carouselIndex: Int = 0,
    val isConsentChecked: Boolean = false,
    val isConsentAccepted: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val totalSlides: Int = 4
    val isLastSlide: Boolean
        get() = carouselIndex == totalSlides - 1
}

class OnboardingViewModel(
    private val initializeAppSettingsUseCase: InitializeAppSettingsUseCase,
    private val getAppSettingsUseCase: GetAppSettingsUseCase,
    private val acceptConsentUseCase: AcceptConsentUseCase,
    private val completeOnboardingUseCase: CompleteOnboardingUseCase,
    private val isPinSetProvider: () -> Boolean = { false }
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    val slides = listOf(
        OnboardingSlideData(
            categoryBadge = "Selamat Datang",
            title = "Ruang Aman untuk Kesejahteraan Emosional",
            subtitle = "Selaras hadir sebagai pendamping mandiri untuk merawat ketenangan pikiran, mengenali suasana hati, dan memulihkan ritme hidup Anda.",
            iconDescription = "🌱"
        ),
        OnboardingSlideData(
            categoryBadge = "Kenali Harimu",
            title = "Pencatatan Harian yang Sederhana & Penuh Makna",
            subtitle = "Pantau suasana hati (skala 1–5), tingkat energi (skala 1–10), dan durasi tidur malam Anda untuk melihat pola kesehatan emosional berkala.",
            iconDescription = "☀️"
        ),
        OnboardingSlideData(
            categoryBadge = "Ruang Bernapas",
            title = "Relaksasi Pernapasan Kapan Pun Dibutuhkan",
            subtitle = "Latihan ritmis terbukti klinis (4-7-8, Box Breathing) atau rancang pola kustom Anda sendiri untuk meredakan kecemasan dan stres.",
            iconDescription = "💨"
        ),
        OnboardingSlideData(
            categoryBadge = "Pahami Dirimu",
            title = "Penapisan PHQ-9 & Ringkasan Konselor",
            subtitle = "Evaluasi mandiri dengan instrumen baku depresi PHQ-9 serta cetak ringkasan rekam mandiri format PDF untuk sesi konsultasi profesional.",
            iconDescription = "📊"
        )
    )

    fun checkAppLaunchState(splashDelayMs: Long = 1000L, forceOnboarding: Boolean = false): Job {
        return viewModelScope.launch {
            if (splashDelayMs > 0) {
                delay(splashDelayMs)
            }
            if (forceOnboarding) {
                _uiState.update { it.copy(currentDestination = OnboardingDestination.ONBOARDING_CAROUSEL) }
                return@launch
            }
            val settings = initializeAppSettingsUseCase().getOrNull()
            if (settings != null && settings.onboardingCompleted && settings.isConsentAccepted) {
                if (isPinSetProvider()) {
                    _uiState.update { it.copy(currentDestination = OnboardingDestination.NAVIGATE_TO_APP_LOCK) }
                } else {
                    _uiState.update { it.copy(currentDestination = OnboardingDestination.NAVIGATE_TO_DASHBOARD) }
                }
            } else if (settings != null && settings.isConsentAccepted) {
                _uiState.update { it.copy(currentDestination = OnboardingDestination.ONBOARDING_CAROUSEL) }
            } else {
                _uiState.update { it.copy(currentDestination = OnboardingDestination.ONBOARDING_CAROUSEL) }
            }
        }
    }

    fun restartForTesting() {
        _uiState.update {
            it.copy(
                currentDestination = OnboardingDestination.SPLASH,
                carouselIndex = 0,
                isConsentChecked = false,
                isConsentAccepted = false,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    /**
     * Resets the onboarding state back to slide 1 of the carousel (used when clearing all data).
     */
    fun resetToOnboarding() {
        _uiState.value = OnboardingUiState(
            currentDestination = OnboardingDestination.ONBOARDING_CAROUSEL,
            carouselIndex = 0,
            isConsentChecked = false,
            isConsentAccepted = false,
            isOnboardingCompleted = false,
            isLoading = false,
            errorMessage = null
        )
    }

    fun nextSlide() {
        val current = _uiState.value.carouselIndex
        if (current < _uiState.value.totalSlides - 1) {
            _uiState.update { it.copy(carouselIndex = current + 1) }
        } else {
            proceedToConsent()
        }
    }

    fun prevSlide() {
        val current = _uiState.value.carouselIndex
        if (current > 0) {
            _uiState.update { it.copy(carouselIndex = current - 1) }
        }
    }

    fun setSlideIndex(index: Int) {
        if (index in 0 until _uiState.value.totalSlides) {
            _uiState.update { it.copy(carouselIndex = index) }
        }
    }

    fun skipToConsent() {
        proceedToConsent()
    }

    private fun proceedToConsent() {
        _uiState.update { it.copy(currentDestination = OnboardingDestination.INFORMED_CONSENT) }
    }

    fun backToCarousel() {
        _uiState.update {
            it.copy(
                currentDestination = OnboardingDestination.ONBOARDING_CAROUSEL,
                carouselIndex = it.totalSlides - 1
            )
        }
    }

    fun setConsentChecked(checked: Boolean) {
        _uiState.update { it.copy(isConsentChecked = checked, errorMessage = null) }
    }

    fun submitConsentAndComplete(): Job {
        val state = _uiState.value
        if (!state.isConsentChecked || state.isLoading) {
            return Job().apply { complete() }
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        return viewModelScope.launch {
            val acceptResult = acceptConsentUseCase()
            if (acceptResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal menyimpan persetujuan: ${acceptResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            val completeResult = completeOnboardingUseCase()
            if (completeResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gagal menyelesaikan orientasi: ${completeResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isConsentAccepted = true,
                    isOnboardingCompleted = true,
                    currentDestination = OnboardingDestination.NAVIGATE_TO_DASHBOARD
                )
            }
        }
    }

    class Factory(
        private val repository: AppSettingsRepository,
        private val isPinSetProvider: () -> Boolean = { false }
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            val init = InitializeAppSettingsUseCase(repository)
            val get = GetAppSettingsUseCase(repository)
            val accept = AcceptConsentUseCase(repository)
            val complete = CompleteOnboardingUseCase(repository)
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(init, get, accept, complete, isPinSetProvider) as T
        }
    }
}
