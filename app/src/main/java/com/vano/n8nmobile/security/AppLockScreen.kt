package com.vano.n8nmobile.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.vano.n8nmobile.ui.AiwaColors
import com.vano.n8nmobile.ui.AiwaDecorativeFont

@Composable
fun AppLockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    var pinInput by remember { mutableStateOf("") }
    var errorShown by remember { mutableStateOf(false) }
    val biometricEnabled = remember { AppLockStore.isBiometricEnabled(context) }

    fun tryBiometric() {
        val activity = context as? FragmentActivity ?: return
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onUnlocked()
            }
        })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka Aiwa")
            .setSubtitle("Gunakan sidik jari buat masuk")
            .setNegativeButtonText("Pakai PIN")
            .build()
        prompt.authenticate(promptInfo)
    }

    LaunchedEffect(Unit) {
        if (biometricEnabled) tryBiometric()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AiwaColors.Background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Masukin PIN", color = Color.White, fontSize = 20.sp, fontFamily = AiwaDecorativeFont)
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (i < pinInput.length) AiwaColors.Pink else Color.White.copy(alpha = 0.2f))
                )
            }
        }
        if (errorShown) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("PIN salah", color = Color.Red, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "del")
        )
        rows.forEach { row ->
            Row {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable {
                                when (key) {
                                    "del" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                    "bio" -> if (biometricEnabled) tryBiometric()
                                    else -> {
                                        if (pinInput.length < 6) {
                                            pinInput += key
                                            if (pinInput.length == 6) {
                                                if (AppLockStore.verifyPin(context, pinInput)) {
                                                    onUnlocked()
                                                } else {
                                                    errorShown = true
                                                    pinInput = ""
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "del" -> Icon(Icons.Default.Backspace, contentDescription = "Hapus", tint = Color.White)
                            "bio" -> if (biometricEnabled) {
                                Icon(Icons.Default.Fingerprint, contentDescription = "Sidik jari", tint = Color.White)
                            }
                            else -> Text(key, color = Color.White, fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}
