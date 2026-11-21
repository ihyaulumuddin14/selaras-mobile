package com.example.selaras.features.breathing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun BreathingPatternFormScreen(
    state: BreathingUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onInhaleChange: (String) -> Unit,
    onExhaleChange: (String) -> Unit,
    onAdjustInhale: (Int) -> Unit,
    onAdjustExhale: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val isEditing = state.editingId != null
    var confirmDelete by remember(isEditing) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        BreathingHeader(
            title = if (isEditing) "Edit Pola Custom" else "Buat Pola Custom",
            onBack = onBack
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Text("Selaras · Pola Custom", color = BreathingColors.Primary,
                fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(18.dp))
            Text("Atur ritme yang ingin kamu gunakan.",
                color = BreathingColors.Text, fontSize = 16.sp)
            Spacer(Modifier.height(18.dp))

            OutlinedTextField(
                value = state.formName,
                onValueChange = onNameChange,
                label = { Text("Nama pola") },
                placeholder = { Text("Masukkan nama pola") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(13.dp),
                colors = breathingTextFieldColors()
            )
            Spacer(Modifier.height(16.dp))
            DurationInput(
                title = "Tarik napas (inhale)",
                value = state.formInhale,
                onValueChange = onInhaleChange,
                onAdjust = onAdjustInhale
            )
            Spacer(Modifier.height(16.dp))
            DurationInput(
                title = "Embuskan napas (exhale)",
                value = state.formExhale,
                onValueChange = onExhaleChange,
                onAdjust = onAdjustExhale
            )
            Spacer(Modifier.height(20.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BreathingColors.PaleGreen,
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Preview pola", color = BreathingColors.Text,
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tarik ${state.formInhale.ifBlank { "–" }} detik · " +
                            "Embus ${state.formExhale.ifBlank { "–" }} detik",
                        color = BreathingColors.Muted, fontSize = 14.sp
                    )
                }
            }
            if (state.error != null) {
                Spacer(Modifier.height(10.dp))
                Text(state.error, color = BreathingColors.Error, fontSize = 13.sp)
            }
            if (isEditing) {
                Spacer(Modifier.height(9.dp))
                TextButton(onClick = { confirmDelete = true }, enabled = !state.isSaving) {
                    Text("Hapus pola ini", color = BreathingColors.Error)
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            BreathingPrimaryButton(
                text = if (state.isSaving) "Menyimpan..." else if (isEditing) {
                    "Simpan Perubahan"
                } else {
                    "Simpan Pola"
                },
                enabled = !state.isSaving,
                onClick = onSave
            )
            Spacer(Modifier.height(9.dp))
            BreathingSecondaryButton("Batal", enabled = !state.isSaving, onClick = onBack)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus pola pernapasan?") },
            text = { Text("Pola kustom ini akan dihapus dari perangkat. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Hapus", color = BreathingColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun DurationInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onAdjust: (Int) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(title) },
        suffix = { Text("detik") },
        supportingText = { Text("Durasi 1–60 detik") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        isError = value.toIntOrNull()?.let { it !in 1..60 } ?: true,
        colors = breathingTextFieldColors()
    )
    Spacer(Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = { onAdjust(-1) },
            modifier = Modifier.weight(1f).height(47.dp),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, BreathingColors.Outline)
        ) {
            Text("−", color = BreathingColors.Primary, fontSize = 20.sp)
        }
        OutlinedButton(
            onClick = { onAdjust(1) },
            modifier = Modifier.weight(1f).height(47.dp),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, BreathingColors.Outline)
        ) {
            Text("+", color = BreathingColors.Primary, fontSize = 20.sp)
        }
    }
}

@Composable
private fun breathingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = BreathingColors.Primary,
    focusedLabelColor = BreathingColors.Primary,
    unfocusedBorderColor = BreathingColors.Outline,
    unfocusedLabelColor = BreathingColors.Muted,
    cursorColor = BreathingColors.Primary,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White
)
