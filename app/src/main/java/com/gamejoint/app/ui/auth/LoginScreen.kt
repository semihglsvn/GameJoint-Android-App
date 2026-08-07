package com.gamejoint.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamejoint.app.data.local.SessionManager
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onNavigateToVerify: (String) -> Unit,
    onNavigateToOAuthComplete: (String, String) -> Unit // NEW
) {
    val uiState by viewModel.uiState.collectAsState()

    // 1. Initialize DataStore SessionManager
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // NEW: For Google Sign In
    val sessionManager = remember { SessionManager(context) }

    var usernameOrEmail by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBgColor = if (isLightMode) MaterialTheme.colorScheme.background else Color(0xFF181818)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    // --- AUTO-NAVIGATION & TOKEN SAVING ---
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is LoginState.Success -> {
                // Save the token securely to the device
                sessionManager.saveToken(state.token)
                onNavigateToHome()
            }
            is LoginState.Unverified -> {
                // Push them to the 6-digit OTP screen
                onNavigateToVerify(state.email)
            }
            is LoginState.OAuthIncomplete -> {
                // Need username/DOB to complete Google signup
                onNavigateToOAuthComplete(state.email, state.providerToken)
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appBgColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to GameJoint",
            style = MaterialTheme.typography.headlineMedium,
            color = primaryTextColor
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = usernameOrEmail,
            onValueChange = { usernameOrEmail = it; localError = null },
            label = { Text("Username or Email") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor,
                unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60),
                unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60),
                unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; localError = null },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = primaryTextColor,
                unfocusedTextColor = primaryTextColor,
                focusedLabelColor = Color(0xFF27AE60),
                unfocusedLabelColor = secondaryTextColor,
                focusedBorderColor = Color(0xFF27AE60),
                unfocusedBorderColor = secondaryTextColor,
                cursorColor = Color(0xFF27AE60)
            )
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                text = "Forgot Password?",
                color = Color(0xFF27AE60),
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
                        viewModel.login(usernameOrEmail.trim(), password)
                    }
                ) {
                    Text("Login", color = Color.White)
                }

                // --- NEW: GOOGLE SIGN-IN BUTTON ---
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "OR", color = secondaryTextColor, style = MaterialTheme.typography.bodySmall)

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLightMode) Color(0xFFF2F2F2) else Color(0xFF333333),
                        contentColor = primaryTextColor
                    ),
                    onClick = {
                        coroutineScope.launch {
                            localError = null
                            val token = GoogleAuthHelper.signInWithGoogle(context)
                            if (token != null) {
                                viewModel.oauthLogin("GOOGLE", token)
                            } else {
                                localError = "Google Sign-In was cancelled or failed."
                            }
                        }
                    }
                ) {
                    Text("Sign in with Google")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Don't have an account? Register here",
            color = Color(0xFF27AE60),
            modifier = Modifier.clickable { onNavigateToRegister() }
        )
    }
}