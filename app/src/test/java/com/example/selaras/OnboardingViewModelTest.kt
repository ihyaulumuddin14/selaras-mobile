package com.example.selaras

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.selaras.core.database.SelarasDatabase
import com.example.selaras.data.datasource.local.AppSettingsLocalDataSource
import com.example.selaras.data.repository.AppSettingsRepositoryImpl
import com.example.selaras.domain.usecase.onboarding.AcceptConsentUseCase
import com.example.selaras.domain.usecase.onboarding.CompleteOnboardingUseCase
import com.example.selaras.domain.usecase.onboarding.GetAppSettingsUseCase
import com.example.selaras.domain.usecase.onboarding.InitializeAppSettingsUseCase
import com.example.selaras.features.onboarding.OnboardingDestination
import com.example.selaras.features.onboarding.OnboardingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OnboardingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: SelarasDatabase
    private lateinit var repository: AppSettingsRepositoryImpl
    private lateinit var viewModel: OnboardingViewModel
    private var isPinSetMock: Boolean = false

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SelarasDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val localDataSource = AppSettingsLocalDataSource(database.appSettingsDao())
        repository = AppSettingsRepositoryImpl(localDataSource)

        val initUseCase = InitializeAppSettingsUseCase(repository)
        val getUseCase = GetAppSettingsUseCase(repository)
        val acceptUseCase = AcceptConsentUseCase(repository)
        val completeUseCase = CompleteOnboardingUseCase(repository)

        viewModel = OnboardingViewModel(
            initializeAppSettingsUseCase = initUseCase,
            getAppSettingsUseCase = getUseCase,
            acceptConsentUseCase = acceptUseCase,
            completeOnboardingUseCase = completeUseCase,
            isPinSetProvider = { isPinSetMock }
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialDestinationIsSplash() {
        assertEquals(OnboardingDestination.SPLASH, viewModel.uiState.value.currentDestination)
        assertEquals(0, viewModel.uiState.value.carouselIndex)
        assertFalse(viewModel.uiState.value.isConsentChecked)
    }

    @Test
    fun testSplashCheckForNewUserNavigatesToCarousel() = runTest(testDispatcher) {
        viewModel.checkAppLaunchState(splashDelayMs = 0).join()
        advanceUntilIdle()

        assertEquals(OnboardingDestination.ONBOARDING_CAROUSEL, viewModel.uiState.value.currentDestination)
    }

    @Test
    fun testSplashCheckForReturningUserNavigatesToDashboard() = runTest(testDispatcher) {
        // Given returning user who already completed onboarding
        repository.initializeIfFirstLaunch()
        repository.acceptConsent()
        repository.completeOnboarding()
        isPinSetMock = false

        viewModel.checkAppLaunchState(splashDelayMs = 0).join()
        advanceUntilIdle()

        assertEquals(OnboardingDestination.NAVIGATE_TO_DASHBOARD, viewModel.uiState.value.currentDestination)
    }

    @Test
    fun testSplashCheckForReturningUserWithPinNavigatesToAppLock() = runTest(testDispatcher) {
        repository.initializeIfFirstLaunch()
        repository.acceptConsent()
        repository.completeOnboarding()
        isPinSetMock = true

        viewModel.checkAppLaunchState(splashDelayMs = 0).join()
        advanceUntilIdle()

        assertEquals(OnboardingDestination.NAVIGATE_TO_APP_LOCK, viewModel.uiState.value.currentDestination)
    }

    @Test
    fun testCarouselSlideProgressionAndSkip() {
        viewModel.setSlideIndex(0)
        assertEquals(0, viewModel.uiState.value.carouselIndex)

        viewModel.nextSlide()
        assertEquals(1, viewModel.uiState.value.carouselIndex)

        viewModel.nextSlide()
        assertEquals(2, viewModel.uiState.value.carouselIndex)

        viewModel.prevSlide()
        assertEquals(1, viewModel.uiState.value.carouselIndex)

        viewModel.skipToConsent()
        assertEquals(OnboardingDestination.INFORMED_CONSENT, viewModel.uiState.value.currentDestination)

        viewModel.backToCarousel()
        assertEquals(OnboardingDestination.ONBOARDING_CAROUSEL, viewModel.uiState.value.currentDestination)
        assertEquals(viewModel.uiState.value.totalSlides - 1, viewModel.uiState.value.carouselIndex)
    }

    @Test
    fun testConsentSubmissionCompletesOnboarding() = runTest(testDispatcher) {
        viewModel.skipToConsent()
        assertEquals(OnboardingDestination.INFORMED_CONSENT, viewModel.uiState.value.currentDestination)
        assertFalse(viewModel.uiState.value.isConsentChecked)

        // Cannot submit when unchecked
        viewModel.submitConsentAndComplete().join()
        assertEquals(OnboardingDestination.INFORMED_CONSENT, viewModel.uiState.value.currentDestination)

        // Check consent and submit
        viewModel.setConsentChecked(true)
        assertTrue(viewModel.uiState.value.isConsentChecked)

        viewModel.submitConsentAndComplete().join()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isConsentAccepted)
        assertTrue(state.isOnboardingCompleted)
        assertEquals(OnboardingDestination.NAVIGATE_TO_DASHBOARD, state.currentDestination)

        // Verify stored in repository
        val storedSettings = repository.getSettings()
        assertNotNull(storedSettings)
        assertTrue(storedSettings!!.isConsentAccepted)
        assertTrue(storedSettings.onboardingCompleted)
    }
}
