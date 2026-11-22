package com.example.selaras.features.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.selaras.R

internal object OnboardingColors {
    val Primary = Color(0xFF356859)
    val PrimaryContainer = Color(0xFFDCECE3)
    val Background = Color(0xFFF7F7F2)
    val Surface = Color(0xFFFFFFFF)
    val TextPrimary = Color(0xFF243B35)
    val TextSecondary = Color(0xFF52675F)
    val Outline = Color(0xFF7B9187)
    val CardBorder = Color(0xFFE2E8E3)
    val AccentSage = Color(0xFFE0EDE4)
    val Error = Color(0xFFB3261E)
}

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel,
    forceShowOnboarding: Boolean = false,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToAppLock: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.currentDestination) {
        when (state.currentDestination) {
            OnboardingDestination.NAVIGATE_TO_DASHBOARD -> onNavigateToDashboard()
            OnboardingDestination.NAVIGATE_TO_APP_LOCK -> onNavigateToAppLock()
            else -> Unit
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        color = OnboardingColors.Background
    ) {
        AnimatedContent(
            targetState = state.currentDestination,
            transitionSpec = {
                (fadeIn(animationSpec = tween(320, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it / 3 })
                    .togetherWith(
                        fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing)) +
                            slideOutHorizontally(animationSpec = tween(250, easing = FastOutSlowInEasing)) { -it / 3 }
                    )
            },
            label = "OnboardingDestinationTransition",
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                OnboardingDestination.SPLASH -> {
                    SplashScreen(
                        onCheckLaunchState = {
                            viewModel.checkAppLaunchState(forceOnboarding = forceShowOnboarding)
                        }
                    )
                }
                OnboardingDestination.ONBOARDING_CAROUSEL -> {
                    BackHandler(enabled = state.carouselIndex > 0) {
                        viewModel.prevSlide()
                    }
                    OnboardingCarouselContent(
                        state = state,
                        slides = viewModel.slides,
                        onNext = viewModel::nextSlide,
                        onPrev = viewModel::prevSlide,
                        onDotClick = viewModel::setSlideIndex
                    )
                }
                OnboardingDestination.INFORMED_CONSENT -> {
                    BackHandler {
                        viewModel.backToCarousel()
                    }
                    InformedConsentContent(
                        state = state,
                        onConsentChange = viewModel::setConsentChecked,
                        onSubmitConsent = { viewModel.submitConsentAndComplete() },
                        onBackToCarousel = viewModel::backToCarousel
                    )
                }
                OnboardingDestination.CLOUD_BACKUP_PROMPT -> {
                    BackHandler {
                        viewModel.skipCloudBackup()
                    }
                    CloudBackupPromptContent(
                        state = state,
                        onModeChange = viewModel::setCloudBackupMode,
                        onEmailChange = viewModel::setBackupEmail,
                        onPasswordChange = viewModel::setBackupPassword,
                        onUserNameChange = viewModel::setBackupUserName,
                        onSubmit = { viewModel.submitCloudAuth() },
                        onSkip = { viewModel.skipCloudBackup() }
                    )
                }
                OnboardingDestination.NAVIGATE_TO_DASHBOARD,
                OnboardingDestination.NAVIGATE_TO_APP_LOCK -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = OnboardingColors.Primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplashScreen(
    onCheckLaunchState: () -> Unit
) {
    LaunchedEffect(Unit) {
        onCheckLaunchState()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Surface(
                modifier = Modifier.size(96.dp),
                shape = CircleShape,
                color = OnboardingColors.PrimaryContainer,
                border = BorderStroke(2.dp, OnboardingColors.Primary.copy(alpha = 0.2f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_selaras),
                        contentDescription = "Logo Selaras",
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Selaras",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = OnboardingColors.TextPrimary,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = OnboardingColors.Primary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
fun OnboardingCarouselContent(
    state: OnboardingUiState,
    slides: List<OnboardingSlideData>,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDotClick: (Int) -> Unit
) {
    val currentSlide = slides.getOrNull(state.carouselIndex) ?: slides.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Action Bar (Badge on the left, Skip button on the right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = OnboardingColors.AccentSage
            ) {
                Text(
                    text = currentSlide.categoryBadge,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = OnboardingColors.Primary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Slide Visual Emblem Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = OnboardingColors.Surface),
            border = BorderStroke(1.dp, OnboardingColors.CardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = currentSlide.iconRes),
                    contentDescription = currentSlide.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Headline & Description
        Text(
            text = currentSlide.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnboardingColors.TextPrimary,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = currentSlide.subtitle,
            fontSize = 15.sp,
            color = OnboardingColors.TextSecondary,
            lineHeight = 22.sp
        )

        Spacer(Modifier.weight(1f))

        // Pagination Dots Indicator
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until state.totalSlides) {
                val isSelected = i == state.carouselIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(8.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) OnboardingColors.Primary
                            else OnboardingColors.Outline.copy(alpha = 0.3f)
                        )
                        .clickable { onDotClick(i) }
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // Navigation Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.carouselIndex > 0) {
                OutlinedButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OnboardingColors.Outline)
                ) {
                    Text(
                        text = "Kembali",
                        color = OnboardingColors.Primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(if (state.carouselIndex > 0) 1f else 2f)
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = OnboardingColors.Primary)
            ) {
                Text(
                    text = if (state.isLastSlide) "Selesai" else "Lanjut",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun InformedConsentContent(
        state: OnboardingUiState,
        onConsentChange: (Boolean) -> Unit,
        onSubmitConsent: () -> Unit,
        onBackToCarousel: () -> Unit
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clickable(role = Role.Button, onClick = onBackToCarousel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = OnboardingColors.Primary
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Privasi & Persetujuan",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnboardingColors.TextPrimary
                )
            }

            Text(
                text = "Ketahui batasan layanan dan bagaimana Selaras melindungi ruang aman Anda.",
                fontSize = 14.sp,
                color = OnboardingColors.TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(16.dp))

            // Error message if any
            if (state.errorMessage != null) {
                Surface(
                    color = OnboardingColors.Error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = state.errorMessage,
                        color = OnboardingColors.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Scrollable Consent Clauses
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                ConsentClauseCard(
                    title = "Bukan Alat Diagnosis Medis",
                    content = "Selaras adalah sarana refleksi mandiri (*self-care companion*) dan penapisan awal. Selaras BUKAN pengganti diagnosis medis, pemeriksaan psikiatri, atau sesi psikoterapi formal dari tenaga kesehatan profesional berlisensi."
                )

                Spacer(Modifier.height(12.dp))

                ConsentClauseCard(
                    title = "Kedaulatan Privasi & Enkripsi Lokal",
                    content = "Seluruh catatan suasana hati harian, durasi tidur, dan hasil penapisan depresi PHQ-9 Anda disimpan secara lokal pada perangkat dan dienkripsi penuh menggunakan AES-256 (SQLCipher). Data Anda tidak dijual kepada pihak ketiga."
                )

                Spacer(Modifier.height(12.dp))

                ConsentClauseCard(
                    title = "Bantuan Krisis & Kontak Darurat",
                    content = "Jika Anda mengalami krisis psikologis hebat atau memiliki dorongan untuk menyakiti diri sendiri, segera hubungi Layanan Darurat Kesehatan Jiwa Kemenkes (Hotline 119 ext 8) atau segera kunjungi instalasi gawat darurat rumah sakit terdekat."
                )

                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(12.dp))

            // Checkbox Agreement Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onConsentChange(!state.isConsentChecked) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = state.isConsentChecked,
                    onCheckedChange = onConsentChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = OnboardingColors.Primary,
                        checkmarkColor = Color.White
                    )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Saya telah membaca, memahami, dan menyetujui seluruh ketentuan di atas.",
                    fontSize = 13.sp,
                    color = OnboardingColors.TextPrimary,
                    lineHeight = 18.sp
                )
            }

            Spacer(Modifier.height(16.dp))

            // Submit Button
            Button(
                onClick = onSubmitConsent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = state.isConsentChecked && !state.isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OnboardingColors.Primary,
                    disabledContainerColor = OnboardingColors.Outline.copy(alpha = 0.2f)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Saya Setuju & Lanjutkan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }

@Composable
private fun ConsentClauseCard(
    title: String,
    content: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OnboardingColors.Surface),
        border = BorderStroke(1.dp, OnboardingColors.CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnboardingColors.TextPrimary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                fontSize = 13.sp,
                color = OnboardingColors.TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun CloudBackupPromptContent(
    state: OnboardingUiState,
    onModeChange: (Boolean) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onUserNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingColors.Background)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // Header & Category Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(OnboardingColors.PrimaryContainer)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Opsional • Pencadangan Awan",
                color = OnboardingColors.Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Cadangkan Data Anda",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = OnboardingColors.TextPrimary,
            lineHeight = 32.sp
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Selaras bersifat offline-first. Pendaftaran akun bersifat opsional untuk mencadangkan catatan check-in, pola napas, dan hasil skrining agar bisa dipulihkan saat berganti perangkat.",
            fontSize = 13.sp,
            color = OnboardingColors.TextSecondary,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(16.dp))

        // Mode Toggle Tab: Daftar Akun Baru vs Masuk
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(OnboardingColors.Surface)
                .border(1.dp, OnboardingColors.CardBorder, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Button(
                onClick = { onModeChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isCloudBackupMode) OnboardingColors.Primary else Color.Transparent,
                    contentColor = if (state.isCloudBackupMode) Color.White else OnboardingColors.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                elevation = null
            ) {
                Text("Daftar Akun", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = { onModeChange(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!state.isCloudBackupMode) OnboardingColors.Primary else Color.Transparent,
                    contentColor = if (!state.isCloudBackupMode) Color.White else OnboardingColors.TextSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f),
                elevation = null
            ) {
                Text("Sudah Punya Akun", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Form Fields (Scrollable)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            if (state.syncErrorMessage != null) {
                Surface(
                    color = OnboardingColors.Error.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OnboardingColors.Error.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = state.syncErrorMessage,
                        color = OnboardingColors.Error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.isCloudBackupMode) {
                OutlinedTextField(
                    value = state.backupUserName,
                    onValueChange = onUserNameChange,
                    label = { Text("Nama Pengguna (Opsional)") },
                    placeholder = { Text("Contoh: Ahmad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = OnboardingColors.Primary,
                        focusedLabelColor = OnboardingColors.Primary,
                        cursorColor = OnboardingColors.Primary,
                        focusedContainerColor = OnboardingColors.Surface,
                        unfocusedContainerColor = OnboardingColors.Surface
                    )
                )

                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = state.backupEmail,
                onValueChange = onEmailChange,
                label = { Text("Alamat Email") },
                placeholder = { Text("nama@contoh.com") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OnboardingColors.Primary,
                    focusedLabelColor = OnboardingColors.Primary,
                    cursorColor = OnboardingColors.Primary,
                    focusedContainerColor = OnboardingColors.Surface,
                    unfocusedContainerColor = OnboardingColors.Surface
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = state.backupPassword,
                onValueChange = onPasswordChange,
                label = { Text("Kata Sandi") },
                placeholder = { Text("Minimal 8 karakter") },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(
                            text = if (isPasswordVisible) "Sembunyikan" else "Lihat",
                            fontSize = 11.sp,
                            color = OnboardingColors.Primary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OnboardingColors.Primary,
                    focusedLabelColor = OnboardingColors.Primary,
                    cursorColor = OnboardingColors.Primary,
                    focusedContainerColor = OnboardingColors.Surface,
                    unfocusedContainerColor = OnboardingColors.Surface
                )
            )

            Spacer(Modifier.height(12.dp))

            // Notice on Smart Merge & Privacy
            Surface(
                color = OnboardingColors.Surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, OnboardingColors.CardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isCloudBackupMode) {
                        "Catatan lokal Anda akan dicadangkan secara aman."
                    } else {
                        "Memulihkan data dari cloud akan menggabungkan riwayat awan dengan catatan lokal perangkat ini."
                    },
                    fontSize = 12.sp,
                    color = OnboardingColors.TextSecondary,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(8.dp))

        // Action Buttons
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !state.isSyncing && state.backupEmail.isNotBlank() && state.backupPassword.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = OnboardingColors.Primary,
                disabledContainerColor = OnboardingColors.Outline.copy(alpha = 0.2f)
            )
        ) {
            if (state.isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (state.isCloudBackupMode) "Daftar & Cadangkan" else "Masuk & Pulihkan Data",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // Skip Button
        TextButton(
            onClick = onSkip,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = !state.isSyncing
        ) {
            Text(
                text = "Lewati untuk Sekarang",
                color = OnboardingColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}
