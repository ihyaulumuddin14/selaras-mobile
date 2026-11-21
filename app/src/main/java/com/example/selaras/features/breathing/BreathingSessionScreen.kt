package com.example.selaras.features.breathing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.domain.usecase.breathing.BreathingPhase
import com.example.selaras.domain.usecase.breathing.BreathingRunState
import com.example.selaras.domain.usecase.breathing.BreathingSessionSnapshot

@Composable
internal fun BreathingSessionScreen(
    state: BreathingUiState,
    onBack: () -> Unit,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit
) {
    val pattern = state.sessionPattern ?: return
    val snapshot = state.session
    // Fade the instructions around phase boundaries instead of changing text abruptly.
    val instructionAlpha = if (snapshot.runState == BreathingRunState.RUNNING) {
        (snapshot.phaseProgress / 0.09f).coerceIn(0f, 1f) *
            ((1f - snapshot.phaseProgress) / 0.07f).coerceIn(0f, 1f)
    } else {
        1f
    }
    var showHelp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BreathingHeader("Latihan Relaksasi", onBack = onBack, onHelp = { showHelp = true })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("SELARAS", fontSize = 11.sp, letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold, color = BreathingColors.Primary)
            Spacer(Modifier.height(11.dp))
            Text(
                pattern.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                color = BreathingColors.Text
            )
            Spacer(Modifier.height(7.dp))
            Text(
                "Tarik ${pattern.inhaleSeconds} dtk · Embus ${pattern.exhaleSeconds} dtk",
                color = BreathingColors.Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(37.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BreathingOrb(snapshot = snapshot, modifier = Modifier.size(304.dp))
            }
            Spacer(Modifier.height(30.dp))
            Text(
                titleFor(snapshot),
                modifier = Modifier.fillMaxWidth().alpha(instructionAlpha),
                fontSize = 25.sp,
                color = BreathingColors.Text,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Normal
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "Waktu aktif ${formatActiveTime(snapshot.activeMillis)}",
                modifier = Modifier.fillMaxWidth(),
                color = BreathingColors.Primary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                hintFor(snapshot),
                modifier = Modifier.fillMaxWidth().alpha(instructionAlpha),
                color = BreathingColors.Muted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            BreathingPrimaryButton(
                text = when (snapshot.runState) {
                    BreathingRunState.READY -> "Mulai"
                    BreathingRunState.RUNNING -> "Jeda"
                    BreathingRunState.PAUSED -> "Lanjutkan"
                },
                onClick = if (snapshot.runState == BreathingRunState.RUNNING) onPause
                    else onStartOrResume
            )
            Spacer(Modifier.height(9.dp))
            BreathingSecondaryButton("Hentikan", onClick = onStop)
            Spacer(Modifier.height(6.dp))
            Text(
                "Sesi tidak disimpan. Hentikan atau kembali untuk keluar tanpa riwayat.",
                color = BreathingColors.Muted,
                fontSize = 11.sp
            )
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Ikuti ritmenya") },
            text = {
                Text(
                    "Saat lingkaran membesar, tarik napas secara perlahan. " +
                        "Saat mengecil, embuskan napas. Kamu bebas menjeda " +
                        "atau menghentikan latihan kapan saja."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Mengerti") }
            }
        )
    }
}

private fun titleFor(snapshot: BreathingSessionSnapshot): String =
    when (snapshot.runState) {
        BreathingRunState.READY -> "Mulai saat kamu siap"
        BreathingRunState.PAUSED -> "Latihan dijeda"
        BreathingRunState.RUNNING -> when (snapshot.phase) {
            BreathingPhase.INHALE -> "Tarik napas"
            BreathingPhase.HOLD_INHALE -> "Tahan sebentar"
            BreathingPhase.EXHALE -> "Embuskan napas"
            BreathingPhase.HOLD_EXHALE -> "Jeda sejenak"
        }
    }

private fun hintFor(snapshot: BreathingSessionSnapshot): String =
    when (snapshot.runState) {
        BreathingRunState.READY -> "Duduk nyaman dan mulai perlahan."
        BreathingRunState.PAUSED -> "Lanjutkan saat kamu sudah siap."
        BreathingRunState.RUNNING -> when (snapshot.phase) {
            BreathingPhase.INHALE -> "Tarik napas secara perlahan."
            BreathingPhase.HOLD_INHALE -> "Tahan dengan nyaman, tanpa memaksa."
            BreathingPhase.EXHALE -> "Embuskan napas secara perlahan."
            BreathingPhase.HOLD_EXHALE -> "Lepaskan ketegangan sejenak."
        }
    }

private fun formatActiveTime(activeMs: Long): String {
    val seconds = activeMs / 1_000L
    return "%02d:%02d".format(seconds / 60L, seconds % 60L)
}
