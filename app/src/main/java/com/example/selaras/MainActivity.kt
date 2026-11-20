package com.example.selaras

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.selaras.core.ui.theme.SelarasTheme
import com.example.selaras.features.breathing.BreathingScreen
import com.example.selaras.features.breathing.BreathingViewModel

class MainActivity : ComponentActivity() {
    private lateinit var breathingViewModel: BreathingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as SelarasApplication).container.breathingRepository
        breathingViewModel = ViewModelProvider(
            this,
            BreathingViewModel.Factory(repository)
        )[BreathingViewModel::class.java]
        enableEdgeToEdge()
        setContent {
            SelarasTheme(darkTheme = false, dynamicColor = false) {
                // Temporary preview entry: replace with AppNavigation when team routes land.
                BreathingScreen(viewModel = breathingViewModel, onExit = ::finish)
            }
        }
    }

    override fun onStop() {
        if (::breathingViewModel.isInitialized) breathingViewModel.onAppBackgrounded()
        super.onStop()
    }
}
