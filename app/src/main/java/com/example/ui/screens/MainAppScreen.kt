package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MeldViewModel

@Composable
fun MainAppScreen(
    viewModel: MeldViewModel,
    modifier: Modifier = Modifier
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isPlayerExpanded by viewModel.isPlayerExpanded.collectAsState()
    val playerSheetTab by viewModel.playerSheetTab.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()

    // Handle back button for expanded player and sub-screens
    BackHandler(enabled = isPlayerExpanded || selectedPlaylist != null) {
        if (isPlayerExpanded) {
            viewModel.setPlayerExpanded(false)
        } else if (selectedPlaylist != null) {
            viewModel.selectPlaylist(null)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Scaffold(
            containerColor = DeepBlack,
            bottomBar = {
                Column {
                    // Mini Player (Visible when a track is active and full sheet is closed)
                    if (playerState.currentTrack != null && !isPlayerExpanded) {
                        MiniPlayer(
                            playerState = playerState,
                            onExpandClick = { viewModel.setPlayerExpanded(true) },
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.nextTrack() },
                            onLikeClick = {
                                playerState.currentTrack?.let { viewModel.toggleLiked(it) }
                            }
                        )
                    }

                    // Bottom Navigation Bar
                    NavigationBar(
                        containerColor = DarkSurface,
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MainTab.HOME && selectedPlaylist == null,
                            onClick = {
                                viewModel.selectPlaylist(null)
                                viewModel.selectTab(MainTab.HOME)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "Home"
                                )
                            },
                            label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FlacCyan,
                                selectedTextColor = FlacCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_home")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.EXPLORE && selectedPlaylist == null,
                            onClick = {
                                viewModel.selectPlaylist(null)
                                viewModel.selectTab(MainTab.EXPLORE)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                                    contentDescription = "Explore"
                                )
                            },
                            label = { Text("Explore", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FlacCyan,
                                selectedTextColor = FlacCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_explore")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.LIBRARY && selectedPlaylist == null,
                            onClick = {
                                viewModel.selectPlaylist(null)
                                viewModel.selectTab(MainTab.LIBRARY)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.LIBRARY) Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                    contentDescription = "Library"
                                )
                            },
                            label = { Text("Library", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FlacCyan,
                                selectedTextColor = FlacCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_library")
                        )

                        NavigationBarItem(
                            selected = currentTab == MainTab.EXTENSIONS && selectedPlaylist == null,
                            onClick = {
                                viewModel.selectPlaylist(null)
                                viewModel.selectTab(MainTab.EXTENSIONS)
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MainTab.EXTENSIONS) Icons.Filled.Extension else Icons.Outlined.Extension,
                                    contentDescription = "Extensions"
                                )
                            },
                            label = { Text("Extensions", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = FlacCyan,
                                selectedTextColor = FlacCyan,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = DarkSurfaceElevated
                            ),
                            modifier = Modifier.testTag("nav_extensions")
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (selectedPlaylist != null) {
                    PlaylistDetailScreen(
                        playlist = selectedPlaylist!!,
                        viewModel = viewModel,
                        onBack = { viewModel.selectPlaylist(null) }
                    )
                } else {
                    when (currentTab) {
                        MainTab.HOME -> HomeScreen(viewModel = viewModel)
                        MainTab.EXPLORE -> ExploreScreen(viewModel = viewModel)
                        MainTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                        MainTab.EXTENSIONS -> ExtensionStoreScreen(viewModel = viewModel)
                    }
                }
            }
        }

        // Full Screen Player Animated Overlay Sheet
        AnimatedVisibility(
            visible = isPlayerExpanded && playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)) + fadeOut()
        ) {
            val currentTrack = playerState.currentTrack
            val downloadProgress = currentTrack?.let { activeDownloads[it.id] }

            FullPlayerSheet(
                playerState = playerState,
                activeTab = playerSheetTab,
                onTabSelected = { viewModel.setPlayerSheetTab(it) },
                onDismiss = { viewModel.setPlayerExpanded(false) },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.nextTrack() },
                onPrevious = { viewModel.previousTrack() },
                onSeek = { viewModel.seekTo(it) },
                onToggleShuffle = { viewModel.toggleShuffle() },
                onCycleRepeat = { viewModel.cycleRepeatMode() },
                onToggleLiked = { currentTrack?.let { viewModel.toggleLiked(it) } },
                onDownloadFlac = { currentTrack?.let { viewModel.downloadFlacTrack(it) } },
                onFormatSelected = { viewModel.setAudioQuality(it) },
                onEqualizerChanged = { viewModel.updateEqualizer(it) },
                onSleepTimerSet = { viewModel.setSleepTimer(it) },
                onQueueTrackClick = { viewModel.playTrack(it, playerState.queue) },
                downloadProgress = downloadProgress
            )
        }
    }
}
