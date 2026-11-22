package com.example.selaras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.selaras.core.di.AppContainer
import com.example.selaras.core.navigation.AppNavigation
import com.example.selaras.core.ui.theme.SelarasTheme
import com.example.selaras.features.breathing.BreathingViewModel
import com.example.selaras.features.checkin.CheckInTab
import com.example.selaras.features.checkin.CheckInViewModel
import com.example.selaras.features.dashboard.DashboardViewModel
import com.example.selaras.features.onboarding.OnboardingScreen
import com.example.selaras.features.onboarding.OnboardingViewModel
import com.example.selaras.features.security.AppLockMasterScreen
import com.example.selaras.features.security.AppLockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.ViewModel
import com.example.selaras.features.screening.ScreeningViewModel

/**
 * Root destinations for application lifecycle and security gates.
 */
enum class AppRootDestination {
    ONBOARDING,
    APP_LOCK,
    MAIN_APP
}

class MainActivity : ComponentActivity() {

    private lateinit var container: AppContainer
    private lateinit var onboardingViewModel: OnboardingViewModel
    private lateinit var appLockViewModel: AppLockViewModel
    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var checkInViewModel: CheckInViewModel
    private lateinit var breathingViewModel: BreathingViewModel
    private lateinit var screeningViewModel: ScreeningViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as SelarasApplication).container

        onboardingViewModel = ViewModelProvider(
            this,
            OnboardingViewModel.Factory(
                repository = container.appSettingsRepository,
                isPinSetProvider = { container.securityRepository.isPinSet() }
            )
        )[OnboardingViewModel::class.java]

        appLockViewModel = ViewModelProvider(
            this,
            AppLockViewModel.Factory(
                securityRepository = container.securityRepository
            )
        )[AppLockViewModel::class.java]

        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(
                checkInRepository = container.checkInRepository,
                screeningRepository = container.screeningRepository,
                promptManager = container.dailyCheckInPromptManager,
                appSettingsRepository = container.appSettingsRepository
            )
        )[DashboardViewModel::class.java]

        checkInViewModel = ViewModelProvider(
            this,
            CheckInViewModel.Factory(container.checkInRepository)
        )[CheckInViewModel::class.java]

        breathingViewModel = ViewModelProvider(
            this,
            BreathingViewModel.Factory(container.breathingRepository)
        )[BreathingViewModel::class.java]

        screeningViewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ScreeningViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ScreeningViewModel(
                            submitScreeningUseCase = container.submitScreeningUseCase,
                            screeningRepository = container.screeningRepository,
                            getScreeningQuotaUseCase = container.getScreeningQuotaUseCase
                        ) as T
                    }
                    throw IllegalArgumentException(
                        "Unknown ViewModel class: ${modelClass.name}"
                    )
                }
            }
        )[ScreeningViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            SelarasTheme {
                MainApp(
                    container = container,
                    onboardingViewModel = onboardingViewModel,
                    appLockViewModel = appLockViewModel,
                    dashboardViewModel = dashboardViewModel,
                    checkInViewModel = checkInViewModel,
                    breathingViewModel = breathingViewModel,
                    screeningViewModel = screeningViewModel
                )
            }
        }
    }
}

@Composable
fun MainApp(
    container: AppContainer,
    onboardingViewModel: OnboardingViewModel,
    appLockViewModel: AppLockViewModel,
    dashboardViewModel: DashboardViewModel,
    checkInViewModel: CheckInViewModel,
    breathingViewModel: BreathingViewModel,
    screeningViewModel: ScreeningViewModel,
    modifier: Modifier = Modifier,
) {
    var rootDestination by remember { mutableStateOf(AppRootDestination.ONBOARDING) }
    val coroutineScope = rememberCoroutineScope()

    AnimatedContent(
        targetState = rootDestination,
        transitionSpec = {
            (fadeIn(animationSpec = tween(350, easing = FastOutSlowInEasing)) +
                slideInHorizontally(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(280, easing = FastOutSlowInEasing)) +
                        slideOutHorizontally(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it / 4 }
                )
        },
        label = "RootDestinationTransition",
        modifier = modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            AppRootDestination.ONBOARDING -> {
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    onNavigateToDashboard = {
                        rootDestination = AppRootDestination.MAIN_APP
                    },
                    onNavigateToAppLock = {
                        appLockViewModel.switchToUnlockMode()
                        rootDestination = AppRootDestination.APP_LOCK
                    }
                )
            }

            AppRootDestination.APP_LOCK -> {
                AppLockMasterScreen(
                    viewModel = appLockViewModel,
                    onUnlockSuccess = {
                        rootDestination = AppRootDestination.MAIN_APP
                    },
                    onSetupComplete = {
                        rootDestination = AppRootDestination.MAIN_APP
                    }
                )
            }

            AppRootDestination.MAIN_APP -> {
                AppNavigation(
                    container = container,
                    dashboardViewModel = dashboardViewModel,
                    checkInViewModel = checkInViewModel,
                    breathingViewModel = breathingViewModel,
                    screeningViewModel = screeningViewModel,
                    appLockViewModel = appLockViewModel,
                    onResetToOnboarding = {
                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                container.database.clearAllTables()
                                container.securityRepository.clearSecurity()
                                container.dailyCheckInPromptManager.clear()
                            }
                            dashboardViewModel.refresh()
                            checkInViewModel.setTab(CheckInTab.TODAY)
                            onboardingViewModel.resetToOnboarding()
                            rootDestination = AppRootDestination.ONBOARDING
                        }
                    }
                )
            }
        }
    }
}
