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
import androidx.compose.runtime.remember
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
import java.io.File
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
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

    if (!uiState.isSimulatedMode) {
        LiveCounselorSummaryContent(
            uiState = uiState,
            viewModel = effectiveViewModel,
            context = context,
            onNavigateBack = onNavigateBack,
            modifier = modifier
        )
        return
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
            onSupportClick = {
                Toast.makeText(context, "Layanan Bantuan: Silakan hubungi konselor atau psikolog profesional Anda.", Toast.LENGTH_SHORT).show()
            }
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
                        onToggleManual = { effectiveViewModel.toggleScreeningManual(it) },
                        onToggleRoutine = { effectiveViewModel.toggleScreeningRoutine(it) },
                        onToggleIncludeNotes = { effectiveViewModel.toggleIncludeNotes(it) },
                        onToggleNoteN09 = { effectiveViewModel.toggleNoteN09(it) },
                        onToggleNoteN11 = { effectiveViewModel.toggleNoteN11(it) },
                        onCreateSummary = { effectiveViewModel.createSummary() },
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.INELIGIBLE -> {
                    IneligibleScreen(
                        uiState = uiState,
                        onIsiCheckIn = onNavigateBack,
                        onLihatRiwayat = onNavigateBack,
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.PREPARING -> {
                    PreparingScreen(
                        uiState = uiState,
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.PREVIEW -> {
                    PreviewScreen(
                        uiState = uiState,
                        onExportPdf = { effectiveViewModel.exportPdf(context) },
                        onUbahPilihan = { effectiveViewModel.backToSelectContent() },
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.SOURCE_CHANGED -> {
                    SourceChangedScreen(
                        uiState = uiState,
                        onPerbaruiPratinjau = { effectiveViewModel.refreshPreview() },
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.GENERATING_PDF -> {
                    GeneratingPdfScreen(
                        uiState = uiState,
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                CounselorScreenState.PDF_ERROR -> {
                    PdfErrorScreen(
                        uiState = uiState,
                        onRetryExport = { effectiveViewModel.retryExport(context) },
                        onBackToPreview = { effectiveViewModel.backToPreview() },
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
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
                        onSupportClick = {
                            Toast.makeText(context, "Layanan bantuan konseling siap dihubungi", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SelectContentScreen(
    uiState: CounselorSummaryUiState,
    onToggleManual: (Boolean) -> Unit,
    onToggleRoutine: (Boolean) -> Unit,
    onToggleIncludeNotes: (Boolean) -> Unit,
    onToggleNoteN09: (Boolean) -> Unit,
    onToggleNoteN11: (Boolean) -> Unit,
    onCreateSummary: () -> Unit,
    onSupportClick: () -> Unit
) {
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
    Spacer(modifier = Modifier.height(14.dp))

    WeeklyDaysRecordedCard(
        recordedDays = uiState.recordedDays,
        totalDays = uiState.totalDays,
        recordedDaysText = "Tercatat: 7, 9, 11, dan 13 Oktober",
        missingDaysText = "Belum tercatat: 8, 10, dan 12 Oktober",
        isEligible = true
    )

    Spacer(modifier = Modifier.height(22.dp))
    Text(
        text = "Pilih data yang ingin kamu bawa",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(12.dp))

    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Catatan Check-in Harian · Utama",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Analisis suasana hati, durasi tidur, dan energi dari 4 hari tercatat akan menjadi dasar utama ringkasan mingguanmu.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 18.sp
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Hasil Skrining Berkala · Opsional",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "Pilih hasil skrining yang ingin kamu tunjukkan ke konselor. Kamu juga bisa membuat ringkasan tanpa hasil skrining.",
        fontSize = 12.sp,
        color = MutedGreenText,
        lineHeight = 17.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    CheckboxCard(
        checked = uiState.selectScreeningManual,
        onCheckedChange = onToggleManual,
        title = "Skrining Mandiri · 9 Oktober 2026",
        details = "Kuesioner Kesehatan PHQ-9\nSkor: 10 (Gejala Sedang)\nDiselesaikan pada sesi refleksi mandiri"
    )
    Spacer(modifier = Modifier.height(10.dp))

    CheckboxCard(
        checked = uiState.selectScreeningRoutine,
        onCheckedChange = onToggleRoutine,
        title = "Skrining Berkala · 11 Oktober 2026",
        details = "Kuesioner Kesehatan PHQ-9\nSkor: 8 (Gejala Ringan)\nDiselesaikan pada evaluasi dua mingguan"
    )

    Spacer(modifier = Modifier.height(20.dp))
    Text(
        text = "Catatan Pribadi · Opsional",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = DarkGreenText
    )
    Spacer(modifier = Modifier.height(10.dp))

    CheckboxCard(
        checked = uiState.includePersonalNotes,
        onCheckedChange = onToggleIncludeNotes,
        title = "Sertakan Catatan Refleksi Pribadi",
        details = "Secara bawaan dinonaktifkan untuk menjaga privasimu. Aktifkan jika ada catatan khusus yang ingin didiskusikan."
    )

    if (uiState.includePersonalNotes) {
        Spacer(modifier = Modifier.height(10.dp))
        Column(modifier = Modifier.padding(start = 16.dp)) {
            CheckboxCard(
                checked = uiState.selectNoteN09,
                onCheckedChange = onToggleNoteN09,
                title = "Catatan 9 Oktober 2026",
                details = "\"Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan.\""
            )
            Spacer(modifier = Modifier.height(8.dp))
            CheckboxCard(
                checked = uiState.selectNoteN11,
                onCheckedChange = onToggleNoteN11,
                title = "Catatan 11 Oktober 2026",
                details = "\"Latihan pernapasan membantu saya lebih tenang di malam hari.\""
            )
        }
    }
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = "Catatan tidak dianalisis otomatis. Hanya catatan yang kamu pilih yang akan dimasukkan ke dokumen.",
        fontSize = 11.sp,
        color = MutedGreenText
    )

    Spacer(modifier = Modifier.height(20.dp))
    CardItemContainer(containerColor = LavenderCard) {
        Text(
            text = "Kamu Tetap Memegang Kendali Penuh",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Kamu bisa meninjau seluruh isi pratinjau sebelum menyimpannya ke PDF. Aplikasi tidak pernah mengirim data ke konselor secara otomatis.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(22.dp))
    PrimaryButton(text = "Buat Ringkasan Mingguan", onClick = onCreateSummary)

    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = "Membuat ringkasan tidak memerlukan koneksi internet dan tidak mengurangi kuota skrining berkala.",
        fontSize = 11.sp,
        color = MutedGreenText,
        lineHeight = 16.sp
    )
    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun IneligibleScreen(
    uiState: CounselorSummaryUiState,
    onIsiCheckIn: () -> Unit,
    onLihatRiwayat: () -> Unit,
    onSupportClick: () -> Unit
) {
    WarningCardContainer(
        title = "Data Belum Mencukupi",
        message = "Dibutuhkan minimal 4 hari catatan check-in dalam satu minggu agar ringkasan memiliki pola yang bermakna untuk konselor. Saat ini baru tercatat 3 hari."
    )
    Spacer(modifier = Modifier.height(14.dp))

    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
    Spacer(modifier = Modifier.height(12.dp))

    WeeklyDaysRecordedCard(
        recordedDays = 3,
        totalDays = 7,
        recordedDaysText = "Tercatat: 7, 9, dan 11 Oktober",
        missingDaysText = "Belum tercatat: 8, 10, 12, dan 13 Oktober",
        isEligible = false
    )

    Spacer(modifier = Modifier.height(20.dp))
    DisabledCustomButton(text = "Buat Ringkasan Mingguan")

    Spacer(modifier = Modifier.height(10.dp))
    Text(
        text = "Lengkapi 1 hari check-in lagi untuk membuka pratinjau dan ekspor dokumen PDF.",
        fontSize = 11.sp,
        color = MutedGreenText
    )

    Spacer(modifier = Modifier.height(16.dp))
    PrimaryButton(text = "Isi Check-in Hari Ini", onClick = onIsiCheckIn)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedCustomButton(text = "Lihat Riwayat & Hasil Skrining", onClick = onLihatRiwayat)

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
            text = "Hasil skrining PHQ-9 tetap tersimpan aman di riwayatmu, namun ringkasan berkala membutuhkan rekaman suasana hati harian yang konsisten.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun PreparingScreen(
    uiState: CounselorSummaryUiState,
    onSupportClick: () -> Unit
) {
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
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
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
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
        Text(
            text = "Tercatat pada tanggal: 7, 9, 11, dan 13 Oktober 2026",
            fontSize = 11.sp,
            color = MutedGreenText
        )
        Spacer(modifier = Modifier.height(14.dp))

        MetricRow("Rata-rata Suasana Hati", "2,0 / 5")
        MetricRow("Rata-rata Durasi Tidur", "6,0 jam")
        MetricRow("Rata-rata Tingkat Energi", "2,5 / 5")

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Rata-rata dihitung dari 4 hari yang kamu isi. Tanggal tanpa pengisian tidak dianggap sebagai nilai nol.",
            fontSize = 11.sp,
            color = MutedGreenText
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Rincian Harian (Mood / Tidur / Energi):\n• 7 Okt: 2/5 · 6 jam · 2/5\n• 9 Okt: 3/5 · 7 jam · 3/5\n• 11 Okt: 2/5 · 6 jam · 3/5\n• 13 Okt: 1/5 · 5 jam · 2/5",
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

    if (!uiState.selectScreeningManual && !uiState.selectScreeningRoutine && uiState.snapshotId != "S-02") {
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
        CardItemContainer(containerColor = Color.White) {
            Text(text = "Skrining Mandiri · 9 Oktober 2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Instrumen: Kuesioner PHQ-9 (9 Pertanyaan)\nSkor Total: 10\nKategori: Gejala Depresi Tingkat Sedang\nDiselesaikan pada sesi refleksi mandiri", fontSize = 11.sp, color = MutedGreenText, lineHeight = 17.sp)
        }
        Spacer(modifier = Modifier.height(8.dp))
        CardItemContainer(containerColor = Color.White) {
            Text(text = "Skrining Berkala · 11 Oktober 2026", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Instrumen: Kuesioner PHQ-9 (9 Pertanyaan)\nSkor Total: 8\nKategori: Gejala Depresi Tingkat Ringan\nDiselesaikan pada evaluasi berkala", fontSize = 11.sp, color = MutedGreenText, lineHeight = 17.sp)
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

    if (!uiState.includePersonalNotes && uiState.snapshotId != "S-02") {
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
        CardItemContainer(containerColor = Color.White) {
            Text(text = "Catatan Refleksi · 9 Oktober 2026", fontSize = 11.sp, color = MutedGreenText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "\"Hari ini terasa berat. Saya ingin membicarakan cara meminta dukungan.\"", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = DarkGreenText)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Hanya catatan yang kamu setujui yang akan muncul di dokumen.", fontSize = 11.sp, color = MutedGreenText)
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
    Spacer(modifier = Modifier.height(10.dp))
    DisabledCustomButton(text = "Ekspor ke File PDF")

    Spacer(modifier = Modifier.height(16.dp))
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
    Spacer(modifier = Modifier.height(12.dp))
    DocumentInfoCard(uiState = uiState)

    Spacer(modifier = Modifier.height(16.dp))
    OutlinedCustomButton(text = "Bantuan & Kontak Konselor", onClick = onSupportClick)
}

@Composable
private fun GeneratingPdfScreen(
    uiState: CounselorSummaryUiState,
    onSupportClick: () -> Unit
) {
    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Sedang Membuat Berkas PDF...",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Dokumen PDF sedang disusun berdasarkan data pratinjau yang telah kamu setujui.",
            fontSize = 12.sp,
            color = MutedGreenText
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    DisabledCustomButton(text = "Memproses Dokumen...")

    Spacer(modifier = Modifier.height(16.dp))
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
    Spacer(modifier = Modifier.height(12.dp))
    DocumentInfoCard(uiState = uiState)

    Spacer(modifier = Modifier.height(18.dp))
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
        message = "Terjadi kendala saat membuat dokumen PDF di penyimpanan perangkat. Data pratinjaumu tetap aman, silakan coba kembali."
    )
    Spacer(modifier = Modifier.height(14.dp))

    PrimaryButton(text = "Coba Buat PDF Lagi", onClick = onRetryExport)
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedCustomButton(text = "Kembali ke Pratinjau", onClick = onBackToPreview)

    Spacer(modifier = Modifier.height(16.dp))
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")
    Spacer(modifier = Modifier.height(12.dp))
    DocumentInfoCard(uiState = uiState)

    Spacer(modifier = Modifier.height(18.dp))
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LightSageGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = PineGreen,
                modifier = Modifier.size(36.dp)
            )
        }
    }

    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Dokumen PDF Siap Disimpan",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Dokumen belum dikirim ke siapa pun. Kamu memiliki kendali penuh untuk menentukan kapan dan kepada siapa berkas ini dibagikan.",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 17.sp
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "Serasi · Ringkasan Mingguan.pdf",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Berkas tersimpan di folder lokal perangkat. Format dokumen sudah rapi dan siap dicetak atau dikirim ke konselor.",
            fontSize = 12.sp,
            color = MutedGreenText
        )
    }

    Spacer(modifier = Modifier.height(12.dp))
    PeriodBadgeChip("Periode Observasi: 7–13 Oktober 2026")

    Spacer(modifier = Modifier.height(12.dp))
    CardItemContainer(containerColor = Color.White) {
        Text(
            text = "Ringkasan Isi Dokumen",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "• 4 hari check-in suasana hati & tidur\n• Rata-rata mood 2,0/5 · tidur 6 jam · energi 2,5/5\n• Rekapitulasi skrining & catatan pilihan\n• Pernyataan mandiri non-diagnosis klinis",
            fontSize = 12.sp,
            color = MutedGreenText,
            lineHeight = 20.sp
        )
    }

    Spacer(modifier = Modifier.height(20.dp))
    PrimaryButton(text = "Simpan Dokumen ke Perangkat", onClick = onSimpanPdf)
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
            color = DarkGreenText
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
            color = MutedGreenText
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val days = listOf(7, 8, 9, 10, 11, 12, 13)
            val isChecked = if (isEligible) {
                listOf(true, false, true, false, true, false, true)
            } else {
                listOf(true, false, true, false, true, false, false)
            }

            for (i in days.indices) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isChecked[i]) PineGreen else Color.White)
                            .border(1.dp, if (isChecked[i]) PineGreen else Color(0xFFC7D3CD), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isChecked[i]) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = days[i].toString(), fontSize = 11.sp, color = DarkGreenText)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = recordedDaysText, fontSize = 11.sp, color = MutedGreenText)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = missingDaysText, fontSize = 11.sp, color = MutedGreenText)
    }
}

@Composable
private fun DocumentInfoCard(uiState: CounselorSummaryUiState) {
    CardItemContainer(containerColor = LightSageGreen) {
        Text(
            text = "Informasi Dokumen",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = DarkGreenText
        )
        Spacer(modifier = Modifier.height(4.dp))
        val screeningsOption = if (uiState.selectScreeningManual && uiState.selectScreeningRoutine) {
            "Skrining Mandiri & Berkala disertakan"
        } else if (uiState.selectScreeningManual) {
            "Skrining Mandiri disertakan"
        } else if (uiState.selectScreeningRoutine) {
            "Skrining Berkala disertakan"
        } else {
            "Tidak menyertakan hasil skrining"
        }
        val noteOption = if (uiState.includePersonalNotes) "Catatan refleksi disertakan" else "Catatan pribadi tidak disertakan"

        Text(
            text = "Waktu Pembuatan: 13 Oktober 2026 · 14.30 WIB\nPeriode Observasi: 7–13 Oktober 2026\nData Check-in: 4 hari valid (7, 9, 11, 13 Oktober)\nLampiran Skrining: $screeningsOption\nLampiran Catatan: $noteOption\nIsi dokumen PDF sama persis dengan pratinjau ini.",
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
private fun CheckboxCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    details: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(checkedColor = PineGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = details, fontSize = 11.sp, color = MutedGreenText, lineHeight = 16.sp)
            }
        }
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


@Composable
private fun LiveCounselorSummaryContent(
    uiState: CounselorSummaryUiState,
    viewModel: CounselorSummaryViewModel,
    context: Context,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    val isSummarySelection = uiState.screenState == CounselorScreenState.SELECT_CONTENT ||
        uiState.screenState == CounselorScreenState.INELIGIBLE
    Column(modifier = modifier.fillMaxSize().background(CanvasBackground)) {
        CounselorHeader(
            title = when (uiState.screenState) {
                CounselorScreenState.SELECT_CONTENT, CounselorScreenState.INELIGIBLE -> "Ringkasan untuk Konselor"
                CounselorScreenState.EXPORT_SUCCESS -> "Dokumen Siap"
                CounselorScreenState.GENERATING_PDF -> "Membuat PDF"
                else -> "Pratinjau Ringkasan"
            },
            onBack = {
                if (isSummarySelection) onNavigateBack()
                else if (uiState.screenState == CounselorScreenState.PREVIEW) viewModel.backToSelectContent()
                else viewModel.backToPreview()
            },
            onSupportClick = {
                Toast.makeText(context, "Hubungi konselor atau tenaga kesehatan jika membutuhkan bantuan.", Toast.LENGTH_SHORT).show()
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(10.dp))
            PeriodBadgeChip(
                "Periode: ${formatCounselorDate(uiState.periodStartDate)} - ${formatCounselorDate(uiState.periodEndDate)}"
            )
            Spacer(Modifier.height(16.dp))
            when (uiState.screenState) {
                CounselorScreenState.SELECT_CONTENT, CounselorScreenState.INELIGIBLE -> {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = PineGreen)
                        Spacer(Modifier.height(12.dp))
                        Text("Membaca catatan lokal...", color = MutedGreenText)
                    } else {
                        LiveSelectCounselorContent(uiState, viewModel)
                    }
                }
                CounselorScreenState.PREPARING, CounselorScreenState.GENERATING_PDF -> {
                    CardItemContainer(containerColor = LightSageGreen) {
                        CircularProgressIndicator(color = PineGreen, modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (uiState.screenState == CounselorScreenState.PREPARING)
                                "Menyusun ringkasan dari data asli di perangkat..."
                            else "Menyiapkan berkas PDF dari ringkasan tersimpan...",
                            color = DarkGreenText, fontSize = 14.sp
                        )
                    }
                }
                CounselorScreenState.PREVIEW, CounselorScreenState.SOURCE_CHANGED -> {
                    LiveCounselorPreview(uiState)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Ekspor ke PDF") { viewModel.exportPdf(context) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedCustomButton("Ubah pilihan data") { viewModel.backToSelectContent() }
                }
                CounselorScreenState.PDF_ERROR -> {
                    WarningCardContainer(
                        "PDF belum berhasil dibuat",
                        uiState.errorMessage ?: "Penyimpanan PDF gagal. Data ringkasan tidak hilang."
                    )
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("Coba buat PDF lagi") { viewModel.retryExport(context) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedCustomButton("Kembali ke pratinjau") { viewModel.backToPreview() }
                }
                CounselorScreenState.EXPORT_SUCCESS -> {
                    CardItemContainer(containerColor = LightSageGreen) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = PineGreen)
                        Spacer(Modifier.height(10.dp))
                        Text("PDF berhasil dibuat di penyimpanan lokal aplikasi.", color = DarkGreenText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(uiState.generatedPdfFile?.name.orEmpty(), fontSize = 12.sp, color = MutedGreenText)
                        Text("Berkas tidak dibagikan secara otomatis.", color = MutedGreenText, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("Bagikan PDF") { sharePdf(context, uiState.generatedPdfFile) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedCustomButton("Kembali") { onNavigateBack() }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun LiveSelectCounselorContent(
    uiState: CounselorSummaryUiState,
    viewModel: CounselorSummaryViewModel
) {
    uiState.errorMessage?.let {
        WarningCardContainer("Ada kendala", it)
        Spacer(Modifier.height(14.dp))
    }
    CardItemContainer(containerColor = LightSageGreen) {
        Text("Check-in minggu ini", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(6.dp))
        Text("${uiState.recordedDays} dari ${uiState.totalDays} hari tercatat", color = PineGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (uiState.recordedDays >= 4) "Syarat minimal 4 tanggal check-in sudah terpenuhi."
            else "Ringkasan PDF baru dapat dibuat setelah minimal 4 tanggal check-in berbeda dalam minggu ini. Hasil skrining tetap tersimpan walaupun syarat ini belum terpenuhi.",
            fontSize = 12.sp, lineHeight = 18.sp, color = MutedGreenText
        )
    }
    Spacer(Modifier.height(14.dp))
    if (uiState.handoffResultId != null) {
        CardItemContainer(containerColor = LavenderCard) {
            Text("Hasil skrining #${uiState.handoffResultId} telah diterima dari F1.", color = DarkGreenText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            Text("Hasil ini dipilih secara otomatis, tetapi dapat dilepas dari ringkasan.", color = MutedGreenText, fontSize = 12.sp)
        }
        Spacer(Modifier.height(14.dp))
    }
    Text("Hasil skrining · opsional", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
    Spacer(Modifier.height(8.dp))
    val manual = uiState.availableManualScreening
    if (manual == null) {
        Text("Belum ada hasil skrining Manual.", color = MutedGreenText, fontSize = 12.sp)
    } else {
        LiveScreeningCheckbox(manual, uiState.selectScreeningManual, viewModel::toggleScreeningManual)
    }
    Spacer(Modifier.height(8.dp))
    val routine = uiState.availableRoutineScreening
    if (routine == null) {
        Text("Belum ada hasil skrining Rutin.", color = MutedGreenText, fontSize = 12.sp)
    } else {
        LiveScreeningCheckbox(routine, uiState.selectScreeningRoutine, viewModel::toggleScreeningRoutine)
    }
    Spacer(Modifier.height(18.dp))
    val notes = uiState.dailyItems.filter { it.isRecorded && !it.note.isNullOrBlank() }
    Text("Catatan pribadi · opsional", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DarkGreenText)
    Spacer(Modifier.height(8.dp))
    CheckboxCard(
        checked = uiState.includePersonalNotes,
        onCheckedChange = viewModel::toggleIncludeNotes,
        title = "Sertakan catatan pribadi",
        details = "Catatan hanya masuk dokumen jika kamu memilih tanggalnya."
    )
    if (uiState.includePersonalNotes) {
        if (notes.isEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text("Belum ada catatan pribadi yang bisa dipilih.", color = MutedGreenText, fontSize = 12.sp)
        }
        notes.forEach { item ->
            Spacer(Modifier.height(8.dp))
            CheckboxCard(
                checked = item.date in uiState.selectedNoteDates,
                onCheckedChange = { viewModel.toggleNoteDate(item.date, it) },
                title = formatCounselorDate(item.date),
                details = item.note.orEmpty()
            )
        }
    }
    Spacer(Modifier.height(18.dp))
    CardItemContainer(containerColor = LavenderCard) {
        Text("Privasi data", color = DarkGreenText, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Ringkasan dibuat di perangkat. Kamu dapat memeriksa isinya sebelum berbagi. Hasil demo bukan instrumen klinis.", color = MutedGreenText, fontSize = 12.sp, lineHeight = 18.sp)
    }
    Spacer(Modifier.height(16.dp))
    Button(
        onClick = viewModel::createSummary,
        enabled = uiState.recordedDays >= 4 && !uiState.isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = PineGreen),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Text("Buat Ringkasan Mingguan", color = Color.White)
    }
    Spacer(Modifier.height(10.dp))
    OutlinedCustomButton("Muat ulang data") { viewModel.refreshData() }
}

@Composable
private fun LiveScreeningCheckbox(
    result: ScreeningResult,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val demo = result.instrumentVersion.startsWith("DEMO-", ignoreCase = true)
    CheckboxCard(
        checked = checked,
        onCheckedChange = onCheckedChange,
        title = "${if (result.screeningType == ScreeningType.MANUAL) "Manual" else "Rutin"} · ${formatCounselorDate(result.completedAt.atZone(ZoneId.systemDefault()).toLocalDate())}",
        details = "ID #${result.id} · Skor ${result.totalScore}/27\n${if (demo) "Refleksi demo, bukan PHQ-9 dan tidak memiliki makna klinis" else "Skrining awal, bukan diagnosis"}"
    )
}

@Composable
private fun LiveCounselorPreview(uiState: CounselorSummaryUiState) {
    val summary = uiState.previewSummary
    if (summary == null) {
        WarningCardContainer("Ringkasan belum tersedia", "Kembali ke pilihan data dan buat ringkasan terlebih dahulu.")
        return
    }
    CardItemContainer(containerColor = LightSageGreen) {
        Text("Pratinjau ringkasan tersimpan", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(5.dp))
        Text("ID ringkasan: ${summary.id} · Dibuat ${uiState.createdTimeString}", color = MutedGreenText, fontSize = 12.sp)
        Text("Data diambil dari Room dan dibekukan sebagai snapshot saat ringkasan dibuat.", color = MutedGreenText, fontSize = 12.sp)
    }
    Spacer(Modifier.height(16.dp))
    CardItemContainer(containerColor = Color.White) {
        Text("01 · Analisis Check-in", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(10.dp))
        MetricRow("Hari tercatat", "${summary.checkinDaysCount} hari")
        MetricRow("Rata-rata mood", String.format(Locale.forLanguageTag("id"), "%.1f / 5", uiState.avgMood))
        MetricRow("Rata-rata tidur", String.format(Locale.forLanguageTag("id"), "%.1f jam", uiState.avgSleepHours))
        MetricRow("Rata-rata energi", String.format(Locale.forLanguageTag("id"), "%.1f / 10", uiState.avgEnergy))
        Spacer(Modifier.height(8.dp))
        uiState.dailyItems.filter { it.isRecorded }.forEach { item ->
            Text(
                "${formatCounselorDate(item.date)} · mood ${item.mood}/5 · tidur ${item.sleepHours} jam · energi ${item.energy}/10",
                color = MutedGreenText, fontSize = 12.sp, lineHeight = 18.sp
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    CardItemContainer(containerColor = Color.White) {
        Text("02 · Hasil skrining terpilih", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(8.dp))
        if (uiState.previewScreenings.isEmpty()) {
            Text("Tidak ada hasil skrining yang dimasukkan.", color = MutedGreenText, fontSize = 12.sp)
        } else {
            uiState.previewScreenings.forEach { result ->
                Text(
                    "ID #${result.id} · ${if (result.screeningType == ScreeningType.MANUAL) "Manual" else "Rutin"} · skor ${result.totalScore}/27",
                    color = DarkGreenText, fontSize = 13.sp
                )
                Text(
                    if (result.instrumentVersion.startsWith("DEMO-", true)) "Sesi demo: tidak bermakna klinis" else "${result.instrumentVersion} · bukan diagnosis",
                    color = MutedGreenText, fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    CardItemContainer(containerColor = Color.White) {
        Text("03 · Catatan pribadi", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(8.dp))
        val selectedNotes = if (uiState.includePersonalNotes) uiState.dailyItems.filter { it.date in uiState.selectedNoteDates } else emptyList()
        if (selectedNotes.isEmpty()) {
            Text("Tidak menyertakan catatan pribadi.", color = MutedGreenText, fontSize = 12.sp)
        } else {
            selectedNotes.forEach { item ->
                Text("${formatCounselorDate(item.date)}: ${item.note.orEmpty()}", fontSize = 12.sp, color = MutedGreenText, lineHeight = 18.sp)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    CardItemContainer(containerColor = LavenderCard) {
        Text("Bukan diagnosis medis", fontWeight = FontWeight.Bold, color = DarkGreenText)
        Spacer(Modifier.height(4.dp))
        Text("Hasil refleksi demo hanya digunakan untuk demonstrasi alur F1-F4. Tidak boleh ditafsirkan secara klinis.", color = MutedGreenText, fontSize = 12.sp)
    }
}

private fun formatCounselorDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("id")))
