package com.gamejoint.app.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamejoint.app.data.local.SessionManager

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onNavigateToVerify: (String) -> Unit // NEW: Fixes the MainActivity error!
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Initialize DataStore SessionManager
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- AUTO-NAVIGATION & TOKEN SAVING ---
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginState.Success -> {
                // 2. Save the token securely to the device
                sessionManager.saveToken(state.token)
                onNavigateToHome()
            }
            is LoginState.Unverified -> {
                // 3. Push them to the 6-digit OTP screen
                onNavigateToVerify(state.email)
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome to GameJoint", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = usernameOrEmail,
            onValueChange = { usernameOrEmail = it; localError = null },
            label = { Text("Username or Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "Forgot Password?",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .clickable { onNavigateToForgot() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        val displayError = localError ?: if (uiState is LoginState.Error) {
            (uiState as LoginState.Error).message
        } else null

        displayError?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(16.dp))
        }

        when (uiState) {
            is LoginState.Loading, is LoginState.Success, is LoginState.Unverified -> {
                // Keep spinning while we navigate or write to DataStore
                CircularProgressIndicator(color = Color(0xFF27AE60))
            }
            else -> {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                    onClick = {
                        if (usernameOrEmail.isBlank() || password.isBlank()) {
                            localError = "Please enter both credentials."
                            return@Button
                        }
                        localError = null
                        viewModel.login(usernameOrEmail, password)
                    }
                ) {
                    Text("Login")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Don't have an account? Register here",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}