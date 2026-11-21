package com.example.selaras.features.breathing

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.domain.model.BreathingPattern

@Composable
internal fun BreathingCatalogScreen(
    state: BreathingUiState,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
    onStart: () -> Unit
) {
    var showHelp by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BreathingHeader("Latihan Relaksasi", onBack = onBack, onHelp = { showHelp = true })
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "Pilih pola pernapasan yang nyaman untukmu.",
                color = BreathingColors.Text,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(20.dp))
            SectionLabel("Pola Bawaan")
            Spacer(Modifier.height(14.dp))
            BreathingPattern.defaults.forEach { pattern ->
                PatternCard(pattern, pattern.key == state.selectedKey, onSelect, onEdit)
                Spacer(Modifier.height(12.dp))
            }
            Spacer(Modifier.height(13.dp))
            SectionLabel("Pola Custom")
            Spacer(Modifier.height(12.dp))
            if (state.customPatterns.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BreathingColors.PaleGreen,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Belum ada pola kustom. Kamu bisa membuat ritme sendiri.",
                        modifier = Modifier.padding(17.dp),
                        color = BreathingColors.Muted,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(12.dp))
            } else {
                state.customPatterns.forEach { pattern ->
                    PatternCard(pattern, pattern.key == state.selectedKey, onSelect, onEdit)
                    Spacer(Modifier.height(12.dp))
                }
            }
            BreathingSecondaryButton("＋  Buat Pola Baru", onClick = onCreate)
            Spacer(Modifier.height(22.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            BreathingPrimaryButton(
                text = "Mulai Latihan",
                enabled = state.selectedPattern != null,
                onClick = onStart
            )
            Spacer(Modifier.height(6.dp))
            Text(
                state.error ?: if (state.selectedPattern == null) {
                    "Pilih satu pola terlebih dahulu."
                } else {
                    "Latihan tidak menyimpan riwayat sesi."
                },
                color = if (state.error != null) BreathingColors.Error else BreathingColors.Muted,
                fontSize = 12.sp
            )
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Cara latihan") },
            text = {
                Text(
                    "Pilih pola, tekan Mulai Latihan, lalu ikuti gerakan lingkaran. " +
                        "Tarik napas saat lingkaran membesar dan embuskan saat mengecil. " +
                        "Kamu bisa menjeda atau menghentikan sesi kapan saja."
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) { Text("Mengerti") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = BreathingColors.Text,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PatternCard(
    pattern: BreathingPattern,
    selected: Boolean,
    onSelect: (String) -> Unit,
    onEdit: (Long) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.RadioButton) {
            onSelect(pattern.key)
        },
        color = if (selected) BreathingColors.PaleGreen else Color.White,
        border = BorderStroke(if (selected) 2.dp else 1.dp,
            if (selected) BreathingColors.Primary else BreathingColors.Outline),
        shape = RoundedCornerShape(23.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Text("≋", fontSize = 26.sp, color = BreathingColors.Primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(pattern.name, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, color = BreathingColors.Text)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Tarik ${pattern.inhaleSeconds} dtk · Embus ${pattern.exhaleSeconds} dtk",
                    fontSize = 13.sp,
                    color = BreathingColors.Muted
                )
                if (pattern.isCustom) {
                    TextButton(
                        onClick = { pattern.id?.let(onEdit) },
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Text("Ubah pola", fontSize = 12.sp, color = BreathingColors.Primary)
                    }
                }
            }
            Box(
                Modifier.size(21.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(21.dp),
                    color = Color.Transparent,
                    border = BorderStroke(2.dp, BreathingColors.Primary),
                    shape = CircleShape
                ) { }
                if (selected) {
                    Surface(
                        modifier = Modifier.size(11.dp),
                        color = BreathingColors.Primary,
                        shape = CircleShape
                    ) { }
                }
            }
        }
    }
}
