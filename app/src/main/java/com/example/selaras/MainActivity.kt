package com.example.selaras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.selaras.core.navigation.AppNavigation
import com.example.selaras.core.ui.theme.SelarasTheme
import com.example.selaras.features.breathing.BreathingViewModel
import com.example.selaras.features.checkin.CheckInViewModel
import com.example.selaras.features.dashboard.DashboardViewModel

class MainActivity : ComponentActivity() {

    private lateinit var dashboardViewModel: DashboardViewModel
    private lateinit var checkInViewModel: CheckInViewModel
    private lateinit var breathingViewModel: BreathingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SelarasApplication).container

        dashboardViewModel = ViewModelProvider(
            this,
            DashboardViewModel.Factory(
                checkInRepository = container.checkInRepository,
                screeningRepository = container.screeningRepository
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

        enableEdgeToEdge()
        setContent {
            SelarasTheme {
                AppNavigation(
                    container = container,
                    dashboardViewModel = dashboardViewModel,
                    checkInViewModel = checkInViewModel,
                    breathingViewModel = breathingViewModel
                )
            }
        }
    }
}
