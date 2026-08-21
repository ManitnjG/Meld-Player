package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FlacBadge
import com.example.ui.components.TrackListItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeldViewModel

@Composable
fun ExploreScreen(
    viewModel: MeldViewModel,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val spotifyOnlineResults by viewModel.spotifyOnlineResults.collectAsState()
    val isSearchingSpotify by viewModel.isSearchingSpotify.collectAsState()
    val selectedFormat by viewModel.selectedFormatFilter.collectAsState()
    val selectedGenre by viewModel.selectedGenreFilter.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    var directLinkInput by remember { mutableStateOf("") }

    val genreFilters = listOf("All", "Spotify Hits", "Tamil Hits", "Kollywood", "A.R. Rahman", "Anirudh", "Hi-Res 24-bit", "Lossless FLAC", "Cyberpunk", "Jazz & Soul")
    val quickSearches = listOf("Blinding Lights", "Shape of You", "Naa Ready", "Cruel Summer", "Hukum", "Bad Guy", "Chinna Chinna Aasai", "Arabic Kuthu", "Starboy", "Espresso", "Vaseegara")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // Search Header
        item {
            Text(
                text = "Search & Explore",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Search any worldwide song, Spotify track link, or Tamil master in 24-bit FLAC",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Search Text Field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search any song, artist, Spotify link, or Tamil hit...", color = TextTertiary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = FlacCyan) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FlacCyan,
                    unfocusedBorderColor = StrokeColor,
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_text_field")
            )
        }

        // Quick Category / Genre Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genreFilters) { filter ->
                    FilterChip(
                        selected = selectedGenre == filter,
                        onClick = { viewModel.setGenreFilter(filter) },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlacCyan,
                            selectedLabelColor = DeepBlack,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        )
                    )
                }
            }
        }

        // Quick Search Tags (when no active query)
        if (searchQuery.isBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Popular Global Hits & Tamil Master Catalog",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickSearches) { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier.clickable { viewModel.setSearchQuery(tag) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = FlacCyan, modifier = Modifier.size(12.dp))
                                    Text(tag, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Direct SpotiFLAC Link & Universal Song Resolver Box
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ElectricBolt, contentDescription = null, tint = FlacCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SpotiFLAC Spotify Link & Metadata Resolver", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Paste any Spotify track URL (e.g. open.spotify.com/track/...) or type any song title to extract metadata & play in lossless FLAC:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = directLinkInput,
                            onValueChange = { directLinkInput = it },
                            placeholder = { Text("https://open.spotify.com/track/... or Song Title", color = TextTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = FlacCyan,
                                unfocusedBorderColor = StrokeColor,
                                focusedContainerColor = DarkSurfaceElevated,
                                unfocusedContainerColor = DarkSurface
                            )
                        )

                        Button(
                            onClick = {
                                if (directLinkInput.isNotBlank()) {
                                    viewModel.resolveExternalSpotifyLink(directLinkInput)
                                    directLinkInput = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
                            shape = RoundedCornerShape(10.dp),
                            enabled = directLinkInput.isNotBlank()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Spotify Live Online Catalog Results Section (when query exists)
        if (searchQuery.isNotBlank() && spotifyOnlineResults.isNotEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F231D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, tint = LosslessGreen, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "Spotify Online Metadata & FLAC Streams",
                                    color = LosslessGreen,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isSearchingSpotify) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = LosslessGreen
                                )
                            }
                        }

                        spotifyOnlineResults.forEach { onlineTrack ->
                            val isPlaying = playerState.currentTrack?.id == onlineTrack.id
                            TrackListItem(
                                track = onlineTrack,
                                isPlayingThisTrack = isPlaying,
                                onTrackClick = { viewModel.playOnlineSpotifyTrack(onlineTrack) },
                                onLikeClick = { viewModel.toggleLiked(onlineTrack) },
                                onDownloadClick = { viewModel.downloadFlacTrack(onlineTrack) }
                            )
                        }
                    }
                }
            }
        }

        // Search Results / Library Tracks Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) "Library Matches (${searchResults.size})" else "All Available Tracks (${searchResults.size})",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (selectedGenre != "All") {
                    Text(
                        text = "Filtered by $selectedGenre",
                        color = FlacCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (searchResults.isEmpty() && spotifyOnlineResults.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardBackground,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Outlined.MusicOff, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(40.dp))
                        Text("No matching local tracks found for '$searchQuery'", color = TextSecondary, fontSize = 14.sp)
                        if (searchQuery.isNotBlank()) {
                            Button(
                                onClick = {
                                    viewModel.resolveExternalSpotifyLink(searchQuery)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.ElectricBolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resolve & Play '$searchQuery' with Spotify Metadata", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            items(searchResults) { track ->
                val isPlayingThis = playerState.currentTrack?.id == track.id
                TrackListItem(
                    track = track,
                    isPlayingThisTrack = isPlayingThis,
                    onTrackClick = { viewModel.playTrack(track, searchResults) },
                    onLikeClick = { viewModel.toggleLiked(track) },
                    onDownloadClick = { viewModel.downloadFlacTrack(track) }
                )
            }
        }
    }
}
