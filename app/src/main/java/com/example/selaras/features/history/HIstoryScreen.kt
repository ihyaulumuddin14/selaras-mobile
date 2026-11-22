package com.example.selaras.features.history

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
import androidx.compose.material3.Icon
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
import com.example.selaras.SelarasApplication
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.core.ui.components.SelarasIcons
import com.example.selaras.domain.model.ScreeningResult
import com.example.selaras.domain.model.ScreeningType
import com.example.selaras.domain.repository.ScreeningRepository
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.flowOf

private val HistoryPine = Color(0xFF356859)
private val HistoryText = Color(0xFF243B35)
private val HistoryMuted = Color(0xFF52675F)
private val HistoryBackground = Color(0xFFF7F7F2)
private val HistorySage = Color(0xFFDCECE3)
private val HistoryBorder = Color(0xFFE2E8E3)

@Composable
fun HistoryScreen(
    onNavigateToCheckInHistory: () -> Unit = {},
    onNavigateToCounselorPdf: () -> Unit = {},
    modifier: Modifier = Modifier,
    screeningRepository: ScreeningRepository? = null
) {
    val context = LocalContext.current
    val repository = remember(screeningRepository, context) {
        screeningRepository ?: (context.applicationContext as? SelarasApplication)?.container?.screeningRepository
    }
    val resultFlow = remember(repository) {
        repository?.getAllScreeningResultsFlow() ?: flowOf(emptyList())
    }
    val results by resultFlow.collectAsState(initial = emptyList())
    var showScreeningHistory by remember { mutableStateOf(false) }
    var selectedResultId by remember { mutableStateOf<Long?>(null) }
    var selectedResult by remember { mutableStateOf<ScreeningResult?>(null) }
    var loadingResult by remember { mutableStateOf(false) }
    var readError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(repository, selectedResultId) {
        selectedResult = null
        readError = null
        val id = selectedResultId ?: return@LaunchedEffect
        if (repository == null) {
            readError = "Database belum tersedia."
            return@LaunchedEffect
        }
        loadingResult = true
        try {
            selectedResult = repository.getScreeningResultById(id)
            if (selectedResult == null) readError = "Hasil tidak dapat ditemukan."
        } catch (exception: Exception) {
            readError = exception.message ?: "Gagal membaca hasil skrining."
        } finally {
            loadingResult = false
        }
    }

    Column(modifier = modifier.fillMaxSize().background(HistoryBackground)) {
        SelarasHeader(title = if (showScreeningHistory) "Riwayat Skrining" else "Riwayat Catatan")
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            if (!showScreeningHistory) {
                Text(
                    "Lihat rekapitulasi suasana hati, catatan harian, hasil refleksi, dan ringkasan konselor.",
                    fontSize = 14.sp, color = HistoryMuted, lineHeight = 20.sp
                )
                Spacer(Modifier.height(20.dp))
                HistoryMenuCard(
                    title = "Riwayat Check-in & Pola",
                    subtitle = "Pantau perkembangan mood, energi, dan catatan harian.",
                    icon = SelarasIcons.EditNote,
                    onClick = onNavigateToCheckInHistory
                )
                Spacer(Modifier.height(12.dp))
                HistoryMenuCard(
                    title = "Dokumen Ringkasan PDF",
                    subtitle = "Buat ringkasan yang dapat dibawa saat konsultasi.",
                    icon = SelarasIcons.Description,
                    onClick = onNavigateToCounselorPdf
                )
                Spacer(Modifier.height(12.dp))
                HistoryMenuCard(
                    title = "Riwayat Hasil Skrining",
                    subtitle = "Lihat hasil skrining dan sesi refleksi yang tersimpan di perangkat.",
                    icon = SelarasIcons.FactCheck,
                    onClick = { showScreeningHistory = true }
                )
            } else if (selectedResultId != null) {
                OutlinedButton(onClick = { selectedResultId = null }) {
                    Text("Kembali ke daftar")
                }
                Spacer(Modifier.height(12.dp))
                when {
                    loadingResult -> CircularProgressIndicator(color = HistoryPine)
                    readError != null -> Text(readError.orEmpty(), color = Color(0xFFB3261E))
                    selectedResult != null -> HistoryResultDetail(selectedResult!!)
                }
            } else {
                OutlinedButton(onClick = { showScreeningHistory = false }) {
                    Text("Kembali ke riwayat")
                }
                Spacer(Modifier.height(16.dp))
                if (repository == null) {
                    Text("Penyimpanan skrining belum dapat diakses.", color = HistoryMuted)
                } else if (results.isEmpty()) {
                    Text(
                        "Belum ada hasil skrining tersimpan. Selesaikan sesi refleksi melalui tab Skrining.",
                        color = HistoryMuted, fontSize = 14.sp, lineHeight = 21.sp
                    )
                } else {
                    results.sortedByDescending { it.completedAt }.forEach { result ->
                        HistoryResultCard(result = result, onClick = { selectedResultId = result.id })
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HistoryResultCard(result: ScreeningResult, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, HistoryBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(
            "${if (result.screeningType == ScreeningType.MANUAL) "Manual" else "Rutin"} · Skrining PHQ-9",
            color = HistoryText, fontWeight = FontWeight.Bold, fontSize = 16.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(result.completedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id"))), fontSize = 12.sp, color = HistoryMuted)
        Spacer(Modifier.height(8.dp))
        Text("Skor ${result.totalScore}/27 · ID #${result.id}", fontSize = 14.sp, color = HistoryPine, fontWeight = FontWeight.SemiBold)
        Text(
            "Hasil penapisan awal untuk pemantauan mandiri.",
            fontSize = 12.sp, color = HistoryMuted
        )
        Spacer(Modifier.height(8.dp))
        Text("Ketuk untuk melihat rincian jawaban", fontSize = 12.sp, color = HistoryPine)
    }
}

@Composable
private fun HistoryResultDetail(result: ScreeningResult) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Hasil Skrining #${result.id}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = HistoryText)
        Spacer(Modifier.height(8.dp))
        Text("Jenis: ${if (result.screeningType == ScreeningType.MANUAL) "Manual" else "Rutin"}", color = HistoryMuted)
        Text("Skor: ${result.totalScore} dari 27", color = HistoryPine, fontWeight = FontWeight.SemiBold)
        Text("Versi: ${result.instrumentVersion}", color = HistoryMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Hasil penapisan awal untuk membantumu merefleksikan kondisi emosional.",
            color = HistoryMuted, fontSize = 13.sp, lineHeight = 19.sp
        )
        Spacer(Modifier.height(16.dp))
        Text("Jawaban yang tersimpan", fontWeight = FontWeight.Bold, color = HistoryText)
        Spacer(Modifier.height(8.dp))
        if (result.answers.isEmpty()) {
            Text("Tidak ada rincian jawaban yang dapat dibaca.", color = HistoryMuted)
        } else {
            val answerLabels = listOf("Tidak pernah", "Beberapa hari", "Lebih dari separuh waktu", "Hampir setiap hari")
            result.answers.sortedBy { it.questionNumber }.forEach { answer ->
                Text(
                    "Pertanyaan ${answer.questionNumber}: ${answerLabels.getOrElse(answer.answerValue) { "Nilai ${answer.answerValue}" }}",
                    color = HistoryText, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun HistoryMenuCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, HistoryBorder, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(HistorySage),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = HistoryPine, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HistoryText)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = HistoryMuted, lineHeight = 17.sp)
            }
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = HistorySage, contentColor = HistoryPine),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Buka", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
