package com.gamejoint.app.ui.profile

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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamejoint.app.data.model.ReviewResponse
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    targetUsername: String,
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToGame: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val currentUsername by viewModel.currentUsername.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val reviews by viewModel.rawReviews.collectAsState()
    val displayReviews by viewModel.displayReviews.collectAsState()

    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    // --- LUMINANCE DETECTION ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBgColor = if (isLightMode) MaterialTheme.colorScheme.background else Color(0xFF181818)
    val cardBgColor = if (isLightMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray
    val dividerColor = if (isLightMode) MaterialTheme.colorScheme.outlineVariant else Color.DarkGray

    LaunchedEffect(targetUsername) {
        viewModel.loadProfileData(targetUsername)
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewModel.loadProfileData(targetUsername)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(appBgColor)
    ) {
        if (isLoading && profile == null && !isRefreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF55C72E))
            }
        } else if (error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error!!, color = Color.Red, fontWeight = FontWeight.Bold)
            }
        } else if (profile != null) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {

                // --- 1. HEADER ---
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(profile!!.username, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                if (profile!!.isBanned) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color.Red).padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) { Text("BANNED", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                                }
                            }
                            Text("Member Since ${profile!!.createdAt.year}", color = secondaryTextColor, fontSize = 14.sp)
                        }

                        if (currentUsername != null && currentUsername == targetUsername) {
                            IconButton(onClick = onNavigateToSettings) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = secondaryTextColor)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(Modifier.height(24.dp))
                }

                // --- 2. STATS CARD ---
                item {
                    Text("Review Stats", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(Modifier.height(12.dp))

                    val validReviews = reviews
                    val isCritic = profile!!.roleName == "Critic"

                    if (validReviews.isEmpty()) {
                        Text("No reviews published yet.", color = secondaryTextColor)
                        Spacer(Modifier.height(24.dp))
                    } else {
                        val avgRaw = validReviews.map { it.score }.average()
                        val displayAvg = if (isCritic) avgRaw.roundToInt().toString() else "%.1f".format(avgRaw)

                        val posCount = validReviews.count { viewModel.getNormalizedScore(it.score) >= 75.0 }
                        val mixCount = validReviews.count { viewModel.getNormalizedScore(it.score) in 50.0..74.9 }
                        val negCount = validReviews.count { viewModel.getNormalizedScore(it.score) < 50.0 }

                        val highest = validReviews.maxByOrNull { it.score }
                        val lowest = validReviews.minByOrNull { it.score }

                        ElevatedCard(
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(if (isCritic) RoundedCornerShape(12.dp) else CircleShape)
                                        .background(getScoreColor(avgRaw.roundToInt(), isCritic)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(displayAvg, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text("AVG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Distribution", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                                    Spacer(Modifier.height(8.dp))
                                    DistRow("Pos", posCount, validReviews.size, Color(0xFF2ecc71), primaryTextColor, secondaryTextColor)
                                    DistRow("Mix", mixCount, validReviews.size, Color(0xFFf1c40f), primaryTextColor, secondaryTextColor)
                                    DistRow("Neg", negCount, validReviews.size, Color(0xFFe74c3c), primaryTextColor, secondaryTextColor)
                                }
                            }

                            HorizontalDivider(color = dividerColor, modifier = Modifier.padding(horizontal = 16.dp))

                            Row(modifier = Modifier.padding(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Highest Rated", fontSize = 12.sp, color = secondaryTextColor)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ScoreBadge(highest?.score ?: 0, isCritic)
                                        Spacer(Modifier.width(6.dp))
                                        Text(highest?.gameTitle ?: "-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Lowest Rated", fontSize = 12.sp, color = secondaryTextColor)
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ScoreBadge(lowest?.score ?: 0, isCritic)
                                        Spacer(Modifier.width(6.dp))
                                        Text(lowest?.gameTitle ?: "-", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = primaryTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                    HorizontalDivider(color = dividerColor)
                    Spacer(Modifier.height(24.dp))
                }

                // --- 3. FILTERS ---
                item {
                    Text("Reviews (${displayReviews.size})", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = primaryTextColor)
                    Spacer(Modifier.height(8.dp))

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item { FilterChip(selected = currentFilter == "all", onClick = { viewModel.setFilter("all") }, label = { Text("All") }) }
                        item { FilterChip(selected = currentFilter == "green", onClick = { viewModel.setFilter("green") }, label = { Text("Positive") }) }
                        item { FilterChip(selected = currentFilter == "yellow", onClick = { viewModel.setFilter("yellow") }, label = { Text("Mixed") }) }
                        item { FilterChip(selected = currentFilter == "red", onClick = { viewModel.setFilter("red") }, label = { Text("Negative") }) }

                        item { Spacer(Modifier.width(8.dp)) }

                        item { FilterChip(selected = currentSort == "date-desc", onClick = { viewModel.setSort("date-desc") }, label = { Text("Newest") }) }
                        item { FilterChip(selected = currentSort == "desc", onClick = { viewModel.setSort("desc") }, label = { Text("Highest") }) }
                        item { FilterChip(selected = currentSort == "asc", onClick = { viewModel.setSort("asc") }, label = { Text("Lowest") }) }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // --- 4. REVIEWS ---
                items(displayReviews) { review ->
                    ReviewCard(review, profile!!.roleName == "Critic", cardBgColor, primaryTextColor, secondaryTextColor, onNavigateToGame)
                }

                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

@Composable
fun DistRow(label: String, count: Int, total: Int, color: Color, primaryTextColor: Color, secondaryTextColor: Color) {
    val progress = if (total > 0) count.toFloat() / total else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(label, fontSize = 12.sp, modifier = Modifier.width(36.dp), color = primaryTextColor)

        // Parent Box acts as the full empty track and clips the outer edges to be round
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant) // The full empty bar
        ) {
            // Child Box acts as the fill, merging perfectly with the track
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .background(color)
            )
        }

        Text(count.toString(), fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End, color = secondaryTextColor)
    }
}
@Composable
fun ScoreBadge(score: Int, isCritic: Boolean) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(if (isCritic) RoundedCornerShape(4.dp) else CircleShape)
            .background(getScoreColor(score, isCritic)),
        contentAlignment = Alignment.Center
    ) {
        Text(score.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun ReviewCard(review: ReviewResponse, isCritic: Boolean, cardBgColor: Color, primaryTextColor: Color, secondaryTextColor: Color, onNavigateToGame: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBgColor),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(review.createdAt.toString().substringBefore("T"), color = secondaryTextColor, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNavigateToGame(review.gameId) }) {
                ScoreBadge(review.score, isCritic)
                Spacer(Modifier.width(12.dp))
                Text(review.gameTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D9CDB), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (!review.comment.isNullOrEmpty()) {
                Spacer(Modifier.height(12.dp))
                Column(modifier = Modifier.animateContentSize()) {
                    Text(
                        text = review.comment,
                        color = primaryTextColor,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp
                    )
                    if (review.comment.length > 150) {
                        Text(
                            text = if (expanded) "Show Less" else "Read More",
                            color = Color(0xFF4DA6FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp).clickable { expanded = !expanded }
                        )
                    }
                }
            }
        }
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