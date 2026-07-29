package com.gamejoint.app.ui.game

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameDetailScreen(
    gameId: Long,
    viewModel: GameDetailViewModel = viewModel(),
    onNavigateToProfile: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val userReviews by viewModel.userReviews.collectAsState()
    val criticReviews by viewModel.criticReviews.collectAsState()
    val avgUserScore by viewModel.avgUserScore.collectAsState()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val isBanned by viewModel.isBanned.collectAsState()
    val existingReviewId by viewModel.existingReviewId.collectAsState()
    val draftScore by viewModel.draftScore.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBgColor = if (isLightMode) MaterialTheme.colorScheme.background else Color(0xFF121212)
    val cardBgColor = if (isLightMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val dividerColor = if (isLightMode) MaterialTheme.colorScheme.outlineVariant else Color.DarkGray
    val modalBgColor = if (isLightMode) MaterialTheme.colorScheme.surface else Color(0xFF222222)
    val placeholderColor = if (isLightMode) Color.LightGray else Color(0xFF2A2A2A)

    LaunchedEffect(gameId) {
        viewModel.loadGame(gameId)
    }

    LaunchedEffect(Unit) {
        viewModel.feedbackMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewModel.loadGame(gameId)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(appBgColor)
    ) {
        when (val state = uiState) {
            is GameDetailState.Loading -> if (!isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF55C72E))
                }
            }
            is GameDetailState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is GameDetailState.Success -> {
                val game = state.game

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // 1. HERO BANNER
                    item {
                        AsyncImage(
                            model = game.coverImage,
                            contentDescription = game.title,
                            contentScale = ContentScale.Crop,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(placeholderColor),
                            modifier = Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp, vertical = 8.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }

                    // 2. TITLE & GENRES
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(game.title ?: "Unknown", color = primaryTextColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val activeGenres = game.genreNames?.toList() ?: game.genres ?: emptyList()
                                activeGenres.take(3).forEach { genre ->
                                    Box(modifier = Modifier.background(Color(0xFF2D9CDB), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                        Text(genre, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    // 3. SCORE HUB
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val hasLocalCriticReviews = criticReviews.isNotEmpty()
                                val calculatedJointScore = if (hasLocalCriticReviews) {
                                    criticReviews.map { it.score }.average().roundToInt()
                                } else {
                                    game.metascore ?: 0
                                }

                                val scoreColor = getScoreColor(calculatedJointScore, isCritic = true)
                                Box(modifier = Modifier.size(60.dp).background(scoreColor, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                                    Text(if (calculatedJointScore > 0) calculatedJointScore.toString() else "TBD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("JointScore", color = primaryTextColor, fontWeight = FontWeight.Bold)

                                val subtitle = if (hasLocalCriticReviews) {
                                    "Based on ${criticReviews.size} reviews"
                                } else if (calculatedJointScore > 0) {
                                    "External DB rating"
                                } else {
                                    "No reviews yet"
                                }
                                Text(subtitle, color = secondaryTextColor, fontSize = 11.sp)
                            }

                            Box(modifier = Modifier.width(1.dp).height(80.dp).background(dividerColor))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val uScore = avgUserScore ?: 0.0
                                val uColor = getScoreColor((uScore * 10).toInt(), isCritic = true)
                                Box(modifier = Modifier.size(60.dp).background(uColor, RoundedCornerShape(30.dp)), contentAlignment = Alignment.Center) {
                                    Text(if (uScore > 0) String.format(java.util.Locale.US, "%.1f", uScore) else "TBD", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("User Score", color = primaryTextColor, fontWeight = FontWeight.Bold)
                                Text(if (userReviews.isNotEmpty()) "Based on ${userReviews.size} reviews" else "No reviews yet", color = secondaryTextColor, fontSize = 11.sp)
                            }
                        }
                        HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp))
                    }

                    // 4. ABOUT
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).animateContentSize()) {
                            Text("About", color = primaryTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            val platformsStr = game.platformNames?.joinToString(", ") ?: game.platforms?.joinToString(", ")
                            val metaDataMap = mapOf("Developer" to game.developer, "Publisher" to game.publisher, "Release Date" to game.releaseDate, "Platforms" to platformsStr)

                            metaDataMap.forEach { (label, value) ->
                                if (!value.isNullOrBlank()) {
                                    Row(modifier = Modifier.padding(bottom = 4.dp)) {
                                        Text("$label: ", color = secondaryTextColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(value, color = primaryTextColor, fontSize = 13.sp)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = game.description ?: "No description available.",
                                color = primaryTextColor, fontSize = 14.sp, lineHeight = 20.sp,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { isDescriptionExpanded = !isDescriptionExpanded }, contentPadding = PaddingValues(0.dp)) {
                                Text(if (isDescriptionExpanded) "Show Less" else "Read More", color = Color(0xFF55C72E))
                            }

                            // RAWG ATTRIBUTION
                            Spacer(Modifier.height(8.dp))
                            Text("Data courtesy of RAWG.io", color = secondaryTextColor, fontSize = 11.sp, fontStyle = FontStyle.Italic)

                            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 16.dp))
                        }
                    }

                    // 5. CONDITIONAL REVIEW BOX
                    item {
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                            Text("Your Review", color = primaryTextColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            if (!isLoggedIn) {
                                Box(modifier = Modifier.fillMaxWidth().background(cardBgColor, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Sign in or create an account to rate and review this game.", color = secondaryTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (isBanned) {
                                val banBgColor = if (isLightMode) MaterialTheme.colorScheme.errorContainer else Color(0xFF331111)
                                val banTextColor = if (isLightMode) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFFF3333)
                                Box(modifier = Modifier.fillMaxWidth().background(banBgColor, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Your account has been restricted. You cannot post reviews.", color = banTextColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (currentUserRole in 1L..3L) {
                                Box(modifier = Modifier.fillMaxWidth().background(cardBgColor, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Staff members cannot write reviews.", color = secondaryTextColor, fontSize = 14.sp)
                                }
                            } else if (existingReviewId != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(cardBgColor, RoundedCornerShape(8.dp)).padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("You have already reviewed this game.", color = primaryTextColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Score: $draftScore", color = getScoreColor(draftScore, currentUserRole == 4L), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IconButton(onClick = { viewModel.showReviewModal.value = true }) { Icon(Icons.Default.Edit, contentDescription = "Edit Review", tint = Color(0xFF2D9CDB)) }
                                        IconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete Review", tint = Color.Red) }
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxWidth().background(cardBgColor, RoundedCornerShape(8.dp)).clickable { viewModel.showReviewModal.value = true }.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text("Tap to write a review...", color = Color(0xFF2D9CDB), fontWeight = FontWeight.Bold) }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 6. TABS & SORT CHIPS
                    item {
                        SecondaryTabRow(selectedTabIndex = selectedTab, containerColor = Color.Transparent, contentColor = Color(0xFF55C72E)) {
                            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, unselectedContentColor = secondaryTextColor) { Text("User Reviews", modifier = Modifier.padding(16.dp)) }
                            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, unselectedContentColor = secondaryTextColor) { Text("Critic Reviews", modifier = Modifier.padding(16.dp)) }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = currentFilter == "all", onClick = { viewModel.setFilter("all") }, label = { Text("All") }) }
                            item { FilterChip(selected = currentFilter == "green", onClick = { viewModel.setFilter("green") }, label = { Text("Pos") }) }
                            item { FilterChip(selected = currentFilter == "yellow", onClick = { viewModel.setFilter("yellow") }, label = { Text("Mix") }) }
                            item { FilterChip(selected = currentFilter == "red", onClick = { viewModel.setFilter("red") }, label = { Text("Neg") }) }

                            item { Spacer(modifier = Modifier.width(8.dp)) }

                            item { FilterChip(selected = currentSort == "date-desc", onClick = { viewModel.setSort("date-desc") }, label = { Text("Newest") }) }
                            item { FilterChip(selected = currentSort == "date-asc", onClick = { viewModel.setSort("date-asc") }, label = { Text("Oldest") }) }
                            item { FilterChip(selected = currentSort == "desc", onClick = { viewModel.setSort("desc") }, label = { Text("Highest") }) }
                            item { FilterChip(selected = currentSort == "asc", onClick = { viewModel.setSort("asc") }, label = { Text("Lowest") }) }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 7. REVIEWS LIST FEED
                    val activeReviews = if (selectedTab == 0) userReviews else criticReviews

                    if (activeReviews.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No reviews yet.", color = secondaryTextColor) }
                        }
                    } else {
                        items(activeReviews) { review ->
                            ReviewCard(
                                review = review,
                                isCritic = selectedTab == 1,
                                isLoggedIn = isLoggedIn,
                                isBanned = isBanned,
                                currentUserRole = currentUserRole,
                                onAuthorClick = { username -> onNavigateToProfile(username) },
                                onReport = {
                                    viewModel.targetReviewId.value = review.id
                                    viewModel.showReportModal.value = true
                                },
                                onBan = {
                                    viewModel.targetUserId.value = 0L
                                    // FIXED: Safely pass the known non-null username
                                    viewModel.targetUsername.value = review.authorUsername
                                    viewModel.showBanModal.value = true
                                }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(40.dp)) }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        // --- MODALS ---
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                containerColor = modalBgColor,
                title = { Text("Delete Review", color = primaryTextColor) },
                text = { Text("Are you sure you want to permanently delete this review?", color = secondaryTextColor) },
                confirmButton = { Button(onClick = { viewModel.deleteReview(gameId); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete", color = MaterialTheme.colorScheme.onError) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = secondaryTextColor) } }
            )
        }
        if (viewModel.showReviewModal.collectAsState().value) ReviewEditorModal(gameId, currentUserRole == 4L, viewModel)
        if (viewModel.showReportModal.collectAsState().value) ReportModal(viewModel)
        if (viewModel.showBanModal.collectAsState().value) BanModal(viewModel)
    }
}

@Composable
fun ReviewCard(
    review: com.gamejoint.app.data.model.ReviewResponse,
    isCritic: Boolean,
    isLoggedIn: Boolean,
    isBanned: Boolean,
    currentUserRole: Long,
    onAuthorClick: (String) -> Unit,
    onReport: () -> Unit,
    onBan: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val commentText = review.comment ?: ""
    val isLongText = commentText.length > 150

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val reviewCardBgColor = if (isLightMode) MaterialTheme.colorScheme.surface else Color(0xFF1E1E1E)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = reviewCardBgColor),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {

            Text(review.createdAt.toString().substringBefore("T"), color = secondaryTextColor, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ScoreBadge(review.score, isCritic)
                    Spacer(modifier = Modifier.width(12.dp))

                    // FIXED: Eliminated the elvis operator and null check because authorUsername is strictly non-nullable now!
                    Text(
                        text = review.authorUsername,
                        color = Color(0xFF2D9CDB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable { onAuthorClick(review.authorUsername) }
                    )
                }

                if (isLoggedIn && currentUserRole in 1L..3L) {
                    Button(onClick = onBan, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("BAN", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
                    }
                } else if (isLoggedIn && !isBanned) {
                    IconButton(onClick = onReport) { Icon(Icons.Default.Warning, contentDescription = "Report", tint = secondaryTextColor) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = commentText,
                color = primaryTextColor,
                fontSize = 14.sp,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.defaultMinSize(minHeight = 80.dp)
            )

            if (isLongText) {
                Text(
                    text = if (isExpanded) "Show Less" else "Read More",
                    color = Color(0xFF4DA6FF),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp).clickable { isExpanded = !isExpanded }
                )
            }
        }
    }
}

@Composable
fun ScoreBadge(score: Int, isCritic: Boolean) {
    val sColor = getScoreColor(score, isCritic)
    val shape = if (isCritic) RoundedCornerShape(4.dp) else CircleShape
    Box(modifier = Modifier.size(40.dp).background(sColor, shape), contentAlignment = Alignment.Center) {
        Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewEditorModal(gameId: Long, isCritic: Boolean, viewModel: GameDetailViewModel) {
    val currentDraftText by viewModel.draftText.collectAsState()
    val currentDraftScore by viewModel.draftScore.collectAsState()
    val existingId by viewModel.existingReviewId.collectAsState()

    var text by remember { mutableStateOf(currentDraftText) }

    val minScore = 1f
    val maxScore = if (isCritic) 100f else 10f

    val initialScore = currentDraftScore.toFloat().takeIf { it > 0f } ?: (maxScore / 2f)
    var sliderValue by remember { mutableFloatStateOf(initialScore.coerceIn(minScore, maxScore)) }

    val dynamicColor = getScoreColor(sliderValue.toInt(), isCritic)
    var showPostConfirm by remember { mutableStateOf(false) }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val modalBgColor = if (isLightMode) MaterialTheme.colorScheme.surface else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
    val sliderInactiveColor = if (isLightMode) MaterialTheme.colorScheme.outlineVariant else Color(0xFF333333)

    if (showPostConfirm) {
        AlertDialog(
            onDismissRequest = { showPostConfirm = false },
            containerColor = modalBgColor,
            title = { Text(if (existingId != null) "Update Review" else "Post Review", color = primaryTextColor) },
            text = { Text("Are you ready to submit this review?", color = secondaryTextColor) },
            confirmButton = {
                Button(
                    onClick = {
                        showPostConfirm = false
                        viewModel.saveDraft(gameId, text, sliderValue.toInt())
                        viewModel.submitReview(gameId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55C72E))
                ) { Text("Confirm", color = Color.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showPostConfirm = false }) { Text("Wait, go back", color = secondaryTextColor) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.saveDraft(gameId, text, sliderValue.toInt())
            viewModel.showReviewModal.value = false
        },
        containerColor = modalBgColor,
        title = { Text(if (existingId != null) "Edit Review" else "Write Review", color = primaryTextColor) },
        text = {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Score:", color = primaryTextColor, fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.background(dynamicColor, RoundedCornerShape(4.dp)).padding(horizontal = 12.dp, vertical = 4.dp)) {
                        Text("${sliderValue.toInt()}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it.roundToInt().toFloat()
                    },
                    valueRange = minScore..maxScore,
                    colors = SliderDefaults.colors(
                        activeTrackColor = dynamicColor,
                        inactiveTrackColor = sliderInactiveColor
                    ),
                    thumb = {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(primaryTextColor)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= 2000) text = it
                    },
                    label = { Text("Your thoughts...", color = secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = primaryTextColor,
                        unfocusedTextColor = primaryTextColor
                    ),
                    supportingText = {
                        Text("${text.length} / 2000", color = if (text.length >= 2000) MaterialTheme.colorScheme.error else secondaryTextColor)
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { showPostConfirm = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55C72E))
            ) { Text("Review", color = Color.Black) }
        }
    )
}

@Composable
fun ReportModal(viewModel: GameDetailViewModel) {
    val predefinedReasons = listOf("Spam", "Offensive Language", "Spoilers", "Harassment", "Irrelevant")
    var selectedReasons by remember { mutableStateOf(setOf<String>()) }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val modalBgColor = if (isLightMode) MaterialTheme.colorScheme.surface else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    AlertDialog(
        onDismissRequest = { viewModel.showReportModal.value = false },
        containerColor = modalBgColor,
        title = { Text("Report Review", color = primaryTextColor) },
        text = {
            Column {
                Text("Please select one or more reasons:", color = secondaryTextColor, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                predefinedReasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedReasons = if (selectedReasons.contains(reason)) {
                                    selectedReasons - reason
                                } else {
                                    selectedReasons + reason
                                }
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = selectedReasons.contains(reason),
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error, uncheckedColor = secondaryTextColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(reason, color = primaryTextColor, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.submitReport(selectedReasons.toList()) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                enabled = selectedReasons.isNotEmpty()
            ) {
                Text("Submit Report", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showReportModal.value = false }) { Text("Cancel", color = secondaryTextColor) }
        }
    )
}

@Composable
fun BanModal(viewModel: GameDetailViewModel) {
    val username by viewModel.targetUsername.collectAsState()
    var selectedDuration by remember { mutableStateOf<Int?>(1) }
    var banReason by remember { mutableStateOf("") }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val modalBgColor = if (isLightMode) MaterialTheme.colorScheme.surface else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray

    AlertDialog(
        onDismissRequest = { viewModel.showBanModal.value = false },
        containerColor = modalBgColor,
        title = { Text("Ban $username", color = MaterialTheme.colorScheme.error) },
        text = {
            Column {
                OutlinedTextField(
                    value = banReason,
                    onValueChange = { banReason = it },
                    label = { Text("Reason for ban (Optional)", color = secondaryTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = primaryTextColor, unfocusedTextColor = primaryTextColor)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == 1, onClick = { selectedDuration = 1 })
                    Text("24 Hours", color = primaryTextColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == 7, onClick = { selectedDuration = 7 })
                    Text("7 Days", color = primaryTextColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedDuration == null, onClick = { selectedDuration = null })
                    Text("Permanent", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.banUser(selectedDuration, banReason) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Text("Execute Ban", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showBanModal.value = false }) { Text("Cancel", color = secondaryTextColor) }
        }
    )
}