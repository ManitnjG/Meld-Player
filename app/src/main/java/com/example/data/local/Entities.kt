package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String,
    val audioUrl: String,
    val flacAudioUrl: String,
    val bitDepth: String,
    val sampleRate: String,
    val bitrateKbps: Int,
    val audioFormat: String,
    val lyricsJson: String,
    val sourceExtension: String,
    val isLiked: Boolean,
    val isDownloaded: Boolean,
    val localFilePath: String?,
    val genre: String,
    val releaseYear: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val trackIdsCsv: String,
    val isCustom: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "extensions")
data class ExtensionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val iconName: String,
    val repositoryUrl: String,
    val isEnabled: Boolean,
    val isPrimarySource: Boolean,
    val supportedFormatsCsv: String,
    val latencyMs: Int,
    val isInstalled: Boolean,
    val type: String
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val coverUrl: String,
    val playedAt: Long = System.currentTimeMillis(),
    val sourceExtension: String,
    val format: String
)
