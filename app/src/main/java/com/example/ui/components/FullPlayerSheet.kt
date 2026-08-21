package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.*
import com.example.player.PlayerUiState
import com.example.ui.theme.*
import com.example.ui.viewmodel.DownloadProgress
import com.example.ui.viewmodel.PlayerSheetTab
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    playerState: PlayerUiState,
    activeTab: PlayerSheetTab,
    onTabSelected: (PlayerSheetTab) -> Unit,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLiked: () -> Unit,
    onDownloadFlac: () -> Unit,
    onFormatSelected: (AudioFormat) -> Unit,
    onEqualizerChanged: (EqualizerSettings) -> Unit,
    onSleepTimerSet: (Int) -> Unit,
    onQueueTrackClick: (Track) -> Unit,
    downloadProgress: DownloadProgress?,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    Surface(
        color = PlayerBackground,
        modifier = modifier
            .fillMaxSize()
            .testTag("full_player_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("player_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PLAYING FROM SPOTIFLAC",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = playerState.activeExtensionName,
                        color = FlacCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                IconButton(
                    onClick = { showSleepTimerDialog = true },
                    modifier = Modifier.testTag("player_sleep_timer_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (playerState.sleepTimerSecondsRemaining > 0) {
                                Badge(
                                    containerColor = HiResGold,
                                    contentColor = DeepBlack
                                ) {
                                    Text("${playerState.sleepTimerSecondsRemaining / 60}m")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (playerState.sleepTimerSecondsRemaining > 0) HiResGold else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Tab Bar for Full Player Sections
            ScrollableTabRow(
                selectedTabIndex = activeTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = FlacCyan,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == PlayerSheetTab.NOW_PLAYING,
                    onClick = { onTabSelected(PlayerSheetTab.NOW_PLAYING) },
                    text = { Text("Track", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = activeTab == PlayerSheetTab.LYRICS,
                    onClick = { onTabSelected(PlayerSheetTab.LYRICS) },
                    text = { Text("Lyrics", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = activeTab == PlayerSheetTab.QUEUE,
                    onClick = { onTabSelected(PlayerSheetTab.QUEUE) },
                    text = { Text("Queue (${playerState.queue.size})", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = activeTab == PlayerSheetTab.EQUALIZER,
                    onClick = { onTabSelected(PlayerSheetTab.EQUALIZER) },
                    text = { Text("Audiophile EQ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = activeTab == PlayerSheetTab.SOURCE_INFO,
                    onClick = { onTabSelected(PlayerSheetTab.SOURCE_INFO) },
                    text = { Text("Source & FLAC", fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (activeTab) {
                    PlayerSheetTab.NOW_PLAYING -> {
                        NowPlayingTab(
                            track = track,
                            playerState = playerState,
                            onToggleLiked = onToggleLiked,
                            onDownloadFlac = onDownloadFlac,
                            downloadProgress = downloadProgress
                        )
                    }
                    PlayerSheetTab.LYRICS -> {
                        SyncedLyricsTab(
                            lyrics = track.lyrics,
                            currentPositionMs = playerState.currentPositionMs,
                            onLyricClick = { timeMs -> onSeek(timeMs) }
                        )
                    }
                    PlayerSheetTab.QUEUE -> {
                        QueueTab(
                            queue = playerState.queue,
                            currentTrackId = track.id,
                            onTrackClick = onQueueTrackClick
                        )
                    }
                    PlayerSheetTab.EQUALIZER -> {
                        EqualizerTab(
                            settings = playerState.equalizerSettings,
                            onSettingsChanged = onEqualizerChanged
                        )
                    }
                    PlayerSheetTab.SOURCE_INFO -> {
                        SourceInfoTab(
                            track = track,
                            playerState = playerState,
                            onFormatSelected = onFormatSelected,
                            onDownloadFlac = onDownloadFlac,
                            downloadProgress = downloadProgress
                        )
                    }
                }
            }

            // Fixed Bottom Controls Bar (always accessible across tabs)
            PlaybackControlsBar(
                playerState = playerState,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat
            )
        }

        // Sleep timer configuration dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentTimerRemaining = playerState.sleepTimerSecondsRemaining,
                onDismiss = { showSleepTimerDialog = false },
                onSetTimer = { minutes ->
                    onSleepTimerSet(minutes)
                    showSleepTimerDialog = false
                }
            )
        }
    }
}

@Composable
private fun NowPlayingTab(
    track: Track,
    playerState: PlayerUiState,
    onToggleLiked: () -> Unit,
    onDownloadFlac: () -> Unit,
    downloadProgress: DownloadProgress?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // High-Res Artwork with Glowing Aura
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .size(260.dp)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = FlacCyan.copy(alpha = 0.4f),
                    ambientColor = NeonPurple.copy(alpha = 0.3f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(DarkSurfaceElevated)
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = "Full Album Art",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // High-Res Audio Badge Overlay
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = FlacCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${track.bitDepth} / ${track.sampleRate}",
                        color = FlacCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title, Artist, and Favorite/Download Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${track.artist} — ${track.album}",
                    color = TextSecondary,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleLiked,
                    modifier = Modifier.testTag("player_like_button")
                ) {
                    Icon(
                        imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (track.isLiked) NeonPink else TextTertiary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onDownloadFlac,
                    modifier = Modifier.testTag("player_download_button")
                ) {
                    if (downloadProgress != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress.progressPercent / 100f },
                            modifier = Modifier.size(24.dp),
                            color = FlacCyan,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (track.isDownloaded) Icons.Filled.CloudDone else Icons.Outlined.CloudDownload,
                            contentDescription = "Download FLAC",
                            tint = if (track.isDownloaded) FlacCyan else TextTertiary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // SpotiFLAC Stream Source Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (playerState.isPlaying) LosslessGreen else TextTertiary)
                    )
                    Text(
                        text = "Source: ${playerState.activeExtensionName}",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${track.bitrateKbps} kbps FLAC",
                    color = FlacCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SyncedLyricsTab(
    lyrics: List<SyncedLyricLine>,
    currentPositionMs: Long,
    onLyricClick: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    if (lyrics.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Instrumental or Lyrics Loading...", color = TextSecondary, fontSize = 14.sp)
            }
        }
        return
    }

    val activeIndex = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && activeIndex < lyrics.size) {
            coroutineScope.launch {
                listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 40.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val isPast = index < activeIndex

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onLyricClick(line.timestampMs) }
                    .background(if (isActive) DarkSurfaceVariant.copy(alpha = 0.8f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = line.text,
                    color = when {
                        isActive -> FlacCyan
                        isPast -> TextSecondary.copy(alpha = 0.5f)
                        else -> TextPrimary.copy(alpha = 0.8f)
                    },
                    fontSize = if (isActive) 20.sp else 16.sp,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    lineHeight = 26.sp
                )
                if (isActive) {
                    Text(
                        text = formatDuration(line.timestampMs),
                        color = NeonPurple,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QueueTab(
    queue: List<Track>,
    currentTrackId: String,
    onTrackClick: (Track) -> Unit
) {
    if (queue.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Queue is empty", color = TextSecondary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(queue) { index, track ->
            val isCurrent = track.id == currentTrackId
            TrackListItem(
                track = track,
                isPlayingThisTrack = isCurrent,
                onTrackClick = { onTrackClick(track) },
                onLikeClick = {},
                onDownloadClick = {},
                trailingContent = {
                    Text(
                        text = formatDuration(track.durationMs),
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun EqualizerTab(
    settings: EqualizerSettings,
    onSettingsChanged: (EqualizerSettings) -> Unit
) {
    val presets = listOf(
        "Audiophile Hi-Fi", "Bass Punch", "Vocal Clarity", "Electronic", "Rock", "Flat"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        // Master EQ Toggle & Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DSP Audiophile Equalizer",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Switch(
                checked = settings.isEnabled,
                onCheckedChange = { onSettingsChanged(settings.copy(isEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = FlacCyan,
                    checkedTrackColor = DarkSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Preset Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.take(3).forEach { preset ->
                FilterChip(
                    selected = settings.presetName == preset,
                    onClick = {
                        val updatedBands = when (preset) {
                            "Bass Punch" -> listOf(
                                EqualizerBand(0, "60 Hz", 60, 6.0f),
                                EqualizerBand(1, "230 Hz", 230, 4.0f),
                                EqualizerBand(2, "910 Hz", 910, 0.0f),
                                EqualizerBand(3, "3.6 kHz", 3600, 1.0f),
                                EqualizerBand(4, "14 kHz", 14000, 2.0f)
                            )
                            "Vocal Clarity" -> listOf(
                                EqualizerBand(0, "60 Hz", 60, -1.0f),
                                EqualizerBand(1, "230 Hz", 230, 2.0f),
                                EqualizerBand(2, "910 Hz", 910, 5.0f),
                                EqualizerBand(3, "3.6 kHz", 3600, 4.0f),
                                EqualizerBand(4, "14 kHz", 14000, 2.0f)
                            )
                            "Electronic" -> listOf(
                                EqualizerBand(0, "60 Hz", 60, 5.5f),
                                EqualizerBand(1, "230 Hz", 230, 3.0f),
                                EqualizerBand(2, "910 Hz", 910, -1.0f),
                                EqualizerBand(3, "3.6 kHz", 3600, 3.5f),
                                EqualizerBand(4, "14 kHz", 14000, 5.0f)
                            )
                            "Flat" -> settings.bands.map { it.copy(gainDb = 0f) }
                            else -> listOf(
                                EqualizerBand(0, "60 Hz", 60, 3.5f),
                                EqualizerBand(1, "230 Hz", 230, 2.0f),
                                EqualizerBand(2, "910 Hz", 910, 0.0f),
                                EqualizerBand(3, "3.6 kHz", 3600, 2.5f),
                                EqualizerBand(4, "14 kHz", 14000, 4.0f)
                            )
                        }
                        onSettingsChanged(settings.copy(presetName = preset, bands = updatedBands))
                    },
                    label = { Text(preset, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FlacCyan,
                        selectedLabelColor = DeepBlack
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5-Band Sliders
        settings.bands.forEachIndexed { index, band ->
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = band.frequencyRange, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(
                        text = String.format("%+.1f dB", band.gainDb),
                        color = if (band.gainDb > 0) FlacCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = band.gainDb,
                    onValueChange = { newGain ->
                        val updatedBands = settings.bands.toMutableList().apply {
                            this[index] = band.copy(gainDb = newGain)
                        }
                        onSettingsChanged(settings.copy(bands = updatedBands, presetName = "Custom"))
                    },
                    valueRange = -12f..12f,
                    colors = SliderDefaults.colors(
                        thumbColor = FlacCyan,
                        activeTrackColor = FlacCyan,
                        inactiveTrackColor = DarkSurfaceElevated
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bass Boost Rotary & Spatial Audio
        Text("Bass Boost Enhancement", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = settings.bassBoost.toFloat(),
            onValueChange = { onSettingsChanged(settings.copy(bassBoost = it.toInt())) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
        )
    }
}

@Composable
private fun SourceInfoTab(
    track: Track,
    playerState: PlayerUiState,
    onFormatSelected: (AudioFormat) -> Unit,
    onDownloadFlac: () -> Unit,
    downloadProgress: DownloadProgress?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        Text("Streaming Audio Resolution", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        AudioFormat.values().forEach { format ->
            val isSelected = playerState.activeQuality == format
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) DarkSurfaceElevated else DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onFormatSelected(format) }
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) FlacCyan else StrokeColor,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = format.displayName,
                            color = if (isSelected) FlacCyan else TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (format.isLossless) "Bit-perfect uncompressed FLAC audio container" else "Lossy compressed audio stream",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected",
                            tint = FlacCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Technical Stream Details", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = DarkSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Active Extension", playerState.activeExtensionName)
                DetailRow("Codec / Format", track.audioFormat.badgeText)
                DetailRow("Bit Depth", track.bitDepth)
                DetailRow("Sampling Rate", track.sampleRate)
                DetailRow("Bitrate", "${track.bitrateKbps} kbps")
                DetailRow("Channel Layout", "Stereo (2.0)")
                DetailRow("Storage Status", if (track.isDownloaded) "Downloaded to Local Cache" else "Live Cloud Stream")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onDownloadFlac,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (downloadProgress != null) {
                Text("Downloading FLAC: ${downloadProgress.progressPercent}%", fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (track.isDownloaded) "FLAC Cached On Device" else "Download Full FLAC (28.5 MB)", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PlaybackControlsBar(
    playerState: PlayerUiState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderDragPosition by remember { mutableStateOf(0f) }

    val currentPosition = if (isDraggingSlider) {
        sliderDragPosition.toLong()
    } else {
        playerState.currentPositionMs
    }

    val maxDuration = playerState.durationMs.coerceAtLeast(1L)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        // Progress Slider
        Slider(
            value = currentPosition.toFloat(),
            onValueChange = {
                isDraggingSlider = true
                sliderDragPosition = it
            },
            onValueChangeFinished = {
                isDraggingSlider = false
                onSeek(sliderDragPosition.toLong())
            },
            valueRange = 0f..maxDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = FlacCyan,
                activeTrackColor = FlacCyan,
                inactiveTrackColor = DarkSurfaceElevated
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Timestamp row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPosition),
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = formatDuration(playerState.durationMs),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Playback Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = onToggleShuffle,
                modifier = Modifier.testTag("player_shuffle_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (playerState.isShuffle) FlacCyan else TextTertiary,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.testTag("player_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play / Pause Circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(FlacCyan, NeonPurple)
                        )
                    )
                    .clickable { onPlayPause() }
                    .testTag("player_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                if (playerState.isBuffering) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = DeepBlack,
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = DeepBlack,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            // Next
            IconButton(
                onClick = onNext,
                modifier = Modifier.testTag("player_next_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Next Track",
                    tint = TextPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = onCycleRepeat,
                modifier = Modifier.testTag("player_repeat_button")
            ) {
                val (tint, icon) = when (playerState.repeatMode) {
                    RepeatMode.OFF -> Pair(TextTertiary, Icons.Filled.Repeat)
                    RepeatMode.ALL -> Pair(FlacCyan, Icons.Filled.Repeat)
                    RepeatMode.ONE -> Pair(NeonPink, Icons.Filled.RepeatOne)
                }
                Icon(
                    imageVector = icon,
                    contentDescription = "Repeat",
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun SleepTimerDialog(
    currentTimerRemaining: Int,
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit
) {
    val options = listOf(15, 30, 45, 60, 90)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = HiResGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Audiophile Sleep Timer", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (currentTimerRemaining > 0) "Current timer: ${currentTimerRemaining / 60}m remaining" else "Choose timer duration to pause playback automatically:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                options.forEach { minutes ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(minutes) }
                    ) {
                        Text(
                            text = "$minutes Minutes",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
                if (currentTimerRemaining > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonPink.copy(alpha = 0.2f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetTimer(0) }
                    ) {
                        Text(
                            text = "Turn Off Sleep Timer",
                            color = NeonPink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = FlacCyan)
            }
        }
    )
}
