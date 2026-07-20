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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gamejoint.app.data.model.FeaturedGameResponse
import com.gamejoint.app.data.model.GameSummary

// Match the dark theme from the web exactly
val SurfaceDark = Color(0xFF222222)
val MetascoreGreen = Color(0xFF55C72E)
val MetascoreYellow = Color(0xFFD4A017)
val MetascoreRed = Color(0xFFD32F2F)
val MetascoreGray = Color(0xFF555555) // For TBD

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onGameClick: (Long) -> Unit // Navigates to Game Details later
) {
    val uiState by viewModel.uiState.collectAsState()

    when (uiState) {
        is HomeState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        }
        is HomeState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text((uiState as HomeState.Error).message, color = Color.Red)
            }
        }
        is HomeState.Success -> {
            val data = (uiState as HomeState.Success).data

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF181818)) // Outer background
                    .padding(vertical = 16.dp)
            ) {

                // 1. Featured Games
                if (data.featured.isNotEmpty()) {
                    item { SectionTitle("Featured Games") }
                    item {
// We must explicitly add horizontalArrangement here!
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(data.featured) { game ->
                                // For Featured, we use the custom banner if it exists!
                                val imageUrl = game.customBanner ?: game.coverImage ?: ""
// The new fields we manually added to FeaturedGameResponse.kt!
                                val score = game.metascore ?: 0
                                // Just like we did for the other sliders, take up to 2 genres
                                val genreList = game.genres?.take(2) ?: emptyList()

                                GameCard(
                                    title = game.title ?: "Unknown",
                                    imageUrl = imageUrl,
                                    score = score,
                                    genres = genreList,
                                    onClick = { onGameClick(game.gameId ?: 0L) }
                                )
                            }
                        }
                    }
                }

                // 2. Trending
                if (data.trending.isNotEmpty()) {
                    item { SectionTitle("Trending") }
                    item { HorizontalGameCarousel(data.trending, onGameClick) }
                }

                // 3. New Releases
                if (data.newReleases.isNotEmpty()) {
                    item { SectionTitle("New Releases") }
                    item { HorizontalGameCarousel(data.newReleases, onGameClick) }
                }

                // 4. Top Rated
                if (data.topRated.isNotEmpty()) {
                    item { SectionTitle("Top Rated") }
                    item { HorizontalGameCarousel(data.topRated, onGameClick) }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 16.dp)
    )
}

@Composable
fun HorizontalGameCarousel(games: List<GameSummary>, onGameClick: (Long) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(games) { game ->
            // The generator already made it a List! Just take the first 2.
            val genreList = game.genres?.take(2) ?: emptyList()

            // Also adding a safe fallback for game.id just in case it's null
            GameCard(
                title = game.title ?: "Unknown",
                imageUrl = game.coverImage ?: "",
                score = game.metascore ?: 0,
                genres = genreList,
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(260.dp) // Matches the wide aspect ratio in your screenshot
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column {
            // 1. Game Image
            AsyncImage(
                model = imageUrl,
                contentDescription = "$title Cover",
                contentScale = ContentScale.Crop, // Crops image to fit the box cleanly
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            // 2. Info Container
            Column(modifier = Modifier.padding(16.dp)) {

                // Title
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Genres/Platform Chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (genres.isEmpty()) {
                        GenreChip("N/A")
                    } else {
                        genres.forEach { genre ->
                            GenreChip(genre)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF444444), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Footer: METASCORE and Score Box
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "METASCORE",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    ScoreBox(score)
                }
            }
        }
    }
}

@Composable
fun GenreChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = Color.LightGray,
            fontSize = 10.sp
        )
    }
}

@Composable
fun ScoreBox(score: Int) {
    // Determine color based on Metacritic standards
    val scoreColor = when {
        score == 0 -> MetascoreGray // TBD
        score >= 75 -> MetascoreGreen
        score >= 50 -> MetascoreYellow
        else -> MetascoreRed
    }

    val displayScore = if (score == 0) "TBD" else score.toString()

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(scoreColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayScore,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}