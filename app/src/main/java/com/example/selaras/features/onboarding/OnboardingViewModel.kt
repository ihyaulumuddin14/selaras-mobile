package com.example.selaras.features.onboarding

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.selaras.R
import com.example.selaras.core.security.AuthSessionManager
import com.example.selaras.data.datasource.remote.SelarasApiClient
import com.example.selaras.data.datasource.remote.dto.LoginRequestDto
import com.example.selaras.data.datasource.remote.dto.RegisterRequestDto
import com.example.selaras.data.sync.SyncCoordinator
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
    CLOUD_BACKUP_PROMPT,
    NAVIGATE_TO_DASHBOARD,
    NAVIGATE_TO_APP_LOCK
}

data class OnboardingSlideData(
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val iconDescription: String,
    @get:DrawableRes val iconRes: Int = when {
        iconDescription.contains("welcome") -> R.drawable.welcome_slide_icon
        iconDescription.contains("book") -> R.drawable.book_slide_icon
        iconDescription.contains("calm") -> R.drawable.calm_slide_icon
        iconDescription.contains("notes") -> R.drawable.notes_slide_icon
        else -> R.drawable.logo_selaras
    }
)

data class OnboardingUiState(
    val currentDestination: OnboardingDestination = OnboardingDestination.SPLASH,
    val carouselIndex: Int = 0,
    val isConsentChecked: Boolean = false,
    val isConsentAccepted: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Cloud Backup Prompt State
    val isCloudBackupMode: Boolean = true, // true = Register, false = Login
    val backupEmail: String = "",
    val backupPassword: String = "",
    val backupUserName: String = "",
    val isSyncing: Boolean = false,
    val syncErrorMessage: String? = null
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
    private val isPinSetProvider: () -> Boolean = { false },
    private val authSessionManager: AuthSessionManager? = null,
    private val syncCoordinator: SyncCoordinator? = null,
    private val apiClient: SelarasApiClient? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    val slides = listOf(
        OnboardingSlideData(
            categoryBadge = "Selamat Datang di SELARAS",
            title = "Pelan-pelan kenali dirimu",
            subtitle = "Teman untuk memahami perasaan, menemukan jeda, dan merawat diri setiap hari.",
            iconDescription = "welcome_slide_icon.png",
            iconRes = R.drawable.welcome_slide_icon
        ),
        OnboardingSlideData(
            categoryBadge = "Kenali Harimu",
            title = "Apa kabarmu hari ini?",
            subtitle = "Catat suasana hati, energi, dan durasi tidur. Temukan pola dari check-in harianmu.",
            iconDescription = "book_slide_icon.png",
            iconRes = R.drawable.book_slide_icon
        ),
        OnboardingSlideData(
            categoryBadge = "Ruang Bernapas",
            title = "Tarik napas Beri ruang.",
            subtitle = "Ikuti latihan pernapasan sederhana untuk membantu tubuh dan pikiran lebih rileks.",
            iconDescription = "calm_slide_icon.png",
            iconRes = R.drawable.calm_slide_icon
        ),
        OnboardingSlideData(
            categoryBadge = "Pahami Dirimu",
            title = "Pahami pola mulai bercerita.",
            subtitle = "Kenali diri lewat skrining dan ringkasan pribadi. Pilih yang ingin kamu bawa saat konseling.\n" + "Bukan diagnosis atau pengganti tenaga profesional.",
            iconDescription = "notes_slide_icon.png",
            iconRes = R.drawable.notes_slide_icon
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
                errorMessage = null,
                isSyncing = false,
                syncErrorMessage = null
            )
        }
    }

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

    /**
     * Menyimpan persetujuan pengguna (consent) lalu berpindah ke layar penawaran pencadangan awan (opsional).
     */
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

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isConsentAccepted = true,
                    currentDestination = OnboardingDestination.CLOUD_BACKUP_PROMPT
                )
            }
        }
    }

    // ==========================================
    // Cloud Backup Screen Interactions
    // ==========================================

    fun setCloudBackupMode(isRegister: Boolean) {
        _uiState.update { it.copy(isCloudBackupMode = isRegister, syncErrorMessage = null) }
    }

    fun setBackupEmail(email: String) {
        _uiState.update { it.copy(backupEmail = email, syncErrorMessage = null) }
    }

    fun setBackupPassword(password: String) {
        _uiState.update { it.copy(backupPassword = password, syncErrorMessage = null) }
    }

    fun setBackupUserName(userName: String) {
        _uiState.update { it.copy(backupUserName = userName, syncErrorMessage = null) }
    }

    /**
     * Pengguna memilih untuk melewati pencadangan awan (offline-first).
     */
    fun skipCloudBackup(): Job {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        return viewModelScope.launch {
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

            val dest = if (isPinSetProvider()) {
                OnboardingDestination.NAVIGATE_TO_APP_LOCK
            } else {
                OnboardingDestination.NAVIGATE_TO_DASHBOARD
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isOnboardingCompleted = true,
                    currentDestination = dest
                )
            }
        }
    }

    /**
     * Mendaftarkan akun baru atau masuk untuk pencadangan/pemulihan awan.
     */
    fun submitCloudAuth(): Job {
        val state = _uiState.value
        val email = state.backupEmail.trim()
        val password = state.backupPassword
        val userName = state.backupUserName.trim()

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(syncErrorMessage = "Format email tidak valid") }
            return Job().apply { complete() }
        }
        if (password.length < 8) {
            _uiState.update { it.copy(syncErrorMessage = "Kata sandi minimal 8 karakter") }
            return Job().apply { complete() }
        }

        _uiState.update { it.copy(isSyncing = true, syncErrorMessage = null) }
        return viewModelScope.launch {
            try {
                val client = apiClient ?: SelarasApiClient()
                val authResult = if (state.isCloudBackupMode) {
                    client.register(RegisterRequestDto(email, password, userName.ifBlank { null }))
                } else {
                    client.login(LoginRequestDto(email, password))
                }

                if (authResult.isFailure) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncErrorMessage = authResult.exceptionOrNull()?.message ?: "Gagal terhubung ke akun awan"
                        )
                    }
                    return@launch
                }

                val response = authResult.getOrThrow()
                authSessionManager?.saveSession(
                    token = response.token,
                    email = response.user.email,
                    userId = response.user.id,
                    userName = response.user.userName
                )

                // Eksekusi sinkronisasi data awal (Smart Merge LWW)
                if (state.isCloudBackupMode) {
                    syncCoordinator?.backup()
                } else {
                    syncCoordinator?.restore()
                }

                completeOnboardingUseCase()

                val dest = if (isPinSetProvider()) {
                    OnboardingDestination.NAVIGATE_TO_APP_LOCK
                } else {
                    OnboardingDestination.NAVIGATE_TO_DASHBOARD
                }

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        isOnboardingCompleted = true,
                        currentDestination = dest
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncErrorMessage = e.message ?: "Terjadi kesalahan saat otentikasi awan"
                    )
                }
            }
        }
    }

    class Factory(
        private val repository: AppSettingsRepository,
        private val isPinSetProvider: () -> Boolean = { false },
        private val authSessionManager: AuthSessionManager? = null,
        private val syncCoordinator: SyncCoordinator? = null,
        private val apiClient: SelarasApiClient? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(OnboardingViewModel::class.java))
            val init = InitializeAppSettingsUseCase(repository)
            val get = GetAppSettingsUseCase(repository)
            val accept = AcceptConsentUseCase(repository)
            val complete = CompleteOnboardingUseCase(repository)
            @Suppress("UNCHECKED_CAST")
            return OnboardingViewModel(
                init,
                get,
                accept,
                complete,
                isPinSetProvider,
                authSessionManager,
                syncCoordinator,
                apiClient
            ) as T
        }
    }
}
