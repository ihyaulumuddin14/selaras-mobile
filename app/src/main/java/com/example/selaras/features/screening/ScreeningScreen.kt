package com.example.selaras.features.screening

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ScreeningBackground = Color(0xFFF7F7F2)
private val ScreeningPrimary = Color(0xFF356859)
private val ScreeningPrimaryContainer = Color(0xFFDCECE3)
private val ScreeningText = Color(0xFF243B35)
private val ScreeningMuted = Color(0xFF52675F)
private val ScreeningOutline = Color(0xFF7B9187)
private val ScreeningDisabled = Color(0xFFE2E8E3)
private val ScreeningDisabledText = Color(0xFF596B62)
private val ScreeningWarmSupport = Color(0xFFFFF0E5)
private val ScreeningError = Color(0xFFB3261E)

private val ScreeningAnswerLabels = listOf(
    "Tidak pernah",
    "Beberapa hari",
    "Lebih dari separuh waktu",
    "Hampir setiap hari"
)

private val ScreeningQuestions = listOf(
    "Kurang berminat atau bergairah dalam melakukan kegiatan sehari-hari.",
    "Merasa sedih, tertekan, atau putus asa.",
    "Sulit tidur atau mudah terbangun, atau tidur berlebihan.",
    "Merasa lelah atau tidak bertenaga.",
    "Kurang nafsu makan atau makan berlebihan.",
    "Merasa buruk tentang diri sendiri, atau merasa gagal, atau mengecewakan diri atau keluarga.",
    "Sulit berkonsentrasi pada hal-hal seperti membaca atau menonton televisi.",
    "Bergerak atau berbicara sangat lambat, atau sebaliknya merasa gelisah sehingga bergerak lebih banyak dari biasanya.",
    "Memiliki pikiran bahwa lebih baik mati atau menyakiti diri sendiri dengan cara apa pun."
)

@Composable
fun ScreeningScreen(
    viewModel: ScreeningViewModel,
    onOpenHelp: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    onOpenCounselorSummary: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val quotaState by viewModel.quotaUiState.collectAsState()
    var selectedType by remember { mutableStateOf<ScreeningType?>(null) }

    val resetScreening = {
        selectedType = null
        viewModel.resetScreening()
    }

    when (uiState.step) {
        ScreeningStep.SELECTION -> {
            ScreeningSelectionContent(
                selectedType = selectedType,
                quotaState = quotaState,
                onSelectType = { selectedType = it },
                onStart = {
                    selectedType?.let {
                        viewModel.startScreening(
                            isRoutine = it == ScreeningType.ROUTINE
                        )
                    }
                },
                onRefreshQuota = viewModel::refreshQuota,
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }

        ScreeningStep.INTRODUCTION -> {
            ScreeningIntroductionContent(
                screeningType = uiState.selectedType,
                onBack = resetScreening,
                onBegin = viewModel::beginQuestions,
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }

        ScreeningStep.QUESTIONS -> {
            ScreeningQuestionContent(
                currentQuestionNumber = uiState.currentQuestionNumber,
                answers = uiState.answers,
                currentAnswer = uiState.currentAnswer,
                canGoToPreviousQuestion = uiState.canGoToPreviousQuestion,
                canGoToNextQuestion = uiState.canGoToNextQuestion,
                canSubmit = uiState.canSubmit,
                onBack = {
                    if (uiState.canGoToPreviousQuestion) {
                        viewModel.previousQuestion()
                    } else {
                        resetScreening()
                    }
                },
                onSelectAnswer = {
                    viewModel.selectAnswer(
                        questionNumber = uiState.currentQuestionNumber,
                        answerValue = it
                    )
                },
                onPrevious = viewModel::previousQuestion,
                onNext = viewModel::nextQuestion,
                onSubmit = viewModel::submitScreening,
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }

        ScreeningStep.PROCESSING -> {
            ScreeningProcessingContent(
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }

        ScreeningStep.CRISIS -> {
            ScreeningCrisisContent(
                onContinue = viewModel::continueToResult,
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }

        ScreeningStep.RESULT -> {
            uiState.result?.let { result ->
                ScreeningResultContent(
                    result = result,
                    onReset = resetScreening,
                    onOpenHelp = onOpenHelp,
                    onOpenHistory = onOpenHistory,
                    onOpenCounselorSummary = { onOpenCounselorSummary(result.id) },
                    modifier = modifier
                )
            }
        }

        ScreeningStep.ERROR -> {
            ScreeningErrorContent(
                onRetry = viewModel::submitScreening,
                onReturn = viewModel::returnToQuestions,
                errorMessage = uiState.errorMessage,
                onOpenHelp = onOpenHelp,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun ScreeningHelpAction(onOpenHelp: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(ScreeningPrimaryContainer)
            .clickable(onClick = onOpenHelp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "?",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScreeningPrimary
        )
    }
}

@Composable
private fun ScreeningSelectionContent(
    selectedType: ScreeningType?,
    quotaState: ScreeningQuotaUiState,
    onSelectType: (ScreeningType) -> Unit,
    onStart: () -> Unit,
    onRefreshQuota: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    val quota = quotaState.quota

    val manualAvailable =
        !quotaState.isLoading &&
                quotaState.errorMessage == null &&
                quota?.manualAvailable == true

    val routineAvailable =
        !quotaState.isLoading &&
                quotaState.errorMessage == null &&
                quota?.routineAvailable == true

    val selectedAvailable = selectedType?.let { type ->
        !quotaState.isLoading &&
                quotaState.errorMessage == null &&
                quota?.isAvailable(type) == true
    } == true

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id"))
    }

    val manualQuotaText = when {
        quotaState.isLoading -> "Memuat kuota..."
        quotaState.errorMessage != null || quota == null -> "Kuota belum dapat diverifikasi"
        manualAvailable -> "Sisa kuota: ${quota.manualRemaining} kali periode ini"
        else -> "Kuota periode ini sudah digunakan"
    }

    val routineQuotaText = when {
        quotaState.isLoading -> "Memuat jadwal..."
        quotaState.errorMessage != null || quota == null -> "Jadwal belum dapat diverifikasi"
        quota.routineUsed -> {
            val nextCycleStart = quota.weekEndDate.plusDays(1)
            val nextFrom = nextCycleStart.plusDays(5).format(dateFormatter)
            val nextUntil = nextCycleStart.plusDays(6).format(dateFormatter)
            "Selesai untuk periode ini. Dibuka kembali: $nextFrom sampai $nextUntil"
        }
        !quota.isRoutineWindowOpen -> {
            val fromStr = quota.routineAvailableFrom.format(dateFormatter)
            val untilStr = quota.routineAvailableUntil.format(dateFormatter)
            "Dibuka: $fromStr sampai $untilStr"
        }
        routineAvailable -> {
            val untilStr = quota.routineAvailableUntil.format(dateFormatter)
            "Tersedia sekarang sampai $untilStr (1 kali)"
        }
        else -> "Jadwal belum dibuka"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Skrining",
            onBack = null,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Pilih sesi\nskriningmu.",
                fontSize = 24.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Pilih sesi manual atau rutin sesuai kebutuhanmu. Keduanya memiliki periode ketersediaan masing-masing.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(18.dp))

            when {
                quotaState.isLoading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = ScreeningPrimary,
                            strokeWidth = 2.dp
                        )

                        Text(
                            text = "Memuat jadwal dan kuota skrining...",
                            fontSize = 12.sp,
                            color = ScreeningMuted
                        )
                    }
                }

                quotaState.errorMessage != null || quota == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = quotaState.errorMessage
                                ?: "Kuota belum dapat dimuat. Periksa koneksi lalu coba lagi.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = ScreeningError
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = onRefreshQuota,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text("Muat ulang jadwal")
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Periode aktif: ${quota.weekStartDate.format(dateFormatter)} - ${quota.weekEndDate.format(dateFormatter)}",
                        fontSize = 12.sp,
                        color = ScreeningMuted
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            ScreeningTypeCard(
                title = "Manual",
                description = "Skrining mandiri yang dapat dilakukan secara fleksibel satu kali dalam periode minggu aktif.",
                quotaDescription = manualQuotaText,
                enabled = manualAvailable,
                isSelected = selectedType == ScreeningType.MANUAL && manualAvailable,
                onClick = {
                    onSelectType(ScreeningType.MANUAL)
                }
            )

            Spacer(Modifier.height(14.dp))

            ScreeningTypeCard(
                title = "Rutin",
                description = "Skrining berkala yang terbuka pada akhir periode minggu sejak aktivasi aplikasi.",
                quotaDescription = routineQuotaText,
                enabled = routineAvailable,
                isSelected = selectedType == ScreeningType.ROUTINE && routineAvailable,
                onClick = {
                    onSelectType(ScreeningType.ROUTINE)
                }
            )

            Spacer(Modifier.height(20.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = when {
                    selectedType == null ->
                        "Pilih jenis skrining terlebih dahulu."
                    selectedAvailable ->
                        "${selectedType.name.lowercase().replaceFirstChar { it.uppercase() }} dipilih."
                    quotaState.isLoading ->
                        "Tunggu hingga jadwal selesai dimuat."
                    quotaState.errorMessage != null || quota == null ->
                        "Jadwal belum dapat diverifikasi."
                    selectedType == ScreeningType.ROUTINE && !routineAvailable ->
                        "Skrining rutin belum masuk jadwal pembukaan atau sudah digunakan."
                    else ->
                        "Kuota jenis skrining ini sudah digunakan."
                },
                fontSize = 12.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(8.dp))

            ScreeningPrimaryButton(
                text = "Mulai",
                enabled = selectedAvailable,
                onClick = onStart
            )
        }
    }
}

@Composable
private fun ScreeningTypeCard(
    title: String,
    description: String,
    quotaDescription: String,
    enabled: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val activeSelection = enabled && isSelected

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .alpha(if (activeSelection) 0.5f else 1f)
            .background(
                when {
                    !enabled -> ScreeningDisabled
                    activeSelection -> ScreeningPrimaryContainer
                    else -> Color.White
                }
            )
            .border(
                width = if (activeSelection) 2.dp else 1.dp,
                color = if (activeSelection) ScreeningPrimary else ScreeningDisabled,
                shape = shape
            )
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                selected = activeSelection
            }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) ScreeningText else ScreeningDisabledText
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = if (enabled) ScreeningMuted else ScreeningDisabledText
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = quotaDescription,
            fontSize = 11.sp,
            fontWeight = if (!enabled) FontWeight.Medium else FontWeight.Normal,
            color = if (enabled) ScreeningMuted else ScreeningDisabledText
        )
    }
}

@Composable
private fun ScreeningIntroductionContent(
    screeningType: ScreeningType?,
    onBack: () -> Unit,
    onBegin: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Skrining",
            onBack = onBack,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            ScreeningInfoCard(
                title = if (screeningType == ScreeningType.ROUTINE) {
                    "Sesi Rutin"
                } else {
                    "Sesi Manual"
                },
                description = "Kuesioner Kesehatan Pasien (PHQ-9) untuk memantau kondisi emosionalmu."
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Skrining Kesehatan\nEmosional",
                fontSize = 24.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Kuesioner PHQ-9 · 9 pertanyaan",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(22.dp))

            ScreeningInfoCard(
                title = "Petunjuk Pengisian",
                description = "Pilih jawaban yang paling sesuai dengan apa yang kamu rasakan dalam 2 minggu terakhir. Luangkan waktu sekitar 2-3 menit untuk menjawab setiap pertanyaan dengan nyaman."
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Hasil skrining ini merupakan penapisan awal untuk membantumu merefleksikan kondisi emosional dan dapat disertakan saat konsultasi dengan tenaga profesional.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningMuted
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            ScreeningPrimaryButton(
                text = "Mulai Skrining",
                onClick = onBegin
            )
        }
    }
}

@Composable
private fun ScreeningQuestionContent(
    currentQuestionNumber: Int,
    answers: Map<Int, Int>,
    currentAnswer: Int?,
    canGoToPreviousQuestion: Boolean,
    canGoToNextQuestion: Boolean,
    canSubmit: Boolean,
    onBack: () -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSubmit: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    val isLastQuestion = currentQuestionNumber == 9

    val canContinue = if (isLastQuestion) {
        canSubmit
    } else {
        canGoToNextQuestion
    }

    val animatedProgress by animateFloatAsState(
        targetValue = currentQuestionNumber / 9f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "screeningProgressBar"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Skrining PHQ-9",
            onBack = onBack,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ScreeningDisabled)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ScreeningPrimary)
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Pertanyaan $currentQuestionNumber dari 9",
                fontSize = 12.sp,
                color = ScreeningPrimary,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${answers.size} jawaban terisi",
                fontSize = 11.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentQuestionNumber,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        )
                    } else {
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth },
                            animationSpec = tween(300, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(300))).togetherWith(
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                        )
                    }
                },
                label = "questionSlideAnimation"
            ) { questionNum ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ScreeningPrimaryContainer)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Pertanyaan $questionNum",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ScreeningPrimary
                        )

                        Spacer(Modifier.height(7.dp))

                        Text(
                            text = ScreeningQuestions.getOrElse(questionNum - 1) {
                                "Pertanyaan tidak tersedia."
                            },
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Medium,
                            color = ScreeningText
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    ScreeningAnswerLabels.forEachIndexed { index, label ->
                        val isSelected = answers[questionNum] == index
                        ScreeningAnswerOption(
                            label = label,
                            isSelected = isSelected,
                            onClick = {
                                onSelectAnswer(index)
                            }
                        )

                        if (index != ScreeningAnswerLabels.lastIndex) {
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Pilih frekuensi yang paling mendekati pengalamanmu selama 2 minggu terakhir.",
                fontSize = 11.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(8.dp))

            ScreeningPrimaryButton(
                text = if (isLastQuestion) "Selesaikan" else "Lanjut",
                enabled = canContinue,
                onClick = {
                    if (isLastQuestion) {
                        onSubmit()
                    } else {
                        onNext()
                    }
                }
            )
        }
    }
}

@Composable
private fun ScreeningAnswerOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(
                if (isSelected) ScreeningPrimaryContainer else Color.White
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) ScreeningPrimary else ScreeningOutline,
                shape = shape
            )
            .clickable(
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics {
                selected = isSelected
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScreeningRadioIndicator(isSelected)

        Spacer(Modifier.size(10.dp))

        Text(
            text = label,
            fontSize = 13.sp,
            color = ScreeningText
        )
    }
}

@Composable
private fun ScreeningRadioIndicator(
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .border(
                width = 1.5.dp,
                color = ScreeningPrimary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(ScreeningPrimary)
            )
        }
    }
}

@Composable
private fun ScreeningProcessingContent(
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Skrining",
            onBack = null,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = ScreeningPrimary,
                strokeWidth = 2.dp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Sedang memproses\njawabanmu...",
                fontSize = 22.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Jawaban divalidasi dan hasil penapisan disiapkan.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(18.dp))

            ScreeningInfoCard(
                title = "Jawaban Tersimpan Aman",
                description = "Seluruh data penapisanmu tersimpan secara terenkripsi di perangkat."
            )
        }
    }
}

@Composable
private fun ScreeningErrorContent(
    onRetry: () -> Unit,
    onReturn: () -> Unit,
    errorMessage: String?,
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Kendala",
            onBack = onReturn,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(2.dp, ScreeningError, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    fontSize = 23.sp,
                    color = ScreeningError,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Proses belum\nselesai.",
                fontSize = 22.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = errorMessage ?: "Terjadi kendala. Silakan coba kembali.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(18.dp))

            ScreeningInfoCard(
                title = "Jawabanmu tetap tersedia",
                description = "Jawaban yang sudah kamu pilih tetap tersimpan sementara. Kamu tidak perlu mengulang kuesioner dari awal."
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ScreeningPrimaryButton(
                text = "Coba Lagi",
                onClick = onRetry
            )

            OutlinedButton(
                onClick = onReturn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Kembali ke pertanyaan")
            }
        }
    }
}

@Composable
private fun ScreeningCrisisContent(
    onContinue: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningWarmSupport)
    ) {
        SelarasHeader(
            title = "Bantuan & Dukungan",
            onBack = null,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(18.dp))

            Text(
                text = "Kami di sini\nuntukmu.",
                fontSize = 25.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Jawabanmu menunjukkan adanya indikasi yang memerlukan perhatian khusus. Kamu berhak mendapatkan bantuan dan dukungan profesional.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningText
            )

            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Layanan Bantuan Profesional",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScreeningText
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Jika kamu merasa terbebani atau memerlukan teman bicara, layanan dukungan krisis dan konseling profesional tersedia untuk mendampingimu.",
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = ScreeningMuted
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Indikator bantuan bukan diagnosis medis. Kamu tetap berhak mendapatkan dukungan dan bantuan profesional.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = ScreeningMuted
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onOpenHelp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Lihat Kontak Bantuan")
            }

            ScreeningPrimaryButton(
                text = "Lanjutkan ke Hasil",
                onClick = onContinue
            )
        }
    }
}

@Composable
private fun ScreeningResultContent(
    result: ScreeningResult,
    onReset: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenCounselorSummary: () -> Unit,
    modifier: Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        SelarasHeader(
            title = "Hasil Skrining",
            onBack = onReset,
            actions = { ScreeningHelpAction(onOpenHelp) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Hasil Skrining PHQ-9",
                fontSize = 23.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScreeningPrimaryContainer)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Hasil Skrining Tersimpan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScreeningText
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Kategori: ${result.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    fontSize = 12.sp,
                    color = ScreeningMuted
                )
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(18.dp)
            ) {
                Text(
                    text = "Skor Total",
                    fontSize = 13.sp,
                    color = ScreeningMuted
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "${result.totalScore} / 27",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = ScreeningPrimary
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Instrumen: ${result.instrumentVersion}",
                    fontSize = 12.sp,
                    color = ScreeningMuted
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Hasil ini merupakan penapisan awal untuk memantau kesejahteraan emosionalmu secara mandiri.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = ScreeningMuted
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScreeningPrimaryButton(
                text = "Lihat Riwayat",
                onClick = onOpenHistory
            )

            OutlinedButton(
                onClick = onOpenCounselorSummary,
                enabled = result.id > 0L,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Buka Ringkasan Konselor")
            }

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Kembali ke Beranda")
            }
        }
    }
}

@Composable
private fun ScreeningInfoCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ScreeningPrimaryContainer)
            .padding(15.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = ScreeningText
        )

        Spacer(Modifier.height(5.dp))

        Text(
            text = description,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = ScreeningMuted
        )
    }
}

@Composable
private fun ScreeningPrimaryButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ScreeningPrimary,
            contentColor = Color.White,
            disabledContainerColor = ScreeningDisabled,
            disabledContentColor = ScreeningDisabledText
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
