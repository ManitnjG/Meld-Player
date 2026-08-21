package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PlaybackHistoryEntity
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.components.TrackListItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeldViewModel

enum class LibraryTab {
    LIKED, DOWNLOADS, PLAYLISTS, HISTORY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MeldViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(LibraryTab.LIKED) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    val likedTracks by viewModel.likedTracks.collectAsState()
    val downloadedTracks by viewModel.downloadedTracks.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val history by viewModel.playbackHistory.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayedTracks.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // Library Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Library",
                        color = TextPrimary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Lossless collection & offline downloads",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (selectedTab == LibraryTab.PLAYLISTS) {
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.testTag("create_playlist_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddCircle,
                            contentDescription = "Create Playlist",
                            tint = FlacCyan,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }

        // Library Section Tabs
        item {
            PrimaryTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = DarkSurfaceElevated,
                contentColor = FlacCyan,
                modifier = Modifier.clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = selectedTab == LibraryTab.LIKED,
                    onClick = { selectedTab = LibraryTab.LIKED },
                    text = { Text("Liked (${likedTracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == LibraryTab.DOWNLOADS,
                    onClick = { selectedTab = LibraryTab.DOWNLOADS },
                    text = { Text("FLAC Offline (${downloadedTracks.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == LibraryTab.PLAYLISTS,
                    onClick = { selectedTab = LibraryTab.PLAYLISTS },
                    text = { Text("Playlists (${playlists.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == LibraryTab.HISTORY,
                    onClick = { selectedTab = LibraryTab.HISTORY },
                    text = { Text("History", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        when (selectedTab) {
            LibraryTab.LIKED -> {
                if (likedTracks.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.FavoriteBorder,
                            title = "No Liked Songs Yet",
                            description = "Heart songs while listening to save them to your lossless collection."
                        )
                    }
                } else {
                    item {
                        Button(
                            onClick = { viewModel.playTrack(likedTracks.first(), likedTracks) },
                            colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play All Liked Tracks (${likedTracks.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                    items(likedTracks) { track ->
                        val isPlaying = playerState.currentTrack?.id == track.id
                        TrackListItem(
                            track = track,
                            isPlayingThisTrack = isPlaying,
                            onTrackClick = { viewModel.playTrack(track, likedTracks) },
                            onLikeClick = { viewModel.toggleLiked(track) },
                            onDownloadClick = { viewModel.downloadFlacTrack(track) }
                        )
                    }
                }
            }

            LibraryTab.DOWNLOADS -> {
                item {
                    val totalMb = downloadedTracks.size * 28.5f
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Offline High-Res FLAC Storage", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Cached for bit-perfect offline listening", color = TextSecondary, fontSize = 12.sp)
                            }
                            Text(
                                text = String.format("%.1f MB", totalMb),
                                color = FlacCyan,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (downloadedTracks.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.CloudDownload,
                            title = "No Downloaded Tracks",
                            description = "Tap the download icon on any song to cache bit-perfect FLAC audio locally."
                        )
                    }
                } else {
                    items(downloadedTracks) { track ->
                        val isPlaying = playerState.currentTrack?.id == track.id
                        TrackListItem(
                            track = track,
                            isPlayingThisTrack = isPlaying,
                            onTrackClick = { viewModel.playTrack(track, downloadedTracks) },
                            onLikeClick = { viewModel.toggleLiked(track) },
                            onDownloadClick = { viewModel.removeDownload(track) },
                            trailingContent = {
                                IconButton(onClick = { viewModel.removeDownload(track) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete download", tint = NeonPink)
                                }
                            }
                        )
                    }
                }
            }

            LibraryTab.PLAYLISTS -> {
                items(playlists) { playlist ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectPlaylist(playlist) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceElevated)
                            ) {
                                AsyncImage(
                                    model = playlist.coverUrl,
                                    contentDescription = playlist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = playlist.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${playlist.trackIds.size} tracks • ${playlist.description}",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (playlist.isCustom) {
                                IconButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete Playlist", tint = TextTertiary)
                                }
                            }
                        }
                    }
                }
            }

            LibraryTab.HISTORY -> {
                if (history.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Outlined.History,
                            title = "No Playback History Yet",
                            description = "Songs you play from Home, Explore, or SpotiFLAC will be recorded to your persistent Room database here."
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${history.size} listening events logged",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (recentlyPlayed.isNotEmpty()) {
                                    Button(
                                        onClick = { viewModel.playTrack(recentlyPlayed.first(), recentlyPlayed) },
                                        colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showClearHistoryDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    items(history, key = { it.historyId }) { entry ->
                        val isCurrent = playerState.currentTrack?.id == entry.trackId
                        val diffMs = remember(entry.playedAt) { System.currentTimeMillis() - entry.playedAt }
                        val timeString = remember(diffMs) {
                            val seconds = diffMs / 1000
                            val minutes = seconds / 60
                            val hours = minutes / 60
                            val days = hours / 24
                            when {
                                seconds < 60 -> "Just now"
                                minutes < 60 -> "${minutes}m ago"
                                hours < 24 -> "${hours}h ago"
                                days == 1L -> "Yesterday"
                                else -> "${days}d ago"
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) CardBackground.copy(alpha = 0.9f) else DarkSurface,
                            border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.dp, FlacCyan.copy(alpha = 0.6f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.playHistoryEntry(entry) }
                                .testTag("history_item_${entry.historyId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceElevated)
                                ) {
                                    AsyncImage(
                                        model = entry.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    if (isCurrent && playerState.isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(DeepBlack.copy(alpha = 0.4f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.GraphicEq,
                                                contentDescription = "Playing",
                                                tint = FlacCyan,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entry.title,
                                        color = if (isCurrent) FlacCyan else TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${entry.artist} • ${entry.album}",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = timeString,
                                            color = TextTertiary,
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "• ${entry.sourceExtension}",
                                            color = LosslessGreen,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DarkSurfaceElevated
                                ) {
                                    Text(
                                        text = entry.format,
                                        color = if (entry.format.contains("24-BIT") || entry.format.contains("FLAC")) FlacCyan else HiResGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteHistoryEntry(entry.historyId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Delete from history",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, desc ->
                viewModel.createPlaylist(name, desc)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Listening History?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all saved playback history records from your local Room database. Your saved and liked tracks will remain intact.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceElevated
        )
    }
}

@Composable
private fun EmptyStateView(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text("Create Lossless Playlist", color = TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlacCyan,
                        unfocusedBorderColor = StrokeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlacCyan,
                        unfocusedBorderColor = StrokeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
