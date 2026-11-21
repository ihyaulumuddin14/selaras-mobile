package com.example.selaras.core.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.selaras.core.di.AppContainer
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
import com.example.selaras.features.settings.SettingsScreen

/**
 * Sub-destinations that push on top of the bottom navigation bar.
 */
enum class SubDestination {
    NONE,
    BREATHING,
    COUNSELOR_SUMMARY,
    SETTINGS
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
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var subDestination by remember { mutableStateOf(SubDestination.NONE) }

    // System BackHandler
    BackHandler(enabled = subDestination != SubDestination.NONE || currentTab != NavTab.HOME) {
        if (subDestination != SubDestination.NONE) {
            subDestination = SubDestination.NONE
        } else if (currentTab != NavTab.HOME) {
            currentTab = NavTab.HOME
        }
    }

    val isShowingBottomNav = subDestination == SubDestination.NONE

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isShowingBottomNav) {
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
        ) {
            when (subDestination) {
                SubDestination.BREATHING -> {
                    BreathingScreen(
                        viewModel = breathingViewModel,
                        onExit = { subDestination = SubDestination.NONE }
                    )
                }

                SubDestination.COUNSELOR_SUMMARY -> {
                    CounselorSummaryScreen(
                        onNavigateBack = { subDestination = SubDestination.NONE }
                    )
                }

                SubDestination.SETTINGS -> {
                    SettingsScreen(
                        onNavigateBack = { subDestination = SubDestination.NONE },
                        onResetPin = { /* Handled via Settings or AppLock */ },
                        onDeleteAllData = { /* Handled via Settings */ }
                    )
                }

                SubDestination.NONE -> {
                    when (currentTab) {
                        NavTab.HOME -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToCheckIn = { currentTab = NavTab.CHECK_IN },
                                onNavigateToBreathing = { subDestination = SubDestination.BREATHING },
                                onNavigateToScreening = { currentTab = NavTab.SCREENING },
                                onNavigateToCounselorSummary = { subDestination = SubDestination.COUNSELOR_SUMMARY },
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
                                onStartScreening = { /* Ready for PHQ-9 Question flow */ }
                            )
                        }

                        NavTab.HISTORY -> {
                            HistoryScreen(
                                onNavigateToCheckInHistory = { currentTab = NavTab.CHECK_IN },
                                onNavigateToCounselorPdf = { subDestination = SubDestination.COUNSELOR_SUMMARY }
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
}
