@file:Suppress("NewApi")

package com.example.selaras.features.counselor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.selaras.SelarasApplication
import com.example.selaras.core.ui.components.SelarasIcons
import com.example.selaras.domain.usecase.counselor.CreateCounselorSummaryUseCase
import com.example.selaras.domain.usecase.counselor.GetCounselorEligibilityUseCase
import kotlinx.coroutines.flow.flowOf
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CanvasBackground = Color(0xFFF7F7F2)
private val PineGreen = Color(0xFF356859)
private val LightSageGreen = Color(0xFFE0EDE4)
private val DarkGreenText = Color(0xFF243B35)
private val MutedGreenText = Color(0xFF52675F)
private val LavenderCard = Color(0xFFEEE5F5)
private val WarningPink = Color(0xFFFDE8E8)
private val WarningBorder = Color(0xFFF5C2C7)
private val DisabledContainer = Color(0xFFE2E8E3)
private val DisabledContent = Color(0xFF8E9E98)
private val CardBorder = Color(0xFFE5ECE8)

@Composable
fun CounselorSummaryScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToCheckIn: () -> Unit = onNavigateBack,
    modifier: Modifier = Modifier,
    viewModel: CounselorSummaryViewModel? = null
) {
    val context = LocalContext.current
    val effectiveViewModel: CounselorSummaryViewModel = viewModel ?: remember {
        val app = context.applicationContext as? SelarasApplication
        if (app != null) {
            val container = app.container
            val createUseCase = CreateCounselorSummaryUseCase(
                container.checkInRepository,
                container.screeningRepository,
                container.counselorRepository
            )
            val eligibilityUseCase = GetCounselorEligibilityUseCase(container.checkInRepository)
            CounselorSummaryViewModel(
                checkInRepository = container.checkInRepository,
                screeningRepository = container.screeningRepository,
                counselorRepository = container.counselorRepository,
                createSummaryUseCase = createUseCase,
                getEligibilityUseCase = eligibilityUseCase,
                autoLoad = true
            )
        } else {
            CounselorSummaryViewModel.createPreviewInstance()
        }
    }

    val uiState by effectiveViewModel.uiState.collectAsState()
    val handoffStore = remember(context) {
        (context.applicationContext as? SelarasApplication)?.container?.screeningHandoffStore
    }
    val handoffFlow = remember(handoffStore) {
        handoffStore?.selectedResultId ?: flowOf<Long?>(null)
    }
    val handoffId by handoffFlow.collectAsState(initial = null)
    LaunchedEffect(effectiveViewModel, handoffId) {
        handoffId?.let(effectiveViewModel::acceptHandoffResultId)
    }

    val headerTitle = when (uiState.screenState) {
        CounselorScreenState.SELECT_CONTENT,
        CounselorScreenState.INELIGIBLE,
        CounselorScreenState.PREPARING -> "Ringkasan untuk Konselor"
        CounselorScreenState.PREVIEW,
        CounselorScreenState.SOURCE_CHANGED,
        CounselorScreenState.GENERATING_PDF,
        CounselorScreenState.PDF_ERROR -> "Pratinjau Ringkasan"
        CounselorScreenState.EXPORT_SUCCESS -> "Dokumen Siap"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        // App Header with Support Agent icon (navigates to Help)
        CounselorHeader(
            title = headerTitle,
            onBack = {
                when (uiState.screenState) {
                    CounselorScreenState.SELECT_CONTENT,
                    CounselorScreenState.INELIGIBLE -> onNavigateBack()
                    CounselorScreenState.PREVIEW,
                    CounselorScreenState.SOURCE_CHANGED -> effectiveViewModel.backToSelectContent()
                    CounselorScreenState.PDF_ERROR,
                    CounselorScreenState.GENERATING_PDF -> effectiveViewModel.backToPreview()
                    CounselorScreenState.EXPORT_SUCCESS,
                    CounselorScreenState.PREPARING -> onNavigateBack()
                }
            },
            onSupportClick = onNavigateToHelp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            when (uiState.screenState) {
                CounselorScreenState.SELECT_CONTENT -> {
                    SelectContentScreen(
                        uiState = uiState,
                        onToggleScreening = { id, checked -> effectiveViewModel.toggleScreeningItem(id, checked) },
                        onToggleIncludeNotes = { effectiveViewModel.toggleIncludeNotes(it) },
                        onToggleNote = { date, checked -> effectiveViewModel.toggleNoteItem(date, checked) },
                        onCreateSummary = { effectiveViewModel.createSummary() },
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.INELIGIBLE -> {
                    IneligibleScreen(
                        uiState = uiState,
                        onIsiCheckIn = onNavigateToCheckIn,
                        onLihatRiwayat = onNavigateBack,
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.PREPARING -> {
                    PreparingScreen(
                        uiState = uiState,
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.PREVIEW -> {
                    PreviewScreen(
                        uiState = uiState,
                        onExportPdf = { effectiveViewModel.exportPdf(context) },
                        onUbahPilihan = { effectiveViewModel.backToSelectContent() },
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.SOURCE_CHANGED -> {
                    SourceChangedScreen(
                        uiState = uiState,
                        onPerbaruiPratinjau = { effectiveViewModel.refreshPreview() },
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.GENERATING_PDF -> {
                    GeneratingPdfScreen(
                        uiState = uiState,
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.PDF_ERROR -> {
                    PdfErrorScreen(
                        uiState = uiState,
                        onRetryExport = { effectiveViewModel.retryExport(context) },
                        onBackToPreview = { effectiveViewModel.backToPreview() },
                        onSupportClick = onNavigateToHelp
                    )
                }
                CounselorScreenState.EXPORT_SUCCESS -> {
                    ExportSuccessScreen(
                        uiState = uiState,
                        onSimpanPdf = {
                            Toast.makeText(context, "Dokumen PDF berhasil disimpan di penyimpanan lokal aplikasi", Toast.LENGTH_SHORT).show()
                        },
                        onPilihCaraBerbagi = {
                            sharePdf(context, uiState.generatedPdfFile)
                        },
                        onKembaliKeRiwayat = onNavigateBack,
                        onSupportClick = onNavigateToHelp
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// -------------------------------------------------------------
// Sub-screen 1: Select Content (Clean Checkbox + Info Dialog Pattern)
// -------------------------------------------------------------
@Composable
private fun SelectContentScreen(
    uiState: CounselorSummaryUiState,
    onToggleScreening: (Long, Boolean) -> Unit,
    onToggleIncludeNotes: (Boolean) -> Unit,
    onToggleNote: (LocalDate, Boolean) -> Unit,
    onCreateSummary: () -> Unit,
    onSupportClick: () -> Unit
) {
    var selectedScreeningForInfo by remember { mutableStateOf<CounselorScreeningUiItem?>(null) }
    var selectedNoteForInfo by remember { mutableStateOf<CounselorNoteUiItem?>(null) }

    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    val periodText = "Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}"
    PeriodBadgeChip(periodText)
    Spacer(modifier = Modifier.height(14.dp))

    WeeklyDaysRecordedCard(
        recordedDays = uiState.recordedDays,
        totalDays = uiState.totalDays,
        recordedDaysText = "Tercatat: ${uiState.recordedDays} hari",
        missingDaysText = "Belum tercatat: ${uiState.totalDays - uiState.recordedDays} hari",
        isEligible = true
    )

    if (uiState.handoffResultId != null) {
        Spacer(modifier = Modifier.height(14.dp))
        CardItemContainer(containerColor = LavenderCard) {
            Text(
                text = "Hasil skrining #${uiState.handoffResultId} telah diterima dari F1.",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreenText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hasil ini dipilih secara otomatis, tetapi dapat dilepas dari ringkasan.",
                fontSize = 12.sp,
                color = MutedGreenText
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Pilih data yang ingin kamu bawa",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(12.dp))

    // 1. Check-in Harian (Wajib / Utama)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(LightSageGreen)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Wajib",
                tint = PineGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Catatan Check-in Harian",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Analisis suasana hati, tidur, & energi (${uiState.recordedDays} dari ${uiState.totalDays} hari)",
                    fontSize = 12.sp,
                    color = MutedGreenText
                )
            }
            Text(
                text = "Wajib",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PineGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // 2. Hasil Skrining PHQ-9 (Opsional)
    Text(
        text = "Hasil Skrining PHQ-9 · Opsional",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (uiState.availableScreenings.isEmpty()) {
        CardItemContainer(containerColor = LavenderCard) {
            Text(
                text = "Tidak ada hasil skrining pada periode ini.",
                fontSize = 12.sp,
                color = MutedGreenText
            )
        }
    } else {
        uiState.availableScreenings.forEach { screening ->
            CleanSelectableItem(
                checked = screening.isSelected,
                onCheckedChange = { onToggleScreening(screening.id, it) },
                title = screening.title,
                onInfoClick = { selectedScreeningForInfo = screening }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Catatan Pribadi (Opsional)
    Text(
        text = "Catatan Refleksi Pribadi · Opsional",
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Master Toggle Checkbox for Personal Notes
    CleanSelectableItem(
        checked = uiState.includePersonalNotes,
        onCheckedChange = onToggleIncludeNotes,
        title = "Sertakan Catatan Pribadi",
        badgeText = if (uiState.includePersonalNotes) "Aktif — Pilih catatan di bawah" else "Nonaktif"
    )

    if (uiState.includePersonalNotes) {
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            if (uiState.availableNotes.isEmpty()) {
                Text(
                    text = "Tidak ada catatan refleksi pada periode ini.",
                    fontSize = 12.sp,
                    color = MutedGreenText
                )
            } else {
                uiState.availableNotes.forEach { noteItem ->
                    CleanSelectableItem(
                        checked = noteItem.isSelected,
                        onCheckedChange = { onToggleNote(noteItem.date, it) },
                        title = noteItem.title,
                        onInfoClick = { selectedNoteForInfo = noteItem }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    PrimaryButton(text = "Buat Ringkasan Mingguan (PDF)", onClick = onCreateSummary)

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "*Membuat ringkasan tidak mengurangi kuota skrining berkala.",
        fontSize = 11.sp,
        color = MutedGreenText,
        lineHeight = 16.sp
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)

    // Popup Info Skrining
    if (selectedScreeningForInfo != null) {
        val sc = selectedScreeningForInfo!!
        SelarasInfoDialog(
            title = sc.typeName,
            subtitle = sc.dateString,
            details = listOf(
                "Instrumen" to "Kuesioner PHQ-9 (9 Pertanyaan)",
                "Skor Total" to "${sc.score} / 27",
                "Kategori" to sc.categoryText,
                "Keterangan" to "Hasil penapisan mandiri ini bertujuan untuk mempermudah percakapan dengan konselor dan bukan vonis diagnosis medis."
            ),
            onDismiss = { selectedScreeningForInfo = null }
        )
    }

    // Popup Info Catatan Pribadi
    if (selectedNoteForInfo != null) {
        val note = selectedNoteForInfo!!
        SelarasInfoDialog(
            title = "Catatan Refleksi Pribadi",
            subtitle = note.date.format(periodFmt),
            details = listOf(
                "Isi Catatan" to "\"${note.noteText}\"",
                "Keterangan" to "Hanya catatan yang kamu centang yang akan dimasukkan ke dalam dokumen ringkasan untuk konselor."
            ),
            onDismiss = { selectedNoteForInfo = null }
        )
    }
}

@Composable
private fun IneligibleScreen(
    uiState: CounselorSummaryUiState,
    onIsiCheckIn: () -> Unit,
    onLihatRiwayat: () -> Unit,
    onSupportClick: () -> Unit
) {
    val daysNeeded = (4 - uiState.recordedDays).coerceAtLeast(1)
    WarningCardContainer(
        title = "Data Belum Mencukupi",
        message = "Dibutuhkan minimal 4 hari catatan check-in dalam satu minggu agar ringkasan memiliki pola yang bermakna untuk konselor. Saat ini baru tercatat ${uiState.recordedDays} hari."
    )
    Spacer(modifier = Modifier.height(14.dp))

    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    PeriodBadgeChip("Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}")
    Spacer(modifier = Modifier.height(12.dp))

    // Days Recorded Card
    WeeklyDaysRecordedCard(
        recordedDays = uiState.recordedDays,
        totalDays = uiState.totalDays,
        recordedDaysText = "Tercatat: ${uiState.recordedDays} hari",
        missingDaysText = "Belum tercatat: ${uiState.totalDays - uiState.recordedDays} hari",
        isEligible = false
    )

    Spacer(modifier = Modifier.height(20.dp))
    DisabledCustomButton(text = "Buat Ringkasan Mingguan (PDF)")

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Lengkapi $daysNeeded hari check-in lagi untuk membuka pratinjau dan ekspor dokumen PDF.",
        fontSize = 11.sp,
        color = MutedGreenText,
        lineHeight = 17.sp
    )

    if (!uiState.isTodayCheckedIn) {
        Spacer(modifier = Modifier.height(16.dp))
        PrimaryButton(text = "Isi Check-in Hari Ini", onClick = onIsiCheckIn)
    }

    Spacer(modifier = Modifier.height(18.dp))
    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "Hasil Skrining Bukan Pengganti Check-in",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ringkasan konselor membutuhkan minimal 4 hari check-in. Hasil skrining PHQ-9 dapat disertakan secara opsional.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun PreparingScreen(
    uiState: CounselorSummaryUiState,
    onSupportClick: () -> Unit
) {
    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    PeriodBadgeChip("Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}")
    Spacer(modifier = Modifier.height(24.dp))

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = PineGreen,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Menyiapkan ringkasanmu...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Kami sedang menghitung rata-rata catatan harianmu dan menyusun bagian yang telah kamu pilih.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "Tahapan Penyusunan Dokumen",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "1 · Menghitung rata-rata suasana hati, tidur, & energi\n2 · Menggabungkan data skrining & catatan pilihan\n3 · Menyiapkan dokumen ringkasan untuk kamu tinjau",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 22.sp
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    DisabledCustomButton(text = "Sedang Memproses...")

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun PreviewScreen(
    uiState: CounselorSummaryUiState,
    onExportPdf: () -> Unit,
    onUbahPilihan: () -> Unit,
    onSupportClick: () -> Unit
) {
    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))

    // Lavender intro banner
    CardItemContainer(containerColor = LavenderCard) {
        Text(
            text = "Cerita Kecilmu, Lebih Terarah",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ringkasan ini merangkum catatan suasana hati, tidur, dan energimu agar sesi konsultasi dengan konselor berjalan lebih mudah dan fokus.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 18.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    PeriodBadgeChip("Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}")
    Spacer(modifier = Modifier.height(12.dp))

    DocumentInfoCard(uiState = uiState)

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "01 · Analisis Check-in Harian",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(8.dp))

    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "${uiState.recordedDays} dari ${uiState.totalDays} Hari Tercatat",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        val recordedDatesStr = uiState.recordedDates.joinToString(", ") { it.format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id"))) }
        Text(
            text = "Tercatat pada tanggal: $recordedDatesStr",
            fontSize = 11.sp,
            color = MutedGreenText
        )
        Spacer(modifier = Modifier.height(14.dp))

        MetricRow("Rata-rata Suasana Hati", String.format(Locale.forLanguageTag("id"), "%.1f / 5", uiState.avgMood))
        MetricRow("Rata-rata Durasi Tidur", String.format(Locale.forLanguageTag("id"), "%.1f jam", uiState.avgSleepHours))
        MetricRow("Rata-rata Tingkat Energi", String.format(Locale.forLanguageTag("id"), "%.1f / 10", uiState.avgEnergy))

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Rata-rata dihitung dari ${uiState.recordedDays} hari yang kamu isi. Tanggal tanpa pengisian tidak dianggap sebagai nilai nol.",
            fontSize = 11.sp,
            color = MutedGreenText
        )
        Spacer(modifier = Modifier.height(12.dp))

        val rincianHarianText = if (uiState.dailyItems.any { it.isRecorded }) {
            uiState.dailyItems.filter { it.isRecorded }.joinToString("\n") { item ->
                val fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id"))
                "• ${item.date.format(fmt)}: Mood ${item.mood}/5 · ${String.format(Locale.forLanguageTag("id"), "%.1f", item.sleepHours)} jam · Energi ${item.energy}/10"
            }
        } else {
            "• Belum ada catatan harian"
        }
        Text(
            text = "Rincian Harian (Mood / Tidur / Energi):\n$rincianHarianText",
            fontSize = 11.sp,
            color = MutedGreenText,
            lineHeight = 18.sp
        )
    }

    Spacer(modifier = Modifier.height(10.dp))
    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "Catatan Observasi Mandiri",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Data ini murni rekaman observasi mandirimu. Aplikasi tidak memberikan kesimpulan klinis atau vonis kondisi kesehatan mental secara otomatis.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "02 · Hasil Skrining Terpilih",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Hasil skrining ditampilkan terpisah dari data harian dan tidak digabung menjadi diagnosis medis.",
        fontSize = 11.sp,
        color = MutedGreenText
    )
    Spacer(modifier = Modifier.height(8.dp))

    val selectedScreenings = uiState.availableScreenings.filter { it.isSelected }
    if (selectedScreenings.isEmpty()) {
        CardItemContainer(containerColor = Color.White) {
            Text(
                text = "Tanpa Hasil Skrining",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreenText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tidak ada hasil kuesioner skrining yang disertakan. Empat hari catatan harian sudah cukup untuk membuat ringkasan.",
                fontSize = 12.sp,
                color = MutedGreenText
            )
        }
    } else {
        selectedScreenings.forEach { sc ->
            CardItemContainer(containerColor = Color.White) {
                Text(text = sc.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = sc.details, fontSize = 11.sp, color = MutedGreenText, lineHeight = 17.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "03 · Catatan Pribadi",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(8.dp))

    val selectedNotes = uiState.availableNotes.filter { it.isSelected }
    if (!uiState.includePersonalNotes || selectedNotes.isEmpty()) {
        CardItemContainer(containerColor = Color.White) {
            Text(
                text = "Catatan Tidak Disertakan",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreenText
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pilihan catatan pribadi tidak aktif. Tidak ada teks refleksi pribadi dalam dokumen ini.",
                fontSize = 12.sp,
                color = MutedGreenText
            )
        }
    } else {
        selectedNotes.forEach { note ->
            CardItemContainer(containerColor = Color.White) {
                Text(text = note.title, fontSize = 11.sp, color = MutedGreenText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "\"${note.noteText}\"", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkGreenText)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Hanya catatan yang kamu setujui yang akan muncul di dokumen.", fontSize = 11.sp, color = MutedGreenText)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    CardItemContainer(containerColor = LavenderCard) {
        Text(
            text = "Bukan Diagnosis Klinis",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ringkasan ini bertujuan mempermudah percakapan saat berkonsultasi, bukan sebagai penetapan kondisi medis atau vonis klinis.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = "Periksa seluruh isi dokumen sebelum mengekspor. Berkas PDF baru akan dibuat saat kamu menekan tombol di bawah.",
        fontSize = 11.sp,
        color = MutedGreenText,
        lineHeight = 16.sp
    )
    Spacer(modifier = Modifier.height(14.dp))

    PrimaryButton(text = "Ekspor ke File PDF", onClick = onExportPdf)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedCustomButton(text = "Ubah Pilihan Data", onClick = onUbahPilihan)

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun SourceChangedScreen(
    uiState: CounselorSummaryUiState,
    onPerbaruiPratinjau: () -> Unit,
    onSupportClick: () -> Unit
) {
    WarningCardContainer(
        title = "Ada Pembaruan Data Check-in",
        message = "Terdapat data check-in yang baru masuk sejak pratinjau dibuat. Perbarui pratinjau agar dokumen memuat rekaman terkini."
    )
    Spacer(modifier = Modifier.height(14.dp))

    PrimaryButton(text = "Perbarui Pratinjau", onClick = onPerbaruiPratinjau)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun GeneratingPdfScreen(
    uiState: CounselorSummaryUiState,
    onSupportClick: () -> Unit
) {
    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    PeriodBadgeChip("Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}")
    Spacer(modifier = Modifier.height(24.dp))

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            color = PineGreen,
            strokeWidth = 3.dp,
            modifier = Modifier.size(48.dp)
        )
    }
    Spacer(modifier = Modifier.height(24.dp))

    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Sedang menyiapkan dokumen PDF...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Berkas PDF disusun secara lokal di perangkatmu dan tidak dikirim ke server mana pun.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    DisabledCustomButton(text = "Menyimpan Dokumen...")

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun PdfErrorScreen(
    uiState: CounselorSummaryUiState,
    onRetryExport: () -> Unit,
    onBackToPreview: () -> Unit,
    onSupportClick: () -> Unit
) {
    WarningCardContainer(
        title = "Gagal Menyiapkan PDF",
        message = uiState.errorMessage ?: "Terjadi kendala saat menulis berkas PDF ke penyimpanan lokal. Silakan coba kembali."
    )
    Spacer(modifier = Modifier.height(16.dp))

    PrimaryButton(text = "Coba Lagi", onClick = onRetryExport)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedCustomButton(text = "Kembali ke Pratinjau", onClick = onBackToPreview)
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun ExportSuccessScreen(
    uiState: CounselorSummaryUiState,
    onSimpanPdf: () -> Unit,
    onPilihCaraBerbagi: () -> Unit,
    onKembaliKeRiwayat: () -> Unit,
    onSupportClick: () -> Unit
) {
    CardItemContainer(containerColor = LightSageGreen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PineGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Dokumen Ringkasan Siap",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tersimpan aman di penyimpanan lokal ponselmu",
                    fontSize = 12.sp,
                    color = MutedGreenText
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(14.dp))
    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    PeriodBadgeChip("Periode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}")

    Spacer(modifier = Modifier.height(20.dp))
    PrimaryButton(text = "Simpan ke File", onClick = onSimpanPdf)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedCustomButton(text = "Bagikan Berkas (Kirim ke Konselor)", onClick = onPilihCaraBerbagi)

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Aplikasi tidak mengunggah berkas secara otomatis. Pastikan kamu memeriksa tujuan pengiriman sebelum membagikan dokumen.",
        fontSize = 11.sp,
        color = MutedGreenText,
        lineHeight = 16.sp
    )

    Spacer(modifier = Modifier.height(14.dp))
    OutlinedCustomButton(text = "Kembali ke Beranda", onClick = onKembaliKeRiwayat)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun CounselorHeader(
    title: String,
    onBack: () -> Unit,
    onSupportClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Kembali",
                    tint = DarkGreenText
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGreenText
            )
        }

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(LightSageGreen)
                .clickable { onSupportClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = SelarasIcons.SupportAgent,
                contentDescription = "Bantuan Konseling",
                tint = PineGreen,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun PeriodBadgeChip(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LightSageGreen)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkGreenText,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun WeeklyDaysRecordedCard(
    recordedDays: Int,
    totalDays: Int,
    recordedDaysText: String,
    missingDaysText: String,
    isEligible: Boolean
) {
    CardItemContainer(containerColor = LavenderCard) {
        Text(
            text = "$recordedDays dari $totalDays Hari Tercatat",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (isEligible) {
                "Minimal 4 hari pengisian sudah terpenuhi untuk membuat ringkasan mingguan."
            } else {
                "Dibutuhkan minimal 4 hari check-in yang berbeda untuk membentuk pola mingguan."
            },
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Row of 7 indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val days = listOf(1, 2, 3, 4, 5, 6, 7)
            val activeCheckIndex = recordedDays

            for (i in days.indices) {
                val isChecked = i < activeCheckIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isChecked) PineGreen else Color.White)
                            .border(1.dp, if (isChecked) PineGreen else Color(0xFFC7D3CD), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "H-${days[i]}", fontSize = 10.sp, color = DarkGreenText)
                }
            }
        }
    }
}

/**
 * Clean selectable card focused on checkbox and clear title with an optional Info "i" icon button.
 */
@Composable
private fun CleanSelectableItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    badgeText: String? = null,
    onInfoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = PineGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkGreenText
                )
                if (badgeText != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        color = MutedGreenText
                    )
                }
            }
            if (onInfoClick != null) {
                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = SelarasIcons.Info,
                        contentDescription = "Informasi detail",
                        tint = PineGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean Popup Information Dialog for screening details and personal notes.
 */
@Composable
private fun SelarasInfoDialog(
    title: String,
    subtitle: String? = null,
    details: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PineGreen,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Tutup", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
        title = {
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreenText
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MutedGreenText
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                details.forEach { (label, value) ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PineGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = value,
                        fontSize = 13.sp,
                        color = DarkGreenText,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun DocumentInfoCard(uiState: CounselorSummaryUiState) {
    val periodFmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("id"))
    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Informasi Dokumen",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        val scCount = uiState.availableScreenings.count { it.isSelected }
        val screeningsOption = if (scCount > 0) "$scCount hasil skrining disertakan" else "Tidak menyertakan hasil skrining"
        val notesCount = uiState.availableNotes.count { it.isSelected }
        val noteOption = if (uiState.includePersonalNotes && notesCount > 0) "$notesCount catatan refleksi disertakan" else "Catatan pribadi tidak disertakan"

        Text(
            text = "Waktu Pembuatan: ${uiState.createdTimeString.ifEmpty { "Terkini" }}\nPeriode Observasi: ${uiState.periodStartDate.format(periodFmt)} – ${uiState.periodEndDate.format(periodFmt)}\nData Check-in: ${uiState.recordedDays} hari valid\nLampiran Skrining: $screeningsOption\nLampiran Catatan: $noteOption\nIsi dokumen PDF sama persis dengan pratinjau ini.",
            fontSize = 11.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun WarningCardContainer(title: String, message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WarningPink)
            .border(1.dp, WarningBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = message, fontSize = 12.sp, color = MutedGreenText, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun CardItemContainer(
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = MutedGreenText)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PineGreen)
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PineGreen,
            contentColor = Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OutlinedCustomButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = PineGreen
        ),
        border = ButtonDefaults.outlinedButtonBorder(enabled = true),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DisabledCustomButton(text: String) {
    Button(
        onClick = {},
        enabled = false,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            disabledContainerColor = DisabledContainer,
            disabledContentColor = DisabledContent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun sharePdf(context: Context, file: File?) {
    if (file == null || !file.exists()) {
        Toast.makeText(context, "Dokumen PDF sedang disiapkan", Toast.LENGTH_SHORT).show()
        return
    }

    try {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Ringkasan Konselor"))
    } catch (e: Exception) {
        Toast.makeText(context, "Tidak dapat membagikan dokumen: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
