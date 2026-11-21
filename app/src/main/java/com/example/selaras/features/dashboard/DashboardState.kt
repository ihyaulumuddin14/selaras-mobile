package com.example.selaras.features.dashboard

import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.WeeklyCheckInAnalysis

/**
 * UI State for Selaras Dashboard / Beranda Utama (PRD Section 6.3 & Figma 12:717, 86:1195, 86:1203).
 */
data class DashboardUiState(
    val greeting: String = "Selamat Datang",
    val formattedDate: String = "",
    val userName: String? = null,
    val isNameSet: Boolean = false,
    val todayCheckIn: CheckIn? = null,
    val isTodayCheckedIn: Boolean = false,
    val weeklyAnalysis: WeeklyCheckInAnalysis? = null,
    val latestScreening: ScreeningResult? = null,
    val daysRecordedThisWeek: Int = 0,
    val isEligibleForCounselorSummary: Boolean = false,
    val daysNeededForCounselorSummary: Int = 4,
    val showDailyCheckInDialog: Boolean = false,
    val shouldPulseCheckInCard: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
