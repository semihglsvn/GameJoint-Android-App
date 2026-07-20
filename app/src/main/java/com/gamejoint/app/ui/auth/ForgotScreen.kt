package com.gamejoint.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ForgotScreen(
    viewModel: ForgotViewModel = viewModel(),
    onNavigateBackToLogin: () -> Unit,
    onNavigateToVerification: (String) -> Unit // NEW: Fixes the MainActivity error!
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- AUTO-NAVIGATION ON SUCCESS ---
    LaunchedEffect(uiState) {
        if (uiState is ForgotState.Success) {
            val targetEmail = (uiState as ForgotState.Success).email
            onNavigateToVerification(targetEmail)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Reset Password", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Enter your email address and we will send you a 6-digit code to reset your password.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; localError = null },
            label = { Text("Email Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        val displayError = localError ?: if (uiState is ForgotState.Error) {
            (uiState as ForgotState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (uiState) {
            is ForgotState.Loading, is ForgotState.Success -> {
                CircularProgressIndicator(color = Color(0xFF27AE60))
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    onClick = {
                        if (email.isBlank() || !email.contains("@")) {
                            localError = "Please enter a valid email address."
                            return@Button
                        }
                        localError = null
                        viewModel.requestPasswordReset(email)
                    }
                ) {
                    Text("Send Reset Code")
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Back to Login",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateBackToLogin() }
        )
    }
}