package com.gamejoint.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gamejoint.app.data.model.GameSummary
import kotlinx.coroutines.launch

// REBRAND: Fully transitioned to JointScore naming
val JointScoreGreen = Color(0xFF55C72E)
val JointScoreYellow = Color(0xFFD4A017)
val JointScoreRed = Color(0xFFD32F2F)
val JointScoreGray = Color(0xFF555555)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onGameClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val showBanPopup by viewModel.showBanPopup.collectAsState()
    val banExpiration by viewModel.banExpiration.collectAsState()

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // --- MAGIC FIX: Detect theme dynamically to preserve your custom Dark Mode hexes! ---
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val appBgColor = if (isLightMode) MaterialTheme.colorScheme.background else Color(0xFF181818)
    val cardBgColor = if (isLightMode) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF222222)
    val primaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryTextColor = if (isLightMode) MaterialTheme.colorScheme.onSurfaceVariant else Color.LightGray

    if (showBanPopup) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissBanPopup() },
            containerColor = cardBgColor,
            title = { Text("Account Restricted", color = JointScoreRed, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your account has been reviewed and restricted by a human moderator for violating our community guidelines.", color = primaryTextColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("There is no automated system involved, and this decision is final. No appeals will be accepted at this time.", color = secondaryTextColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Restriction Ends: ${banExpiration ?: "Permanent"}", color = JointScoreRed, fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissBanPopup() }, colors = ButtonDefaults.buttonColors(containerColor = JointScoreRed)) {
                    Text("I Understand", color = Color.White)
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            coroutineScope.launch {
                viewModel.fetchHomeData(isRefresh = true)
                isRefreshing = false
            }
        },
        modifier = Modifier.fillMaxSize().background(appBgColor)
    ) {
        when (uiState) {
            is HomeState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryTextColor)
                }
            }
            is HomeState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text((uiState as HomeState.Error).message, color = Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.fetchHomeData() },
                        colors = ButtonDefaults.buttonColors(containerColor = JointScoreGreen)
                    ) {
                        Text("Tap to Retry", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
            is HomeState.Success -> {
                val data = (uiState as HomeState.Success).data

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                ) {
                    if (data.featured.isNotEmpty()) {
                        item { SectionTitle("Featured Games", primaryTextColor) }
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(data.featured) { game ->
                                    val imageUrl = game.customBanner ?: game.coverImage ?: ""
                                    val score = game.metascore ?: 0
                                    val genreList = game.genres?.take(2) ?: emptyList()

                                    GameCard(
                                        title = game.title ?: "Unknown",
                                        imageUrl = imageUrl,
                                        score = score,
                                        genres = genreList,
                                        cardBgColor = cardBgColor,
                                        primaryTextColor = primaryTextColor,
                                        secondaryTextColor = secondaryTextColor,
                                        onClick = { onGameClick(game.gameId ?: 0L) }
                                    )
                                }
                            }
                        }
                    }

                    if (data.trending.isNotEmpty()) {
                        item { SectionTitle("Trending", primaryTextColor) }
                        item { HorizontalGameCarousel(data.trending, cardBgColor, primaryTextColor, secondaryTextColor, onGameClick) }
                    }

                    if (data.newReleases.isNotEmpty()) {
                        item { SectionTitle("New Releases", primaryTextColor) }
                        item { HorizontalGameCarousel(data.newReleases, cardBgColor, primaryTextColor, secondaryTextColor, onGameClick) }
                    }

                    if (data.topRated.isNotEmpty()) {
                        item { SectionTitle("Top Rated", primaryTextColor) }
                        item { HorizontalGameCarousel(data.topRated, cardBgColor, primaryTextColor, secondaryTextColor, onGameClick) }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, textColor: Color) {
    Text(
        text = title,
        color = textColor,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
    )
}

@Composable
fun HorizontalGameCarousel(games: List<GameSummary>, cardBgColor: Color, primaryTextColor: Color, secondaryTextColor: Color, onGameClick: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games) { game ->
            val activeGenres = game.genres ?: emptyList()
            val genreList = activeGenres.take(2)

            GameCard(
                title = game.title ?: "Unknown",
                imageUrl = game.coverImage ?: "",
                score = game.metascore ?: 0,
                genres = genreList,
                cardBgColor = cardBgColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor,
                onClick = { onGameClick(game.id ?: 0L) }
            )
        }
    }
}

@Composable
fun GameCard(
    title: String,
    imageUrl: String,
    score: Int,
    genres: List<String>,
    cardBgColor: Color,
    primaryTextColor: Color,
    secondaryTextColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$title Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    color = primaryTextColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (genres.isEmpty()) {
                        GenreChip("N/A", secondaryTextColor)
                    } else {
                        genres.forEach { genre ->
                            GenreChip(genre, secondaryTextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = secondaryTextColor.copy(alpha = 0.5f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "JOINTSCORE", color = secondaryTextColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    ScoreBox(score)
                }
            }
        }
    }
}

@Composable
fun GenreChip(text: String, textColor: Color) {
    Box(modifier = Modifier.border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
        Text(text = text.uppercase(), color = textColor, fontSize = 10.sp)
    }
}

@Composable
fun ScoreBox(score: Int) {
    val scoreColor = when {
        score == 0 -> JointScoreGray
        score >= 75 -> JointScoreGreen
        score >= 50 -> JointScoreYellow
        else -> JointScoreRed
    }

    val displayScore = if (score == 0) "TBD" else score.toString()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(scoreColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = displayScore, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}