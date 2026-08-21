package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AudioExtension
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.components.FlacBadge
import com.example.ui.components.TrackListItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeldViewModel

@Composable
fun HomeScreen(
    viewModel: MeldViewModel,
    modifier: Modifier = Modifier
) {
    val allTracks by viewModel.allTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val extensions by viewModel.allExtensions.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayedTracks.collectAsState()

    var selectedCategory by remember { mutableStateOf("All") }
    val categories = listOf("All", "Tamil Hits", "Hi-Res FLAC", "Kollywood", "Cyberpunk", "Jazz & Soul", "Lo-Fi", "Cinematic")

    val filteredTracks = remember(allTracks, selectedCategory) {
        when (selectedCategory) {
            "All" -> allTracks
            "Tamil Hits", "Kollywood" -> allTracks.filter {
                it.genre.contains("Tamil", ignoreCase = true) ||
                it.genre.contains("Kollywood", ignoreCase = true) ||
                it.artist.contains("Rahman", ignoreCase = true) ||
                it.artist.contains("Anirudh", ignoreCase = true) ||
                it.artist.contains("Ilaiyaraaja", ignoreCase = true)
            }
            "Hi-Res FLAC" -> allTracks.filter { it.bitDepth == "24-bit" }
            else -> allTracks.filter { it.genre.contains(selectedCategory.take(4), ignoreCase = true) }
        }
    }

    val primaryExtension = extensions.find { it.isPrimarySource && it.isEnabled } ?: extensions.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // App Header & Extension Status
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "MELD FLAC",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Lossless Audio Client",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Extension pill button
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceElevated,
                    modifier = Modifier.clickable {
                        viewModel.selectTab(com.example.ui.viewmodel.MainTab.EXTENSIONS)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(LosslessGreen)
                        )
                        Text(
                            text = primaryExtension?.name ?: "SpotiFLAC Active",
                            color = FlacCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Hero Featured High-Res Album Banner
        item {
            val heroTrack = allTracks.firstOrNull()
            if (heroTrack != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.playTrack(heroTrack, allTracks) }
                        .testTag("hero_album_banner")
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = heroTrack.coverUrl,
                            contentDescription = "Hero Album",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            DeepBlack.copy(alpha = 0.92f),
                                            DeepBlack.copy(alpha = 0.65f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = FlacCyan
                                ) {
                                    Text(
                                        text = "SPOTIFLAC 24-BIT MASTER",
                                        color = DeepBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = heroTrack.album,
                                    color = TextPrimary,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${heroTrack.artist} • ${heroTrack.genre}",
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.playTrack(heroTrack, allTracks) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = FlacCyan,
                                        contentColor = DeepBlack
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play Lossless", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    text = "${heroTrack.bitrateKbps} kbps FLAC",
                                    color = HiResGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category, fontSize = 12.sp) },
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

        // Persistent 'Recently Played' Section (Stored in Room SQLite Database)
        if (recentlyPlayed.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = null,
                                tint = FlacCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Recently Played",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(
                            onClick = {
                                viewModel.selectTab(com.example.ui.viewmodel.MainTab.LIBRARY)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "View All (${recentlyPlayed.size})",
                                color = FlacCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyPlayed) { track ->
                            val isPlaying = playerState.currentTrack?.id == track.id
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isPlaying) CardBackground.copy(alpha = 0.9f) else DarkSurface,
                                border = if (isPlaying) androidx.compose.foundation.BorderStroke(1.dp, FlacCyan) else null,
                                modifier = Modifier
                                    .width(135.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.playTrack(track, recentlyPlayed) }
                                    .testTag("recent_track_${track.id}")
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(119.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DarkSurfaceElevated)
                                    ) {
                                        AsyncImage(
                                            model = track.coverUrl,
                                            contentDescription = track.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Bit-depth badge overlay
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Black.copy(alpha = 0.75f),
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                        ) {
                                            Text(
                                                text = track.bitDepth,
                                                color = if (track.bitDepth == "24-bit") HiResGold else FlacCyan,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }

                                        // Play state overlay / Quick play button
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isPlaying) FlacCyan else DeepBlack.copy(alpha = 0.65f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying && playerState.isPlaying) Icons.Filled.GraphicEq else Icons.Filled.PlayArrow,
                                                contentDescription = "Play",
                                                tint = if (isPlaying) DeepBlack else Color.White,
                                                modifier = Modifier
                                                    .padding(5.dp)
                                                    .fillMaxSize()
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = track.title,
                                        color = if (isPlaying) FlacCyan else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = track.artist,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trending Tracks Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Trending Lossless Tracks",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${filteredTracks.size} songs",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        items(filteredTracks) { track ->
            val isCurrent = playerState.currentTrack?.id == track.id
            TrackListItem(
                track = track,
                isPlayingThisTrack = isCurrent,
                onTrackClick = { viewModel.playTrack(track, filteredTracks) },
                onLikeClick = { viewModel.toggleLiked(track) },
                onDownloadClick = { viewModel.downloadFlacTrack(track) }
            )
        }

        // Curated Lossless Playlists Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Curated Hi-Res Playlists",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(playlists) { playlist ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardBackground,
                        modifier = Modifier
                            .width(160.dp)
                            .clickable { viewModel.selectPlaylist(playlist) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                AsyncImage(
                                    model = playlist.coverUrl,
                                    contentDescription = playlist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "${playlist.trackIds.size} Tracks",
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = playlist.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = playlist.description,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
