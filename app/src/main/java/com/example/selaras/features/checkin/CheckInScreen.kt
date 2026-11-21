package com.example.selaras.features.checkin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.domain.model.CheckIn
import com.example.selaras.domain.model.MoodTrend
import com.example.selaras.domain.model.WeeklyCheckInAnalysis
import com.example.selaras.features.checkin.components.WeeklyPatternChart
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import com.example.selaras.features.checkin.components.ConfettiEffect
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon

internal object CheckInColors {
    val Primary = Color(0xFF356859)
    val PrimaryContainer = Color(0xFFDCECE3)
    val Background = Color(0xFFF7F7F2)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF243B35)
    val TextSecondary = Color(0xFF52675F)
    val Outline = Color(0xFF7B9187)
    val CardBorder = Color(0xFFE2E8E3)
    val Success = Color(0xFF2E7D32)
    val Warning = Color(0xFFED6C02)
    val Error = Color(0xFFB3261E)
}

@Composable
fun CheckInScreen(
    viewModel: CheckInViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    BackHandler {
        if (state.isEditingToday) {
            viewModel.cancelEditToday()
        } else if (state.currentTab != CheckInTab.TODAY) {
            viewModel.setTab(CheckInTab.TODAY)
        } else {
            onBack()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CheckInColors.Background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            SelarasHeader(
                title = if (state.isEditingToday) "Ubah Check-in Hari Ini" else "Check-in Harian",
                onBack = {
                    if (state.isEditingToday) {
                        viewModel.cancelEditToday()
                    }
                    onBack()
                }
            )

            // Navigation Tabs
            CheckInTabRow(
                selectedTab = state.currentTab,
                onTabSelected = viewModel::setTab
            )

            // Error Banner if present
            if (state.errorMessage != null) {
                Surface(
                    color = CheckInColors.Error.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = CheckInColors.Error,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (state.currentTab) {
                    CheckInTab.TODAY -> {
                        TodayTabContent(
                            state = state,
                            viewModel = viewModel
                        )
                    }
                    CheckInTab.WEEKLY_ANALYSIS -> {
                        WeeklyAnalysisTabContent(
                            analysis = state.weeklyAnalysis,
                            onStartCheckIn = { viewModel.setTab(CheckInTab.TODAY) }
                        )
                    }
                    CheckInTab.HISTORY -> {
                        HistoryTabContent(
                            entries = state.historyEntries,
                            onStartCheckIn = { viewModel.setTab(CheckInTab.TODAY) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckInTabRow(
    selectedTab: CheckInTab,
    onTabSelected: (CheckInTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(CheckInColors.PrimaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        TabButton(
            text = "Hari Ini",
            selected = selectedTab == CheckInTab.TODAY,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(CheckInTab.TODAY) }
        )
        TabButton(
            text = "Pola 7 Hari",
            selected = selectedTab == CheckInTab.WEEKLY_ANALYSIS,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(CheckInTab.WEEKLY_ANALYSIS) }
        )
        TabButton(
            text = "Riwayat",
            selected = selectedTab == CheckInTab.HISTORY,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(CheckInTab.HISTORY) }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(38.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) CheckInColors.Primary else Color.Transparent,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) Color.White else CheckInColors.TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------
// Tab 1: Today Content (Summary Card or 4-Step Wizard)
// ---------------------------------------------------------

@Composable
private fun TodayTabContent(
    state: CheckInUiState,
    viewModel: CheckInViewModel
) {
    if (state.isAlreadyCheckedIn && !state.isEditingToday && state.currentStep != CheckInStep.SUCCESS) {
        // State: Already Checked In Today (Figma 168:2332)
        AlreadyCheckedInView(
            today = requireNotNull(state.todayCheckIn),
            onEdit = viewModel::startEditToday,
            onViewWeekly = { viewModel.setTab(CheckInTab.WEEKLY_ANALYSIS) }
        )
    } else {
        // Wizard Flow: Step 1 (Mood), Step 2 (Energy), Step 3 (Sleep), Step 4 (Note), Step 5 (Success)
        CheckInWizardView(
            state = state,
            viewModel = viewModel
        )
    }
}

@Composable
private fun AlreadyCheckedInView(
    today: CheckIn,
    onEdit: () -> Unit,
    onViewWeekly: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = CircleShape,
                color = CheckInColors.PrimaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("✓", fontSize = 36.sp, color = CheckInColors.Primary)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Kamu Sudah Check-in Hari Ini",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CheckInColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Terima kasih sudah meluangkan waktu untuk mengenali dirimu.",
                fontSize = 14.sp,
                color = CheckInColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Card details
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CheckInColors.Surface),
                border = BorderStroke(1.dp, CheckInColors.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Ringkasan Catatan Hari Ini",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CheckInColors.TextPrimary
                    )
                    Spacer(Modifier.height(14.dp))

                    SummaryRow(label = "Suasana Hati", value = "${today.mood}/5 · ${today.moodLabel}")
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = CheckInColors.CardBorder)
                    SummaryRow(label = "Tingkat Energi", value = "${today.energy}/10")
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = CheckInColors.CardBorder)
                    SummaryRow(label = "Durasi Tidur", value = "${today.sleepHours} jam ${today.sleepRemainingMinutes} menit")

                    if (!today.note.isNullOrBlank()) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = CheckInColors.CardBorder)
                        Text("Catatan Refleksi:", fontSize = 13.sp, color = CheckInColors.TextSecondary, lineHeight = 17.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = today.note,
                            fontSize = 14.sp,
                            color = CheckInColors.TextPrimary,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onViewWeekly,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                Text("Lihat Analisis Pola 7 Hari", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CheckInColors.Outline)
            ) {
                Text("Ubah Catatan Hari Ini", color = CheckInColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = CheckInColors.TextSecondary, fontSize = 14.sp)
        Text(text = value, color = CheckInColors.TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------
// 4-Step Wizard Flow
// ---------------------------------------------------------

@Composable
private fun CheckInWizardView(
    state: CheckInUiState,
    viewModel: CheckInViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        if (state.currentStep != CheckInStep.SUCCESS) {
            WizardProgressBar(currentStep = state.currentStep)
            Spacer(Modifier.height(16.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (state.currentStep) {
                CheckInStep.MOOD -> MoodStep(
                    selectedMood = state.formMood,
                    onSelectMood = viewModel::setMood,
                    onNext = viewModel::nextStep
                )
                CheckInStep.ENERGY -> EnergyStep(
                    selectedEnergy = state.formEnergy,
                    onSelectEnergy = viewModel::setEnergy,
                    onPrev = viewModel::previousStep,
                    onNext = viewModel::nextStep
                )
                CheckInStep.SLEEP -> SleepStep(
                    hours = state.formSleepHours,
                    minutes = state.formSleepMinutes,
                    onDurationChange = viewModel::setSleepDuration,
                    onPrev = viewModel::previousStep,
                    onNext = viewModel::nextStep
                )
                CheckInStep.NOTE -> NoteStep(
                    note = state.formNote,
                    isSaving = state.isSaving,
                    onNoteChange = viewModel::setNote,
                    onPrev = viewModel::previousStep,
                    onSave = viewModel::saveCheckIn
                )
                CheckInStep.SUCCESS -> SuccessStep(
                    onDismiss = viewModel::dismissSuccess
                )
            }
        }
    }
}

@Composable
private fun WizardProgressBar(currentStep: CheckInStep) {
    val stepIndex = when (currentStep) {
        CheckInStep.MOOD -> 1
        CheckInStep.ENERGY -> 2
        CheckInStep.SLEEP -> 3
        CheckInStep.NOTE -> 4
        CheckInStep.SUCCESS -> 4
    }

    val animatedProgress by animateFloatAsState(
        targetValue = stepIndex / 4f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CheckInProgressBarAnimation"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Langkah $stepIndex dari 4",
                fontSize = 13.sp,
                color = CheckInColors.TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = when (currentStep) {
                    CheckInStep.MOOD -> "Suasana Hati"
                    CheckInStep.ENERGY -> "Tingkat Energi"
                    CheckInStep.SLEEP -> "Durasi Tidur"
                    CheckInStep.NOTE -> "Refleksi Pribadi"
                    CheckInStep.SUCCESS -> "Selesai"
                },
                fontSize = 13.sp,
                color = CheckInColors.Primary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = CheckInColors.Primary,
            trackColor = CheckInColors.CardBorder,
        )
    }
}

@Composable
private fun MoodStep(
    selectedMood: Int,
    onSelectMood: (Int) -> Unit,
    onNext: () -> Unit
) {
    val moods = listOf(
        1 to Pair("Sangat Kurang Baik", "😔"),
        2 to Pair("Kurang Baik", "🙁"),
        3 to Pair("Biasa Saja", "😐"),
        4 to Pair("Baik", "🙂"),
        5 to Pair("Sangat Baik", "😊")
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Bagaimana suasana hatimu hari ini?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CheckInColors.TextPrimary
        )
        Text(
            text = "Pilih yang paling mewakili perasaan dominanmu.",
            fontSize = 14.sp,
            color = CheckInColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            moods.forEach { (value, data) ->
                val (label, icon) = data
                val isSelected = selectedMood == value
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) CheckInColors.PrimaryContainer else CheckInColors.Surface,
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) CheckInColors.Primary else CheckInColors.CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable { onSelectMood(value) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(icon, fontSize = 24.sp)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = CheckInColors.TextPrimary
                        )
                        Spacer(Modifier.weight(1f))
                        if (isSelected) {
                            Text("✓", fontSize = 18.sp, color = CheckInColors.Primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
        ) {
            Text("Lanjut", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun EnergyStep(
    selectedEnergy: Int,
    onSelectEnergy: (Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Berapa tingkat energimu hari ini?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CheckInColors.TextPrimary
        )
        Text(
            text = "Skala 1 (Sangat Rendah) hingga 10 (Sangat Tinggi).",
            fontSize = 14.sp,
            color = CheckInColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Current selection badge
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = CheckInColors.PrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Energi: $selectedEnergy / 10",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = CheckInColors.Primary
                )
                Text(
                    text = when (selectedEnergy) {
                        in 1..3 -> "Energi rendah, tubuh membutuhkan istirahat."
                        in 4..6 -> "Energi sedang, cukup untuk aktivitas harian."
                        in 7..8 -> "Energi tinggi dan bersemangat!"
                        else -> "Energi maksimal dan optimal!"
                    },
                    fontSize = 13.sp,
                    color = CheckInColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // 10 Buttons arranged in 2 rows of 5 (ADR-010 & Figma 168:2320)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 1..5) {
                    EnergyButton(
                        number = i,
                        isSelected = selectedEnergy == i,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectEnergy(i) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (i in 6..10) {
                    EnergyButton(
                        number = i,
                        isSelected = selectedEnergy == i,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelectEnergy(i) }
                    )
                }
            }
        }

        // Navigation actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CheckInColors.Outline)
            ) {
                Text("Kembali", color = CheckInColors.Primary)
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                Text("Lanjut", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EnergyButton(
    number: Int,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) CheckInColors.Primary else CheckInColors.Surface,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) CheckInColors.Primary else CheckInColors.CardBorder
        ),
        modifier = modifier
            .height(54.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else CheckInColors.TextPrimary
            )
        }
    }
}

@Composable
private fun SleepStep(
    hours: Int,
    minutes: Int,
    onDurationChange: (Int, Int) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Berapa lama kamu tidur semalam?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CheckInColors.TextPrimary
        )
        Text(
            text = "Durasi istirahat malam memengaruhi kestabilan mood dan energimu.",
            fontSize = 14.sp,
            color = CheckInColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )

        // Duration highlight card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = CheckInColors.PrimaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$hours Jam ${if (minutes > 0) "$minutes Menit" else ""}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = CheckInColors.Primary
                )
                Text(
                    text = when {
                        hours < 6 -> "Kurang dari anjuran sehat (6-8 jam)"
                        hours in 7..9 -> "Durasi tidur ideal & optimal"
                        else -> "Durasi tidur cukup panjang"
                    },
                    fontSize = 13.sp,
                    color = CheckInColors.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Hour Stepper
        Text("Jam Tidur:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = CheckInColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onDurationChange((hours - 1).coerceAtLeast(0), minutes) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.PrimaryContainer),
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("–", fontSize = 22.sp, color = CheckInColors.Primary, fontWeight = FontWeight.Bold)
            }

            Text("$hours Jam", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CheckInColors.TextPrimary)

            Button(
                onClick = { onDurationChange((hours + 1).coerceAtMost(16), minutes) },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.PrimaryContainer),
                modifier = Modifier.size(44.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("+", fontSize = 22.sp, color = CheckInColors.Primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Minute quick buttons
        Text("Menit Tambahan:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = CheckInColors.TextPrimary)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0, 15, 30, 45).forEach { m ->
                val isSelected = minutes == m
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) CheckInColors.Primary else CheckInColors.Surface,
                    border = BorderStroke(1.dp, if (isSelected) CheckInColors.Primary else CheckInColors.CardBorder),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clickable { onDurationChange(hours, m) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "+$m mnt",
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else CheckInColors.TextPrimary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // Navigation
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrev,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CheckInColors.Outline)
            ) {
                Text("Kembali", color = CheckInColors.Primary)
            }
            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                Text("Lanjut", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun NoteStep(
    note: String,
    isSaving: Boolean,
    onNoteChange: (String) -> Unit,
    onPrev: () -> Unit,
    onSave: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Ada hal yang ingin kamu refleksikan?",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CheckInColors.TextPrimary
        )
        Text(
            text = "Catatan ini bersifat opsional dan tersimpan privat di perangkatmu.",
            fontSize = 14.sp,
            color = CheckInColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("Ceritakan peristiwa, pikiran, atau apa pun yang memengaruhi harimu hari ini...") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CheckInColors.Primary,
                unfocusedBorderColor = CheckInColors.CardBorder,
                focusedContainerColor = CheckInColors.Surface,
                unfocusedContainerColor = CheckInColors.Surface
            )
        )
        Text(
            text = "${note.length} / 500 karakter",
            fontSize = 12.sp,
            color = CheckInColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 6.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPrev,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CheckInColors.Outline)
            ) {
                Text("Kembali", color = CheckInColors.Primary)
            }
            Button(
                onClick = onSave,
                enabled = !isSaving,
                modifier = Modifier
                    .weight(2f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Simpan Check-in ✓", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SuccessStep(
    onDismiss: () -> Unit
) {
    val scaleAnim = remember { Animatable(0f) }
    val checkmarkAlpha = remember { Animatable(0f) }
    val rippleScale = remember { Animatable(0.7f) }
    val rippleAlpha = remember { Animatable(0.6f) }

    LaunchedEffect(Unit) {
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            delay(100)
            checkmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            delay(150)
            rippleScale.animateTo(
                targetValue = 1.7f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        launch {
            delay(150)
            rippleAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Confetti celebration animation overlay
        ConfettiEffect(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Expanding soft ripple ring
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = rippleScale.value
                            scaleY = rippleScale.value
                            alpha = rippleAlpha.value
                        }
                        .clip(CircleShape)
                        .background(CheckInColors.Primary.copy(alpha = 0.25f))
                )

                // Bouncy expanding circle
                Surface(
                    shape = CircleShape,
                    color = CheckInColors.PrimaryContainer,
                    modifier = Modifier
                        .size(80.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "✓",
                            fontSize = 42.sp,
                            color = CheckInColors.Primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                alpha = checkmarkAlpha.value
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Catatan Berhasil Disimpan!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = CheckInColors.TextPrimary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Data telah tersimpan di penyimpanan aman lokalmu.",
                fontSize = 14.sp,
                color = CheckInColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                Text("Lihat Analisis 7 Hari", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------
// Tab 2: 7-Day Weekly Pattern Analysis Content
// ---------------------------------------------------------

@Composable
private fun WeeklyAnalysisTabContent(
    analysis: WeeklyCheckInAnalysis?,
    onStartCheckIn: () -> Unit
) {
    val activeAnalysis = analysis ?: remember {
        val today = LocalDate.now()
        WeeklyCheckInAnalysis(
            startDate = today.minusDays(6),
            endDate = today,
            entries = emptyList(),
            totalDaysRecorded = 0,
            averageMood = 0.0,
            averageEnergy = 0.0,
            averageSleepMinutes = 0,
            moodTrend = MoodTrend.INSUFFICIENT_DATA,
            isEligibleForCounselorSummary = false,
            insights = emptyList()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Weekly Trend Pattern Chart (grafik_pola.png - 7 Day Range)
        item {
            WeeklyPatternChart(analysis = activeAnalysis)
        }

        if (!activeAnalysis.hasData) {
            // Empty State Notice below Chart
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CheckInColors.Surface),
                    border = BorderStroke(1.dp, CheckInColors.CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊", fontSize = 36.sp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Belum Ada Catatan 7 Hari Ini",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = CheckInColors.TextPrimary
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Grafik di atas akan otomatis menampilkan kurva tren suasana hati, tingkat energi, dan durasi tidur seiring kamu mencatat check-in harianmu.",
                            fontSize = 13.sp,
                            color = CheckInColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onStartCheckIn,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
                        ) {
                            Text("Mulai Catat Hari Ini", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = "Ringkasan 7 Hari Terakhir",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CheckInColors.TextPrimary
                )
                Text(
                    text = "${activeAnalysis.startDate} s.d. ${activeAnalysis.endDate} (${activeAnalysis.totalDaysRecorded} hari terdata)",
                    fontSize = 13.sp,
                    color = CheckInColors.TextSecondary
                )
            }

            // Stat Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Rata-rata Mood",
                        value = "${activeAnalysis.averageMood} / 5.0",
                        subtitle = "Skala emosi",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Rata-rata Energi",
                        value = "${activeAnalysis.averageEnergy} / 10",
                        subtitle = "Level energi",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Rata-rata Tidur",
                        value = "${activeAnalysis.averageSleepHours} jam",
                        subtitle = "Istirahat malam",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Counselor Summary Eligibility Card (Rule K02)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeAnalysis.isEligibleForCounselorSummary)
                            CheckInColors.PrimaryContainer
                        else
                            CheckInColors.Surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (activeAnalysis.isEligibleForCounselorSummary) CheckInColors.Primary else CheckInColors.CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeAnalysis.isEligibleForCounselorSummary) "✓" else "ℹ",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeAnalysis.isEligibleForCounselorSummary) CheckInColors.Primary else CheckInColors.Warning
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = if (activeAnalysis.isEligibleForCounselorSummary)
                                    "Syarat Ringkasan Konselor (F4) Terpenuhi"
                                else
                                    "Menuju Kelayakan Ringkasan Konselor (F4)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = CheckInColors.TextPrimary
                            )
                            Text(
                                text = if (activeAnalysis.isEligibleForCounselorSummary)
                                    "Tersedia ${activeAnalysis.totalDaysRecorded} hari data (minimal 4 hari). Berkas PDF siap dibuat."
                                else
                                    "Butuh ${4 - activeAnalysis.totalDaysRecorded} hari pencatatan lagi untuk mencapai syarat minimal 4 hari.",
                                fontSize = 12.sp,
                                color = CheckInColors.TextSecondary
                            )
                        }
                    }
                }
            }

            // Pattern Insights Card (Figma 168:2326)
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = CheckInColors.Surface),
                    border = BorderStroke(1.dp, CheckInColors.CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "💡 Pola & Rekomendasi Terdeteksi",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CheckInColors.TextPrimary
                        )
                        Spacer(Modifier.height(10.dp))
                        activeAnalysis.insights.forEach { insight ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", fontSize = 14.sp, color = CheckInColors.Primary, fontWeight = FontWeight.Bold)
                                Text(
                                    text = insight,
                                    fontSize = 13.sp,
                                    color = CheckInColors.TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CheckInColors.Surface),
        border = BorderStroke(1.dp, CheckInColors.CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = CheckInColors.TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CheckInColors.Primary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = CheckInColors.TextSecondary)
        }
    }
}

// ---------------------------------------------------------
// Tab 3: History Entries Content
// ---------------------------------------------------------

@Composable
private fun HistoryTabContent(
    entries: List<CheckIn>,
    onStartCheckIn: () -> Unit
) {
    if (entries.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📝", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Belum Ada Riwayat Check-in",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = CheckInColors.TextPrimary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Riwayat catatan harianmu akan ditampilkan secara kronologis di sini.",
                fontSize = 14.sp,
                color = CheckInColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStartCheckIn,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CheckInColors.Primary)
            ) {
                Text("Mulai Catat Hari Ini")
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Riwayat Check-in (${entries.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = CheckInColors.TextPrimary
                )
            }
            items(entries, key = { it.id }) { item ->
                HistoryItemCard(item)
            }
        }
    }
}

@Composable
private fun HistoryItemCard(item: CheckIn) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy")
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CheckInColors.Surface),
        border = BorderStroke(1.dp, CheckInColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.checkInDate.format(formatter),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = CheckInColors.TextPrimary
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CheckInColors.PrimaryContainer
                ) {
                    Text(
                        text = "Mood: ${item.mood}/5",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CheckInColors.Primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("⚡ Energi: ${item.energy}/10", fontSize = 13.sp, color = CheckInColors.TextSecondary)
                Text("🌙 Tidur: ${item.sleepHours}j ${item.sleepRemainingMinutes}m", fontSize = 13.sp, color = CheckInColors.TextSecondary)
            }
            if (!item.note.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = item.note,
                    fontSize = 13.sp,
                    color = CheckInColors.TextPrimary,
                    maxLines = 2
                )
            }
        }
    }
}
