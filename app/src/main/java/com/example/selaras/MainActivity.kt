package com.example.selaras

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.example.selaras.core.ui.theme.SelarasTheme
import com.example.selaras.features.security.AppLockMasterScreen
import com.example.selaras.features.security.AppLockMode
import com.example.selaras.features.security.AppLockViewModel
import com.example.selaras.features.security.components.SecurityIcons

class MainActivity : ComponentActivity() {

    private lateinit var appLockViewModel: AppLockViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as SelarasApplication).container

        appLockViewModel = ViewModelProvider(
            this,
            AppLockViewModel.Factory(container.securityRepository)
        )[AppLockViewModel::class.java]

        enableEdgeToEdge()
        setContent {
            SelarasTheme {
                var testDestination by remember { mutableStateOf("SECURITY") }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (testDestination) {
                            "SECURITY" -> {
                                AppLockMasterScreen(
                                    viewModel = appLockViewModel,
                                    onUnlockSuccess = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Berhasil Membuka Kunci!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        testDestination = "SUCCESS_HUB"
                                    },
                                    onSetupComplete = {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "PIN & Recovery Berhasil Dibuat!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        testDestination = "SUCCESS_HUB"
                                    }
                                )
                            }
                            "SUCCESS_HUB" -> {
                                SecurityTestHub(
                                    onTestUnlock = {
                                        appLockViewModel.switchToUnlockMode()
                                        testDestination = "SECURITY"
                                    },
                                    onTestRecovery = {
                                        appLockViewModel.switchToRecoveryMode()
                                        testDestination = "SECURITY"
                                    },
                                    onResetAndTestSetup = {
                                        appLockViewModel.restartSecurityForTesting()
                                        testDestination = "SECURITY"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SecurityTestHub(
    onTestUnlock: () -> Unit,
    onTestRecovery: () -> Unit,
    onResetAndTestSetup: () -> Unit
) {
    val pineGreen = Color(0xFF356859)
    val background = Color(0xFFF7F7F2)
    val textPrimary = Color(0xFF243B35)
    val textSecondary = Color(0xFF52675F)
    val primaryContainer = Color(0xFFDCECE3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = SecurityIcons.Shield,
                contentDescription = null,
                tint = pineGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Autentikasi Berhasil",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Fitur App Lock & Recovery Code berjalan dengan sukses sesuai standar keamanan Selaras.",
            fontSize = 14.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Pilih Skenario Pengujian:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Button(
                    onClick = onTestUnlock,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pineGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("1. Tes Buka Kunci (Unlock Screen)")
                }

                Button(
                    onClick = onTestRecovery,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = pineGreen),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("2. Tes Lupa PIN & Pemulihan")
                }

                OutlinedButton(
                    onClick = onResetAndTestSetup,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("3. Reset & Ulangi Setup PIN Awal", color = pineGreen)
                }
            }
        }
    }
}
