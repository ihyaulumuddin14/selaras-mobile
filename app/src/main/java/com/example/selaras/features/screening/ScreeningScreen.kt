
package com.example.selaras.features.screening

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType

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
    "Jarang",
    "Kadang-kadang",
    "Sering"
)

private val ScreeningDemoQuestions = listOf(
    "Saya merasa sulit memulai aktivitas harian.",
    "Energi saya cepat habis saat menjalani kegiatan.",
    "Saya kesulitan berkonsentrasi ketika belajar, bekerja, atau membaca.",
    "Saya merasa kewalahan menghadapi tugas atau tanggung jawab.",
    "Saya sulit merasa tenang ketika menghadapi situasi sehari-hari.",
    "Pola tidur atau waktu istirahat saya terasa terganggu.",
    "Saya kurang tertarik mengikuti kegiatan yang biasanya saya lakukan.",
    "Saya merasa sulit menyelesaikan kegiatan sehari-hari.",
    "Saya membutuhkan waktu lebih lama untuk memulihkan energi setelah beraktivitas."
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        ScreeningTopBar(
            onBack = null,
            onOpenHelp = onOpenHelp
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
                text = "Pilih sesi\nrefleksimu.",
                fontSize = 24.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Pilih sesi demo manual atau rutin. Keduanya memiliki kuota mingguan yang terpisah.",
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
                            text = "Memuat kuota skrining...",
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
                            Text("Muat ulang kuota")
                        }
                    }
                }

                else -> {
                    Text(
                        text = "Periode kuota: ${
                            quota.weekStartDate.format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        } - ${
                            quota.weekEndDate.format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                            )
                        }",
                        fontSize = 12.sp,
                        color = ScreeningMuted
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            ScreeningTypeCard(
                title = "Manual",
                description = "Skrining mandiri yang dapat dilakukan secara fleksibel.",
                quotaDescription = when {
                    quotaState.isLoading -> "Memuat kuota..."
                    quotaState.errorMessage != null || quota == null ->
                        "Kuota belum dapat diverifikasi"
                    manualAvailable -> "Sisa kuota: ${quota.manualRemaining} kali minggu ini"
                    else -> "Kuota minggu ini sudah digunakan"
                },
                enabled = manualAvailable,
                isSelected = selectedType == ScreeningType.MANUAL && manualAvailable,
                onClick = {
                    onSelectType(ScreeningType.MANUAL)
                }
            )

            Spacer(Modifier.height(14.dp))

            ScreeningTypeCard(
                title = "Rutin",
                description = "Skrining berkala sesuai jadwal yang ditentukan sistem.",
                quotaDescription = when {
                    quotaState.isLoading -> "Memuat kuota..."
                    quotaState.errorMessage != null || quota == null ->
                        "Kuota belum dapat diverifikasi"
                    routineAvailable -> "Sisa kuota: ${quota.routineRemaining} kali minggu ini"
                    else -> "Kuota minggu ini sudah digunakan"
                },
                enabled = routineAvailable,
                isSelected = selectedType == ScreeningType.ROUTINE && routineAvailable,
                onClick = {
                    onSelectType(ScreeningType.ROUTINE)
                }
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Kuota manual dan rutin dikelola secara terpisah.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = ScreeningMuted
            )
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
                        "Tunggu hingga kuota selesai dimuat."
                    quotaState.errorMessage != null || quota == null ->
                        "Kuota belum dapat diverifikasi."
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

            ScreeningRadioIndicator(activeSelection)
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
        ScreeningTopBar(
            onBack = onBack,
            onOpenHelp = onOpenHelp
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
                    "Kesempatan rutin"
                } else {
                    "Kesempatan manual"
                },
                description = "Satu sesi refleksi demo mengikuti pilihanmu."
            )

            Spacer(Modifier.height(22.dp))

            Text(
                text = "Refleksi Kondisi\nEmosional",
                fontSize = 24.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "Sesi refleksi demo · 9 pertanyaan",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = ScreeningText
            )

            Spacer(Modifier.height(22.dp))

            ScreeningInfoCard(
                title = "Latihan demo",
                description = "Pertanyaan contoh untuk menguji alur aplikasi."
            )

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Pertanyaan ini dibuat sementara untuk demonstrasi aplikasi. Ini bukan instrumen PHQ-9, bukan alat skrining tervalidasi, dan tidak dapat digunakan untuk menilai atau mendiagnosis kondisi kesehatan mental.",
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
                text = "Mulai Refleksi Demo",
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreeningBackground)
    ) {
        ScreeningTopBar(
            onBack = onBack,
            onOpenHelp = onOpenHelp
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(9) { index ->
                    val segmentModifier = Modifier
                        .weight(1f)
                        .height(7.dp)
                        .clip(CircleShape)

                    when {
                        index < currentQuestionNumber - 1 -> {
                            Box(
                                modifier = segmentModifier
                                    .background(ScreeningPrimary)
                            )
                        }

                        index == currentQuestionNumber - 1 -> {
                            Box(
                                modifier = segmentModifier
                                    .border(
                                        1.dp,
                                        ScreeningPrimary,
                                        CircleShape
                                    )
                            )
                        }

                        else -> {
                            Box(
                                modifier = segmentModifier
                                    .background(ScreeningDisabled)
                            )
                        }
                    }
                }
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ScreeningPrimaryContainer)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Pertanyaan refleksi demo $currentQuestionNumber",
                    fontSize = 11.sp,
                    color = ScreeningPrimary
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = ScreeningDemoQuestions.getOrElse(currentQuestionNumber - 1) {
                        "Pertanyaan demo tidak tersedia."
                    },
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = ScreeningText
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Mode demo: jawaban hanya digunakan untuk menguji alur aplikasi dan tidak memiliki makna klinis.",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(14.dp))

            ScreeningAnswerLabels.forEachIndexed { index, label ->
                ScreeningAnswerOption(
                    label = label,
                    isSelected = currentAnswer == index,
                    onClick = {
                        onSelectAnswer(index)
                    }
                )

                if (index != ScreeningAnswerLabels.lastIndex) {
                    Spacer(Modifier.height(8.dp))
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
                text = "Pilih frekuensi yang paling mendekati pengalamanmu.",
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
        ScreeningTopBar(
            onBack = null,
            onOpenHelp = onOpenHelp
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
                text = "Jawaban divalidasi dan hasil disiapkan untuk disimpan.",
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(18.dp))

            ScreeningInfoCard(
                title = "Jawabanmu tetap tersedia",
                description = "Jawaban akan dipertahankan jika proses penyimpanan mengalami kendala."
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
        ScreeningTopBar(
            onBack = onReturn,
            onOpenHelp = onOpenHelp
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
                description = "Jawaban yang sudah kamu pilih tetap tersimpan sementara. Kamu tidak perlu mengulang kuesioner."
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
        ScreeningTopBar(
            onBack = null,
            onOpenHelp = onOpenHelp
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
                text = "Jawabanmu menunjukkan adanya hal yang mungkin perlu mendapat perhatian lebih lanjut. Kamu dapat mencari dukungan jika membutuhkannya.",
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
                    text = "Informasi Layanan Bantuan Profesional",
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScreeningText
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Informasi kontak dan kanal layanan bantuan akan ditampilkan setelah diverifikasi.",
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
                Text("Lihat Bantuan")
            }

            ScreeningPrimaryButton(
                text = "Simpan dan Lanjutkan",
                onClick = onContinue
            )

            Text(
                text = "Hasil sudah tersimpan. Tombol ini hanya melanjutkan.",
                fontSize = 11.sp,
                color = ScreeningMuted
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
        ScreeningTopBar(
            onBack = onReset,
            onOpenHelp = onOpenHelp
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
                text = "Hasil Refleksi Demo",
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
                    text = "Hasil demo tersimpan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ScreeningText
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "ID hasil: ${result.id}",
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
                    text = "Skor demo",
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
                    text = "Versi data: ${result.instrumentVersion}",
                    fontSize = 12.sp,
                    color = ScreeningMuted
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Ini adalah hasil simulasi dari pertanyaan demo buatan. Skor tidak memiliki interpretasi klinis, bukan hasil PHQ-9, dan tidak boleh digunakan untuk mengambil keputusan kesehatan.",
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = ScreeningMuted
            )

            Spacer(Modifier.height(18.dp))

            ScreeningInfoCard(
                title = "Data demo tersimpan",
                description = "ID hasil demo: ${result.id}. Data ini hanya untuk menguji penyimpanan dan alur aplikasi, bukan untuk penilaian klinis."
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

            Text(
                text = "Data demo tetap berlabel simulasi dan bukan PHQ-9 tervalidasi.",
                fontSize = 11.sp,
                color = ScreeningMuted
            )

            OutlinedButton(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Kembali ke pilihan")
            }
        }
    }
}

@Composable
private fun ScreeningTopBar(
    onBack: (() -> Unit)?,
    onOpenHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreeningBackground)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "←",
                    fontSize = 20.sp,
                    color = ScreeningPrimary
                )
            }
        } else {
            Spacer(Modifier.size(32.dp))
        }

        Spacer(Modifier.size(8.dp))

        Text(
            text = "Selaras",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = ScreeningText
        )

        Spacer(Modifier.weight(1f))

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
