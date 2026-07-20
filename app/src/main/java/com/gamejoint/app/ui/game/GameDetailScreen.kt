package com.gamejoint.app.ui.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: Long,
    viewModel: GameDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userReviews by viewModel.userReviews.collectAsState()
    val criticReviews by viewModel.criticReviews.collectAsState()
    val avgUserScore by viewModel.avgUserScore.collectAsState()

    // AUTHENTICATION STATES
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isBanned by viewModel.isBanned.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = User, 1 = Critic
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF121212))) {
        when (val state = uiState) {
            is GameDetailState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF55C72E))
            is GameDetailState.Error -> Text(state.message, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            is GameDetailState.Success -> {
                val game = state.game

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // --- 1. HERO BANNER ---
                    item {
                        AsyncImage(
                            model = game.coverImage,
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    // --- 2. TITLE ---
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(game.title ?: "Unknown", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                game.genres?.take(3)?.forEach { genre ->
                                    Box(modifier = Modifier.background(Color(0xFF2D9CDB), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(genre, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    // --- 3. SCORE HUB ---
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val meta = game.metascore ?: 0
                                val metaColor = getScoreColor(meta, isCritic = true)
                                Box(modifier = Modifier.size(60.dp).background(metaColor, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Text(if (meta > 0) meta.toString() else "TBD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Metascore", color = Color.White, fontWeight = FontWeight.Bold)
                            }

                            Box(modifier = Modifier.width(1.dp).height(80.dp).background(Color.DarkGray))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val uScore = avgUserScore ?: 0.0
                                val uColor = getScoreColor((uScore * 10).toInt(), isCritic = true)
                                Box(modifier = Modifier.size(60.dp).background(uColor, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                                    Text(if (uScore > 0) String.format(java.util.Locale.US, "%.1f", uScore) else "TBD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("User Score", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                    }

                    // --- 4. ABOUT ---
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize()) {
                            Text("About", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = game.description ?: "No description.",
                                color = Color.LightGray, fontSize = 14.sp, lineHeight = 20.sp,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4
                            )
                            TextButton(onClick = { isDescriptionExpanded = !isDescriptionExpanded }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (isDescriptionExpanded) "Show Less" else "Read More", color = Color(0xFF55C72E))
                            }
                            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    // --- 5. CONDITIONAL REVIEW BOX ---
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text("Your Review", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!isLoggedIn) {
                                // GUEST
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Sign in or create an account to rate and review this game.", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (isBanned) {
                                // BANNED USER
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF331111), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Your account has been restricted. You cannot post reviews at this time.", color = Color(0xFFFF3333), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (currentUserRole in 1L..3L) {
                                // STAFF MEMBER
                                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Staff members cannot write reviews.", color = Color.Gray, fontSize = 14.sp)
                                }
                            } else {
                                // VALID CRITIC OR USER
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF222222), RoundedCornerShape(8.dp)).clickable { viewModel.showReviewModal.value = true }.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tap to manage or edit your review...", color = Color(0xFF2D9CDB), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // --- 6. NAVIGATION TABS ---
                    item {
                        SecondaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = Color(0xFF55C72E)
                        ) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, unselectedContentColor = Color.White) {
                                Text("User Reviews", modifier = Modifier.padding(16.dp))
                            }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, unselectedContentColor = Color.White) {
                                Text("Critic Reviews", modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    // --- 7. REVIEWS LIST FEED ---
                    val activeReviews = if (selectedTab == 0) userReviews else criticReviews

                    if (activeReviews.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No reviews yet.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(activeReviews) { review ->
                            ReviewCard(
                                review = review,
                                isCritic = selectedTab == 1,
                                isLoggedIn = isLoggedIn,
                                isBanned = isBanned,
                                currentUserRole = currentUserRole,
                                onReport = {
                                    viewModel.targetReviewId.value = review.id
                                    viewModel.showReportModal.value = true
                                },
                                onBan = {
                                    viewModel.targetUserId.value = review.id // Fallback ID mapping
                                    viewModel.targetUsername.value = review.authorUsername ?: "User"
                                    viewModel.showBanModal.value = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (viewModel.showReviewModal.collectAsState().value) {
            ReviewEditorModal(gameId, currentUserRole == 4L, viewModel)
        }
        if (viewModel.showReportModal.collectAsState().value) {
            ReportModal(viewModel)
        }
        if (viewModel.showBanModal.collectAsState().value) {
            BanModal(viewModel)
        }
    }
}

@Composable
fun ReviewCard(
    review: com.gamejoint.app.data.model.ReviewResponse,
    isCritic: Boolean,
    isLoggedIn: Boolean,
    isBanned: Boolean,
    currentUserRole: Long,
    onReport: () -> Unit,
    onBan: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp).background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val sColor = getScoreColor(review.score ?: 0, isCritic)
                val shape = if (isCritic) RoundedCornerShape(4.dp) else RoundedCornerShape(20.dp)
                Box(modifier = Modifier.size(40.dp).background(sColor, shape), contentAlignment = Alignment.Center) {
                    Text(review.score.toString(), color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(review.authorUsername ?: "Anonymous", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // Conditional Moderation Buttons
            if (isLoggedIn && currentUserRole in 1L..3L) {
                Button(onClick = onBan, colors = ButtonDefaults.buttonColors(containerColor = Color.Red), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Text("BAN USER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            } else if (isLoggedIn && !isBanned) {
                IconButton(onClick = onReport) { Icon(Icons.Default.Warning, contentDescription = "Report", tint = Color.Gray) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(review.comment ?: "", color = Color.LightGray, fontSize = 14.sp)
    }
}

fun getScoreColor(score: Int, isCritic: Boolean): Color {
    val normalized = if (!isCritic) score * 10 else score
    return when {
        normalized >= 75 -> Color(0xFF55C72E)
        normalized >= 50 -> Color(0xFFFFB800)
        normalized > 0 -> Color(0xFFFF3333)
        else -> Color.Gray
    }
}

@Composable
fun ReviewEditorModal(gameId: Long, isCritic: Boolean, viewModel: GameDetailViewModel) {
    var text by remember { mutableStateOf(viewModel.draftText.value) }
    var scoreStr by remember { mutableStateOf(viewModel.draftScore.value.toString().takeIf { it != "0" } ?: "") }

    AlertDialog(
        onDismissRequest = {
            viewModel.saveDraft(gameId, text, scoreStr.toIntOrNull() ?: 0)
            viewModel.showReviewModal.value = false
        },
        containerColor = Color(0xFF222222),
        title = { Text("Write Review", color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = scoreStr,
                    onValueChange = { scoreStr = it },
                    label = { Text("Score (1-${if(isCritic) 100 else 10})", color = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Your thoughts...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.saveDraft(gameId, text, scoreStr.toIntOrNull() ?: 0)
                    viewModel.submitReview(gameId)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55C72E))
            ) { Text("Post", color = Color.Black) }
        }
    )
}

@Composable
fun ReportModal(viewModel: GameDetailViewModel) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { viewModel.showReportModal.value = false },
        containerColor = Color(0xFF222222),
        title = { Text("Report Review", color = Color.White) },
        text = {
            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("Reason for report...", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        },
        confirmButton = {
            Button(onClick = { viewModel.submitReport(reason) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Submit Report")
            }
        }
    )
}

@Composable
fun BanModal(viewModel: GameDetailViewModel) {
    val username = viewModel.targetUsername.collectAsState().value
    var selectedDuration by remember { mutableStateOf<Int?>(1) }
    var banReason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { viewModel.showBanModal.value = false },
        containerColor = Color(0xFF222222),
        title = { Text("Ban $username", color = Color.Red) },
        text = {
            Column {
                OutlinedTextField(
                    value = banReason,
                    onValueChange = { banReason = it },
                    label = { Text("Reason for ban (Optional)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == 1, onClick = { selectedDuration = 1 })
                    Text("24 Hours", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == 7, onClick = { selectedDuration = 7 })
                    Text("7 Days", color = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == null, onClick = { selectedDuration = null })
                    Text("Permanent", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.banUser(selectedDuration, banReason) }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                Text("Execute Ban")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showBanModal.value = false }) { Text("Cancel", color = Color.Gray) }
        }
    )
}