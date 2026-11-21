package com.example.selaras.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasIcons
import com.example.selaras.features.dashboard.components.CrisisBanner
import com.example.selaras.features.dashboard.components.DashboardHeader
import com.example.selaras.features.dashboard.components.QuickActionCard
import com.example.selaras.features.dashboard.components.TodayCheckInCard

/**
 * Selaras Dashboard / Beranda Utama Screen (PRD Section 6.3 & Figma 12:717, 86:1195, 86:1203).
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCheckIn: () -> Unit,
    onNavigateToBreathing: () -> Unit,
    onNavigateToScreening: () -> Unit,
    onNavigateToCounselorSummary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val pineGreen = Color(0xFF356859)
    val sageAccent = Color(0xFFE0EDE4)
    val lavenderAccent = Color(0xFFEEE5F5)

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
            .verticalScroll(scrollState)
    ) {
        // 1. Contextual Header
        DashboardHeader(
            greeting = state.greeting,
            formattedDate = state.formattedDate,
            onNavigateToSettings = onNavigateToSettings
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 2. Today's Check-in Card (Unrecorded vs Recorded State)
            TodayCheckInCard(
                todayCheckIn = state.todayCheckIn,
                onStartCheckIn = onNavigateToCheckIn,
                onViewWeeklyAnalysis = onNavigateToCheckIn
            )

            // 3. Quick Action Section Header
            Column {
                Text(
                    text = "Aktivitas Pendamping",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Feature 2: Breathing Exercise
                    QuickActionCard(
                        title = "Latihan Relaksasi Pernapasan",
                        subtitle = "Panduan visual berirama untuk menurunkan ketegangan fisik dan pikiran.",
                        icon = SelarasIcons.Air,
                        containerColor = sageAccent,
                        accentColor = pineGreen,
                        onClick = onNavigateToBreathing
                    )

                    // Feature 1: PHQ-9 Screening
                    val screeningBadge = if (state.latestScreening != null) {
                        "Skor: ${state.latestScreening!!.totalScore}"
                    } else {
                        "1x/Minggu"
                    }
                    QuickActionCard(
                        title = "Skrining PHQ-9",
                        subtitle = "Penapisan baku 9 butir untuk memantau kesehatan emosional secara objektif.",
                        icon = SelarasIcons.FactCheck,
                        containerColor = Color.White,
                        accentColor = pineGreen,
                        badgeText = screeningBadge,
                        onClick = onNavigateToScreening
                    )

                    // Feature 4: Counselor Summary
                    val summaryBadge = if (state.isEligibleForCounselorSummary) {
                        "Siap Diekspor"
                    } else {
                        "${state.daysRecordedThisWeek}/4 Hari"
                    }
                    val summarySubtitle = if (state.isEligibleForCounselorSummary) {
                        "Data observasi minimal 4 hari telah terpenuhi. Siap dikompilasi menjadi berkas PDF."
                    } else {
                        "Butuh ${state.daysNeededForCounselorSummary} hari check-in lagi untuk membuka kompilasi PDF."
                    }

                    QuickActionCard(
                        title = "Bawa ke Konselor",
                        subtitle = summarySubtitle,
                        icon = SelarasIcons.Description,
                        containerColor = lavenderAccent,
                        accentColor = Color(0xFF6B4C8A),
                        badgeText = summaryBadge,
                        onClick = onNavigateToCounselorSummary
                    )
                }
            }

            // 4. Priority Help Path: Crisis Hotline Banner (FR-DSH-03)
            CrisisBanner(
                onNavigateToHelp = onNavigateToHelp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
