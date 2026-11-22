package com.example.selaras.features.settings

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.core.ui.components.SelarasHeader
import com.example.selaras.core.ui.components.SelarasIcons
import com.example.selaras.features.breathing.BreathingColors

/**
 * Screen for managing App Lock, Notifications, and Local Data Privacy (Figma 168:2351, 168:2353).
 */
@Composable
fun SettingsScreen(
    isPinSet: Boolean = false,
    userName: String? = null,
    onUpdateUserName: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onResetPin: () -> Unit = {},
    onDeleteAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val backgroundCanvas = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val pineGreen = Color(0xFF356859)
    val primaryContainer = Color(0xFFDCECE3)
    val cardBorder = Color(0xFFE2E8E3)
    val errorRed = Color(0xFFB3261E)

    var isReminderEnabled by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var nameInput by remember(userName) { mutableStateOf(userName.orEmpty()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundCanvas)
    ) {
        SelarasHeader(
            title = "Pengaturan & Privasi",
            onBack = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Section 0: Profil & Nama Pengguna
            Text(
                text = "Profil Pengguna",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Nama Pengguna",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            if (userName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFE53935))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!userName.isNullOrBlank()) {
                                userName
                            } else {
                                "Belum diatur (Wajib untuk ringkasan konselor)"
                            },
                            fontSize = 12.sp,
                            color = if (!userName.isNullOrBlank()) pineGreen else Color(0xFFE53935)
                        )
                    }

                    Button(
                        onClick = {
                            nameInput = userName.orEmpty()
                            showNameDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryContainer,
                            contentColor = pineGreen
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = if (!userName.isNullOrBlank()) "Ubah Nama" else "Atur Nama",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Keamanan & Kunci Aplikasi
        Text(
            text = "Keamanan Aplikasi",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Kunci Aplikasi (6-Digit PIN)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isPinSet) "Status: Aktif & Terlindungi" else "Status: Belum Diatur",
                        fontSize = 12.sp,
                        color = if (isPinSet) pineGreen else textSecondary
                    )
                }

                Button(
                    onClick = onResetPin,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryContainer,
                        contentColor = pineGreen
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (isPinSet) "Ubah PIN" else "Atur PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 2: Pengingat Harian
        Text(
            text = "Pengingat Check-in",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Alarm Pengingat Malam",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Pukul 21.00 WIB",
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }

                Switch(
                    checked = isReminderEnabled,
                    onCheckedChange = { isReminderEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = pineGreen
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section 3: Data Lokal & Sanitasi
        Text(
            text = "Manajemen Data Lokal",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFFFF0E5))
                .border(1.dp, Color(0xFFFFD8BF), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Hapus Semua Data Mandiri",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = errorRed
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Menghapus seluruh catatan check-in, hasil skrining, pola pernapasan kustom, dan PIN secara permanen dari perangkat ini.",
                fontSize = 12.sp,
                color = textSecondary,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { showDeleteDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = errorRed,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Text(
                    text = "Hapus Semua Data",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Dialog Atur/Ubah Nama Pengguna
        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = {
                    Text(
                        text = if (!userName.isNullOrBlank()) "Ubah Nama Pengguna" else "Atur Nama Pengguna",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Nama ini akan digunakan untuk menyapa di Beranda dan dicantumkan pada dokumen PDF Ringkasan untuk Konselor.",
                            fontSize = 13.sp,
                            color = textSecondary,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nama Lengkap / Panggilan") },
                            placeholder = { Text("Contoh: Ahmad Pratama") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pineGreen,
                                focusedLabelColor = pineGreen,
                                cursorColor = pineGreen
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val trimmed = nameInput.trim()
                            if (trimmed.isNotBlank()) {
                                onUpdateUserName(trimmed)
                                showNameDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = pineGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Batal", color = textSecondary)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = "Hapus Seluruh Data?",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Tindakan ini akan menghapus seluruh catatan check-in, pola pernapasan kustom, hasil skrining, serta PIN keamanan secara permanen. Aplikasi akan kembali ke tahap orientasi awal.",
                        fontSize = 14.sp,
                        color = textSecondary,
                        lineHeight = 20.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteAllData()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = errorRed)
                    ) {
                        Text("Ya, Hapus Semua", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Batal", color = textSecondary)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
}
