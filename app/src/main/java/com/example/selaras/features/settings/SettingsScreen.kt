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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import com.example.selaras.core.security.AuthSession
import com.example.selaras.data.sync.SyncReport
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    authSession: AuthSession = AuthSession(),
    onUpdateUserName: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onResetPin: () -> Unit = {},
    onDeleteAllData: () -> Unit = {},
    onRegister: (email: String, password: String, userName: String?, onResult: (Result<Unit>) -> Unit) -> Unit = { _, _, _, _ -> },
    onLogin: (email: String, password: String, onResult: (Result<Unit>) -> Unit) -> Unit = { _, _, _ -> },
    onBackupNow: (onResult: (Result<SyncReport>) -> Unit) -> Unit = {},
    onRestoreNow: (onResult: (Result<SyncReport>) -> Unit) -> Unit = {},
    onLogout: () -> Unit = {},
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

    // Sync State
    var showSyncAuthDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isSyncLoading by remember { mutableStateOf(false) }
    var syncFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSyncFeedbackError by remember { mutableStateOf(false) }

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
                            color = if (!userName.isNullOrBlank()) pineGreen else Color(0xFFE53935),
                            lineHeight = 17.sp
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

        // Section: Sinkronisasi & Pencadangan Awan (Offline-First Companion)
        Text(
            text = "Pencadangan Awan & Sinkronisasi",
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
            if (syncFeedbackMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSyncFeedbackError) errorRed.copy(alpha = 0.1f) else primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = syncFeedbackMessage ?: "",
                        fontSize = 12.sp,
                        color = if (isSyncFeedbackError) errorRed else pineGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (!authSession.isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Column {
                            Text(
                                text = "Pencadangan Cloud",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Akun kamu belum terhubung, hubungkan email & password untuk mencadangkan data secara aman ke server awan.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showSyncAuthDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryContainer,
                            contentColor = pineGreen
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Hubungkan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Akun Cloud",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(primaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Terhubung", fontSize = 10.sp, color = pineGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = authSession.email ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary
                        )
                        val lastSyncText = authSession.lastSyncedAt?.let {
                            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm").withZone(ZoneId.systemDefault())
                            "Terakhir dicadangkan: ${formatter.format(it)}"
                        } ?: "Belum pernah dicadangkan"
                        Text(
                            text = lastSyncText,
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    TextButton(onClick = { showLogoutDialog = true }) {
                        Text("Keluar", color = errorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isSyncLoading = true
                            syncFeedbackMessage = null
                            onBackupNow { result ->
                                isSyncLoading = false
                                if (result.isSuccess) {
                                    val r = result.getOrNull()
                                    syncFeedbackMessage = "✅ Berhasil dicadangkan (${r?.totalPushed ?: 0} item diunggah)"
                                    isSyncFeedbackError = false
                                } else {
                                    syncFeedbackMessage = "❌ ${result.exceptionOrNull()?.message ?: "Gagal mencadangkan"}"
                                    isSyncFeedbackError = true
                                }
                            }
                        },
                        enabled = !isSyncLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = pineGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        if (isSyncLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Cadangkan Sekarang", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            isSyncLoading = true
                            syncFeedbackMessage = null
                            onRestoreNow { result ->
                                isSyncLoading = false
                                if (result.isSuccess) {
                                    val r = result.getOrNull()
                                    syncFeedbackMessage = "✅ Berhasil dipulihkan (${r?.totalPulled ?: 0} item digabungkan)"
                                    isSyncFeedbackError = false
                                } else {
                                    syncFeedbackMessage = "❌ ${result.exceptionOrNull()?.message ?: "Gagal memulihkan"}"
                                    isSyncFeedbackError = true
                                }
                            }
                        },
                        enabled = !isSyncLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryContainer, contentColor = pineGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(38.dp)
                    ) {
                        Text("Pulihkan Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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

        // Dialog Sinkronisasi & Akun Cloud
        if (showSyncAuthDialog) {
            var isRegisterMode by remember { mutableStateOf(true) }
            var authEmail by remember { mutableStateOf("") }
            var authPassword by remember { mutableStateOf("") }
            var authUserName by remember { mutableStateOf("") }
            var authError by remember { mutableStateOf<String?>(null) }
            var isAuthSubmitting by remember { mutableStateOf(false) }
            var isPasswordVisible by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = {
                    if (!isAuthSubmitting) showSyncAuthDialog = false
                },
                title = {
                    Text(
                        text = if (isRegisterMode) "Daftar Akun Cloud" else "Masuk Akun Cloud",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Hubungkan akun untuk mencadangkan catatan atau memulihkan data saat berganti perangkat.",
                            fontSize = 12.sp,
                            color = textSecondary,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Toggle Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(backgroundCanvas)
                                .border(1.dp, cardBorder, RoundedCornerShape(8.dp))
                                .padding(2.dp)
                        ) {
                            Button(
                                onClick = { isRegisterMode = true; authError = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isRegisterMode) pineGreen else Color.Transparent,
                                    contentColor = if (isRegisterMode) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                elevation = null
                            ) {
                                Text("Daftar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { isRegisterMode = false; authError = null },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isRegisterMode) pineGreen else Color.Transparent,
                                    contentColor = if (!isRegisterMode) Color.White else textSecondary
                                ),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).height(32.dp),
                                elevation = null
                            ) {
                                Text("Masuk", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (authError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(errorRed.copy(alpha = 0.1f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = authError ?: "",
                                    fontSize = 12.sp,
                                    color = errorRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = authUserName,
                                onValueChange = { authUserName = it },
                                label = { Text("Nama Pengguna (Opsional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = pineGreen,
                                    focusedLabelColor = pineGreen,
                                    cursorColor = pineGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = authEmail,
                            onValueChange = { authEmail = it; authError = null },
                            label = { Text("Alamat Email") },
                            placeholder = { Text("nama@contoh.com") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = pineGreen,
                                focusedLabelColor = pineGreen,
                                cursorColor = pineGreen
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = authPassword,
                            onValueChange = { authPassword = it; authError = null },
                            label = { Text("Kata Sandi") },
                            placeholder = { Text("Minimal 8 karakter") },
                            singleLine = true,
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Text(
                                        text = if (isPasswordVisible) "Tutup" else "Buka",
                                        fontSize = 11.sp,
                                        color = pineGreen
                                    )
                                }
                            },
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
                            val emailTrim = authEmail.trim()
                            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailTrim).matches()) {
                                authError = "Format email tidak valid"
                                return@Button
                            }
                            if (authPassword.length < 8) {
                                authError = "Kata sandi minimal 8 karakter"
                                return@Button
                            }

                            isAuthSubmitting = true
                            authError = null

                            if (isRegisterMode) {
                                onRegister(emailTrim, authPassword, authUserName.trim().ifBlank { null }) { res ->
                                    isAuthSubmitting = false
                                    if (res.isSuccess) {
                                        showSyncAuthDialog = false
                                        syncFeedbackMessage = "✅ Akun berhasil didaftarkan dan dihubungkan!"
                                        isSyncFeedbackError = false
                                    } else {
                                        authError = res.exceptionOrNull()?.message ?: "Gagal mendaftar"
                                    }
                                }
                            } else {
                                onLogin(emailTrim, authPassword) { res ->
                                    isAuthSubmitting = false
                                    if (res.isSuccess) {
                                        showSyncAuthDialog = false
                                        syncFeedbackMessage = "✅ Berhasil masuk ke akun cloud!"
                                        isSyncFeedbackError = false
                                    } else {
                                        authError = res.exceptionOrNull()?.message ?: "Email atau password salah"
                                    }
                                }
                            }
                        },
                        enabled = !isAuthSubmitting && authEmail.isNotBlank() && authPassword.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = pineGreen),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        if (isAuthSubmitting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (isRegisterMode) "Daftar & Hubungkan" else "Masuk & Hubungkan", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSyncAuthDialog = false },
                        enabled = !isAuthSubmitting
                    ) {
                        Text("Batal", color = textSecondary)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Dialog Konfirmasi Keluar Akun
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = {
                    Text(
                        text = "Keluar dari Akun Cloud?",
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                text = {
                    Text(
                        text = "Sesi sinkronisasi akan ditutup. Seluruh data pada perangkat ini (Room) akan tetap tersimpan secara aman dan tidak akan dihapus.",
                        fontSize = 13.sp,
                        color = textSecondary,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                            syncFeedbackMessage = "Sesi cloud telah keluar."
                            isSyncFeedbackError = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = errorRed),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Keluar Akun", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Batal", color = textSecondary)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
}
