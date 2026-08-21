package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AudioFormat
import com.example.model.Track
import com.example.ui.theme.*

@Composable
fun FlacBadge(
    format: AudioFormat,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderBrush) = when (format) {
        AudioFormat.FLAC_24BIT -> Triple(
            Color(0xFF002B36),
            FlacCyan,
            Brush.horizontalGradient(listOf(FlacCyan, NeonPurple))
        )
        AudioFormat.FLAC_16BIT -> Triple(
            Color(0xFF06281E),
            LosslessGreen,
            Brush.horizontalGradient(listOf(LosslessGreen, FlacCyan))
        )
        AudioFormat.AAC_320K -> Triple(
            Color(0xFF2D2305),
            HiResGold,
            Brush.horizontalGradient(listOf(HiResGold, NeonPink))
        )
        AudioFormat.OPUS_160K -> Triple(
            Color(0xFF1E293B),
            TextSecondary,
            Brush.horizontalGradient(listOf(StrokeColor, StrokeColor))
        )
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = format.badgeText,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun AnimatedEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = FlacCyan,
    barCount: Int = 4,
    maxHeight: Dp = 18.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(450, easing = LinearEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "h4"
    )

    val heights = listOf(h1, h2, h3, h4)

    Row(
        modifier = modifier.height(maxHeight),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val factor = if (isPlaying) heights[i % heights.size] else 0.2f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(factor)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
fun TrackListItem(
    track: Track,
    isPlayingThisTrack: Boolean,
    onTrackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onTrackClick() }
            .background(if (isPlayingThisTrack) DarkSurfaceVariant.copy(alpha = 0.7f) else Color.Transparent)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album Art
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceElevated)
        ) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = "${track.title} cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (isPlayingThisTrack) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedEqualizerBars(isPlaying = true, barColor = FlacCyan)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = track.title,
                color = if (isPlayingThisTrack) FlacCyan else TextPrimary,
                fontSize = 15.sp,
                fontWeight = if (isPlayingThisTrack) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FlacBadge(format = track.audioFormat)
                Text(
                    text = "${track.artist} • ${track.album}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Actions
        if (trailingContent != null) {
            trailingContent()
        } else {
            IconButton(
                onClick = onLikeClick,
                modifier = Modifier.size(36.dp).testTag("track_like_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (track.isLiked) NeonPink else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(
                onClick = onDownloadClick,
                modifier = Modifier.size(36.dp).testTag("track_download_${track.id}")
            ) {
                Icon(
                    imageVector = if (track.isDownloaded) Icons.Filled.CloudDone else Icons.Outlined.CloudDownload,
                    contentDescription = "Download FLAC",
                    tint = if (track.isDownloaded) FlacCyan else TextTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
