package com.example.selaras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.selaras.core.ui.theme.SelarasTheme
import com.example.selaras.features.onboarding.OnboardingScreen
import com.example.selaras.features.onboarding.OnboardingViewModel

class MainActivity : ComponentActivity() {

    private lateinit var onboardingViewModel: OnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SelarasApplication).container

        onboardingViewModel = ViewModelProvider(
            this,
            OnboardingViewModel.Factory(
                repository = container.appSettingsRepository,
                isPinSetProvider = { container.pinManager.isPinSet() }
            )
        )[OnboardingViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            SelarasTheme(darkTheme = false, dynamicColor = false) {
                // Mode testing: forceShowOnboarding = true agar Splash selalu lanjut ke 4 Slide Carousel & Consent
                OnboardingScreen(
                    viewModel = onboardingViewModel,
                    forceShowOnboarding = true,
                    onNavigateToDashboard = {
                        // Callback selesai onboarding
                    },
                    onNavigateToAppLock = {
                        // Callback jika pengguna sudah memiliki PIN
                    }
                )
            }
        }
    }
}
