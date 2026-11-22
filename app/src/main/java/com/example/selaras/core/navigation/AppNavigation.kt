package com.example.selaras.core.navigation

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.selaras.core.di.AppContainer
import kotlinx.coroutines.launch
import com.example.selaras.features.breathing.BreathingScreen
import com.example.selaras.features.breathing.BreathingViewModel
import com.example.selaras.features.checkin.CheckInScreen
import com.example.selaras.features.checkin.CheckInViewModel
import com.example.selaras.features.counselor.CounselorSummaryScreen
import com.example.selaras.features.dashboard.DashboardScreen
import com.example.selaras.features.dashboard.DashboardViewModel
import com.example.selaras.features.help.HelpScreen
import com.example.selaras.features.history.HistoryScreen
import com.example.selaras.features.screening.ScreeningScreen
import com.example.selaras.features.security.AppLockMasterScreen
import com.example.selaras.features.security.AppLockViewModel
import com.example.selaras.features.settings.SettingsScreen
import com.example.selaras.features.screening.ScreeningViewModel

/**
 * Sub-destinations that push on top of the bottom navigation bar.
 */
enum class SubDestination {
    NONE,
    BREATHING,
    COUNSELOR_SUMMARY,
    SETTINGS,
    PIN_SETUP
}

/**
 * Main application navigation coordinator (ADR-011, ADR-013).
 *
 * Manages the 5 BottomNav tabs (Beranda, Check-in, Skrining, Riwayat, Bantuan)
 * and deep flows into feature detail screens with system BackHandler integration.
 */
@Composable
fun AppNavigation(
    container: AppContainer,
    dashboardViewModel: DashboardViewModel,
    checkInViewModel: CheckInViewModel,
    breathingViewModel: BreathingViewModel,
    screeningViewModel: ScreeningViewModel,
    appLockViewModel: AppLockViewModel? = null,
    onResetToOnboarding: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var subDestination by remember { mutableStateOf(SubDestination.NONE) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appSettings by container.appSettingsRepository.getSettingsFlow().collectAsState(initial = null)

    // System BackHandler: handles sub-destinations, tabs, and double-press to exit
    BackHandler(enabled = true) {
        if (subDestination != SubDestination.NONE) {
            if (subDestination == SubDestination.COUNSELOR_SUMMARY) {
                container.screeningHandoffStore.clear()
            }
            subDestination = SubDestination.NONE
        } else if (currentTab != NavTab.HOME) {
            currentTab = NavTab.HOME
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Tekan sekali lagi untuk keluar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AnimatedContent(
        targetState = subDestination,
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            if (targetState != SubDestination.NONE) {
                (slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                )
            } else {
                (slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300))).togetherWith(
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(200))
                )
            }
        },
        label = "SubDestinationTransition",
        modifier = modifier.fillMaxSize()
    ) { destination ->
        when (destination) {
            SubDestination.NONE -> {
                val screeningUiState by screeningViewModel.uiState.collectAsState()
                val isScreeningSecondStep = currentTab == NavTab.SCREENING &&
                    screeningUiState.step != com.example.selaras.features.screening.ScreeningStep.SELECTION
                val showBottomBar = !isScreeningSecondStep

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (showBottomBar) {
                            BottomNavBar(
                                selectedTab = currentTab,
                                onTabSelected = { tab ->
                                    currentTab = tab
                                    subDestination = SubDestination.NONE
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            contentAlignment = Alignment.TopStart,
                            transitionSpec = {
                                val direction = if (targetState.ordinal > initialState.ordinal) {
                                    AnimatedContentTransitionScope.SlideDirection.Left
                                } else {
                                    AnimatedContentTransitionScope.SlideDirection.Right
                                }
                                (slideIntoContainer(
                                    towards = direction,
                                    animationSpec = tween(280, easing = FastOutSlowInEasing)
                                ) + fadeIn(animationSpec = tween(280))).togetherWith(
                                    slideOutOfContainer(
                                        towards = direction,
                                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                                    ) + fadeOut(animationSpec = tween(200))
                                )
                            },
                            label = "NavTabTransition",
                            modifier = Modifier.fillMaxSize()
                        ) { tab ->
                            when (tab) {
                                NavTab.HOME -> {
                                    DashboardScreen(
                                        viewModel = dashboardViewModel,
                                        isPinSet = container.securityRepository.isPinSet(),
                                        onNavigateToCheckIn = {
                                            checkInViewModel.navigateToNewCheckIn()
                                            currentTab = NavTab.CHECK_IN
                                        },
                                        onNavigateToEditCheckIn = {
                                            checkInViewModel.startEditToday()
                                            currentTab = NavTab.CHECK_IN
                                        },
                                        onNavigateToWeeklyAnalysis = {
                                            checkInViewModel.navigateToWeeklyAnalysis()
                                            currentTab = NavTab.CHECK_IN
                                        },
                                        onNavigateToBreathing = { subDestination = SubDestination.BREATHING },
                                        onNavigateToScreening = { currentTab = NavTab.SCREENING },
                                        onNavigateToCounselorSummary = {
                                            container.screeningHandoffStore.clear()
                                            subDestination = SubDestination.COUNSELOR_SUMMARY
                                        },
                                        onNavigateToSettings = { subDestination = SubDestination.SETTINGS },
                                        onNavigateToHelp = { currentTab = NavTab.HELP }
                                    )
                                }

                                NavTab.CHECK_IN -> {
                                    CheckInScreen(
                                        viewModel = checkInViewModel,
                                        onBack = { currentTab = NavTab.HOME }
                                    )
                                }

                                NavTab.SCREENING -> {
                                    ScreeningScreen(
                                        viewModel = screeningViewModel,
                                        onOpenHelp = {
                                            currentTab = NavTab.HELP
                                        },
                                        onOpenHistory = {
                                            currentTab = NavTab.HISTORY
                                        },
                                        onOpenCounselorSummary = { resultId ->
                                            container.screeningHandoffStore.selectResult(resultId)
                                            subDestination = SubDestination.COUNSELOR_SUMMARY
                                        }
                                    )
                                }

                                NavTab.HISTORY -> {
                                    HistoryScreen(
                                        onNavigateToCheckInHistory = { currentTab = NavTab.CHECK_IN },
                                        onNavigateToCounselorPdf = {
                                            container.screeningHandoffStore.clear()
                                            subDestination = SubDestination.COUNSELOR_SUMMARY
                                        }
                                    )
                                }

                                NavTab.HELP -> {
                                    HelpScreen()
                                }
                            }
                        }
                    }
                }
            }

            SubDestination.BREATHING -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        BreathingScreen(
                            viewModel = breathingViewModel,
                            onExit = { subDestination = SubDestination.NONE }
                        )
                    }
                }
            }

            SubDestination.COUNSELOR_SUMMARY -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        CounselorSummaryScreen(
                            onNavigateToHelp = {
                                subDestination = SubDestination.NONE
                                currentTab = NavTab.HELP
                            },
                            onNavigateToCheckIn = {
                                subDestination = SubDestination.NONE
                                currentTab = NavTab.CHECK_IN
                            },
                            onNavigateToSettings = {
                                subDestination = SubDestination.SETTINGS
                            },
                            onNavigateBack = {
                                container.screeningHandoffStore.clear()
                                subDestination = SubDestination.NONE
                            }
                        )
                    }
                }
            }

            SubDestination.SETTINGS -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        SettingsScreen(
                            isPinSet = container.securityRepository.isPinSet(),
                            userName = appSettings?.userName,
                            onUpdateUserName = { newName ->
                                coroutineScope.launch {
                                    container.appSettingsRepository.updateUserName(newName)
                                }
                            },
                            onNavigateBack = { subDestination = SubDestination.NONE },
                            onResetPin = {
                                if (appLockViewModel != null) {
                                    if (container.securityRepository.isPinSet()) {
                                        appLockViewModel.switchToChangePinMode()
                                    } else {
                                        appLockViewModel.switchToSetupMode()
                                    }
                                    subDestination = SubDestination.PIN_SETUP
                                }
                            },
                            onDeleteAllData = onResetToOnboarding
                        )
                    }
                }
            }

            SubDestination.PIN_SETUP -> {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .consumeWindowInsets(innerPadding)
                    ) {
                        if (appLockViewModel != null) {
                            AppLockMasterScreen(
                                viewModel = appLockViewModel,
                                onUnlockSuccess = { subDestination = SubDestination.SETTINGS },
                                onSetupComplete = { subDestination = SubDestination.SETTINGS },
                                onBackToPrevious = { subDestination = SubDestination.SETTINGS }
                            )
                        } else {
                            subDestination = SubDestination.SETTINGS
                        }
                    }
                }
            }
        }
    }
}
