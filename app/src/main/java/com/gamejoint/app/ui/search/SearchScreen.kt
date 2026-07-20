package com.gamejoint.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@Composable
fun SearchScreen(
    initialQuery: String,
    onGameClick: (Long) -> Unit,
    viewModel: SearchViewModel = viewModel()
) {
    // --- FIX 1: All @Composable State Collections MUST be at the top! ---
    val uiState by viewModel.uiState.collectAsState()
    val totalGames by viewModel.totalResults.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    var showFilters by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    val sortOptions = listOf("Highest Rated", "Lowest Rated", "Newest First", "Oldest First")
    val availableGenres = listOf("Action", "Adventure", "Arcade", "Board Games", "Card", "Casual", "Educational", "Family", "Fighting", "Indie", "Massively Multiplayer", "Platformer", "Puzzle", "Racing", "RPG", "Shooter", "Simulation", "Sports", "Strategy")
    val availablePlatforms = listOf("3DO", "Android", "Apple II", "Atari 2600", "Atari 5200", "Atari 7800", "Atari 8-bit", "Atari Flashback", "Atari Lynx", "Atari ST", "Classic Macintosh", "Commodore / Amiga", "Dreamcast", "Game Boy", "Game Boy Advance", "Game Boy Color", "Game Gear", "GameCube", "Genesis", "iOS", "Jaguar", "Linux", "macOS", "Neo Geo", "NES", "PC", "PlayStation 5", "PlayStation 4", "PlayStation 3", "PlayStation 2", "Xbox Series S/X", "Xbox One", "Xbox 360", "Nintendo Switch", "Web")

    LaunchedEffect(initialQuery) {
        if (viewModel.currentQuery.value.isEmpty() && initialQuery.isNotBlank()) {
            viewModel.currentQuery.value = initialQuery
            viewModel.performSearch()
        } else if (initialQuery.isBlank() && viewModel.currentQuery.value.isEmpty()) {
            showFilters = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF181818))
            .imePadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- HEADER ROW ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explore", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { showFilters = !showFilters },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = "Filters", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Filters", color = Color.White)
                    }
                }
            }

            // --- EXPANDABLE FILTERS ---
            item {
                AnimatedVisibility(visible = showFilters) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222222), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.currentQuery.collectAsState().value,
                            onValueChange = { viewModel.currentQuery.value = it },
                            placeholder = { Text("Type a game name...", color = Color.Gray) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray,
                                    modifier = Modifier.clickable { viewModel.performSearch() }
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    showFilters = false
                                    viewModel.performSearch()
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF333333),
                                unfocusedContainerColor = Color(0xFF333333),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // --- FIX 2: Stable Custom Dropdown (No deprecation warnings) ---
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = viewModel.sortBy.collectAsState().value,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sort By", color = Color.Gray) },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort", tint = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF2D9CDB),
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color(0xFF333333),
                                    unfocusedContainerColor = Color(0xFF333333)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            // Magic Invisible Box that perfectly intercepts clicks for the dropdown
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Transparent)
                                    .clickable { sortExpanded = true }
                            )

                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false },
                                modifier = Modifier.background(Color(0xFF333333))
                            ) {
                                sortOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option, color = Color.White) },
                                        onClick = {
                                            viewModel.sortBy.value = option
                                            sortExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = viewModel.hideTbd.collectAsState().value,
                                    onCheckedChange = { viewModel.hideTbd.value = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF55C72E))
                                )
                                Text("Hide TBD", color = Color.White, fontSize = 14.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Logic: ", color = Color.Gray, fontSize = 14.sp)
                                RadioButton(
                                    selected = !viewModel.isMatchAll.collectAsState().value,
                                    onClick = { viewModel.isMatchAll.value = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2D9CDB))
                                )
                                Text("OR", color = Color.White, fontSize = 12.sp)
                                RadioButton(
                                    selected = viewModel.isMatchAll.collectAsState().value,
                                    onClick = { viewModel.isMatchAll.value = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2D9CDB))
                                )
                                Text("AND", color = Color.White, fontSize = 12.sp)
                            }
                        }

                        // --- FIX 3: Renamed to HorizontalDivider ---
                        HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))

                        Text("Genres", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availableGenres) { genre ->
                                val isSelected = viewModel.selectedGenres.collectAsState().value.contains(genre)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val current = viewModel.selectedGenres.value.toMutableSet()
                                        if (isSelected) current.remove(genre) else current.add(genre)
                                        viewModel.selectedGenres.value = current
                                    },
                                    label = { Text(genre) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF2D9CDB),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Platforms", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        LazyRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(availablePlatforms) { platform ->
                                val isSelected = viewModel.selectedPlatforms.collectAsState().value.contains(platform)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        val current = viewModel.selectedPlatforms.value.toMutableSet()
                                        if (isSelected) current.remove(platform) else current.add(platform)
                                        viewModel.selectedPlatforms.value = current
                                    },
                                    label = { Text(platform) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF55C72E),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                showFilters = false
                                viewModel.performSearch()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF55C72E)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("APPLY FILTERS", fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                    }
                }
            }

            // --- SEARCH RESULTS ---
            when (val state = uiState) {
                is SearchState.Idle -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("Search a game or apply filters.", color = Color.Gray)
                    }
                }
                is SearchState.Loading -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF55C72E))
                    }
                }
                is SearchState.Error -> item {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.Red)
                    }
                }
                is SearchState.Success -> {
                    val games = state.games

                    if (games.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("No games found.", color = Color.Gray)
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Found $totalGames games",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        itemsIndexed(games) { index, game ->

                            // Trigger load next page when at the bottom
                            if (index == games.size - 1) {
                                LaunchedEffect(Unit) {
                                    viewModel.loadNextPage()
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF222222), RoundedCornerShape(8.dp))
                                    .clickable { onGameClick(game.id ?: 0L) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = game.coverImage,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    // ADD THIS: A sleek, dark gray placeholder while the image downloads
                                    placeholder = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF2A2A2A)),
                                    error = androidx.compose.ui.graphics.painter.ColorPainter(Color(0xFF3A3A3A)), // Fallback if link is broken
                                    modifier = Modifier
                                        .size(80.dp, 100.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(16.dp))

                                Column {
                                    Text(
                                        text = game.title ?: "Unknown",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val score = game.metascore ?: 0
                                    val scoreColor = when {
                                        score >= 75 -> Color(0xFF55C72E)
                                        score >= 50 -> Color(0xFFFFB800)
                                        score > 0 -> Color(0xFFFF3333)
                                        else -> Color.Gray
                                    }
                                    Text(
                                        text = if (score > 0) "Score: $score" else "TBD",
                                        color = scoreColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        text = game.genres?.joinToString(", ") ?: "",
                                        color = Color.Gray,
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (isLoadingMore) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFF55C72E), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}