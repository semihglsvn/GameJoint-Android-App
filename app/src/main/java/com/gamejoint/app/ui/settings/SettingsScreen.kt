package com.gamejoint.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onThemeChanged: (Int) -> Unit,
    onLogout: () -> Unit
) {
    val profile by viewModel.profileData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentCacheMb by viewModel.currentCacheMb.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showEmailDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.feedbackMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (isLoading && profile == null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFF55C72E)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(24.dp))

                // --- 1. PROFILE INFO ---
                Text("Account Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileRow("Username", profile?.username ?: "...")
                        ProfileRow("Email", profile?.email ?: "...")
                        ProfileRow("Joined", profile?.createdAt?.toString()?.substringBefore("T") ?: "...")                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- 2. APPEARANCE (THEME) ---
                Text("Appearance", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeButton("System", isSelected = currentTheme == 0, modifier = Modifier.weight(1f)) {
                        viewModel.updateTheme(0)
                        onThemeChanged(0)
                    }
                    ThemeButton("Light", isSelected = currentTheme == 1, modifier = Modifier.weight(1f)) {
                        viewModel.updateTheme(1)
                        onThemeChanged(1)
                    }
                    ThemeButton("Dark", isSelected = currentTheme == 2, modifier = Modifier.weight(1f)) {
                        viewModel.updateTheme(2)
                        onThemeChanged(2)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- 3. STORAGE (CACHE) ---
                Text("Storage (Image Cache)", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Disk Cache Limit", color = MaterialTheme.colorScheme.onSurface)
                            Text("${currentCacheMb} MB", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = currentCacheMb.toFloat(),
                            onValueChange = { viewModel.updateCacheSize(it.roundToInt().toLong()) },
                            valueRange = 10f..500f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF55C72E),
                                activeTrackColor = Color(0xFF55C72E)
                            )
                        )
                        Text("(Changes apply on next app restart)", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                // --- 4. ACCOUNT SECURITY ---
                Text("Account Security", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showEmailDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Change Email Address")
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Change Password")
                }
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF331111), contentColor = Color(0xFFFF4444))
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // --- DIALOGS ---
        if (showEmailDialog) {
            SecureActionDialog(
                title = "Change Email",
                actionType = "Email",
                viewModel = viewModel,
                onDismiss = { showEmailDialog = false; viewModel.resetOtpState() },
                onExecute = { otp, newValue -> viewModel.changeEmail(otp, newValue, onSuccess = onLogout) }
            )
        }

        if (showPasswordDialog) {
            SecureActionDialog(
                title = "Change Password",
                actionType = "Password",
                viewModel = viewModel,
                onDismiss = { showPasswordDialog = false; viewModel.resetOtpState() },
                onExecute = { otp, newValue -> viewModel.changePassword(otp, newValue, onSuccess = onLogout) }
            )
        }

        if (showDeleteDialog) {
            SecureActionDialog(
                title = "Delete Account",
                actionType = "Delete",
                viewModel = viewModel,
                onDismiss = { showDeleteDialog = false; viewModel.resetOtpState() },
                onExecute = { otp, _ -> viewModel.deleteAccount(otp, onSuccess = onLogout) }
            )
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun ThemeButton(label: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val containerColor = if (isSelected) Color(0xFF55C72E) else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface

    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureActionDialog(
    title: String,
    actionType: String,
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
    onExecute: (otp: String, newValue: String) -> Unit
) {
    val otpSent by viewModel.otpSent.collectAsState()
    val isActionLoading by viewModel.isActionLoading.collectAsState()

    var otpInput by remember { mutableStateOf("") }
    var newValueInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                if (!otpSent) {
                    Text("To protect your account, we need to verify your identity. We will send a 6-digit code to your registered email address.", color = Color.Gray, fontSize = 14.sp)
                } else {
                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it },
                        label = { Text("6-Digit Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (actionType != "Delete") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newValueInput,
                            onValueChange = { newValueInput = it },
                            label = { Text("New $actionType") },
                            visualTransformation = if (actionType == "Password") PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                            keyboardOptions = KeyboardOptions(keyboardType = if (actionType == "Email") KeyboardType.Email else KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Warning: This action is permanent and cannot be undone.", color = Color.Red, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!otpSent) {
                        viewModel.requestOtp()
                    } else {
                        onExecute(otpInput, newValueInput)
                    }
                },
                enabled = !isActionLoading,
                colors = ButtonDefaults.buttonColors(containerColor = if (actionType == "Delete" && otpSent) Color.Red else Color(0xFF55C72E))
            ) {
                if (isActionLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                } else {
                    Text(if (!otpSent) "Send Code" else "Confirm", color = if (actionType == "Delete" && otpSent) Color.White else Color.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}