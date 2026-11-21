package com.example.selaras.features.breathing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState

/** Temporary F2 entry point while the team's AppNavigation.kt is still a skeleton. */
@Composable
fun BreathingScreen(
    viewModel: BreathingViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    BackHandler {
        if (state.page == BreathingPage.CATALOG) onExit() else viewModel.goBack()
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = BreathingColors.Background
    ) {
        when (state.page) {
            BreathingPage.CATALOG -> BreathingCatalogScreen(
                state = state,
                onBack = onExit,
                onSelect = viewModel::selectPattern,
                onCreate = viewModel::openCreatePattern,
                onEdit = viewModel::openEditPattern,
                onStart = viewModel::openSession
            )
            BreathingPage.FORM -> BreathingPatternFormScreen(
                state = state,
                onBack = viewModel::goBack,
                onNameChange = viewModel::setName,
                onInhaleChange = viewModel::setInhale,
                onExhaleChange = viewModel::setExhale,
                onAdjustInhale = viewModel::adjustInhale,
                onAdjustExhale = viewModel::adjustExhale,
                onSave = viewModel::saveCustomPattern,
                onDelete = viewModel::deleteCustomPattern
            )
            BreathingPage.SESSION -> BreathingSessionScreen(
                state = state,
                onBack = viewModel::stopSession,
                onStartOrResume = viewModel::startOrResumeSession,
                onPause = viewModel::pauseSession,
                onStop = viewModel::stopSession
            )
        }
    }
}

