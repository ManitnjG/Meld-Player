package com.example.data.repository

import com.example.data.DefaultData
import com.example.data.local.*
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MeldRepository(
    private val database: MeldDatabase
) {
    private val trackDao = database.trackDao()
    private val playlistDao = database.playlistDao()
    private val extensionDao = database.extensionDao()
    private val historyDao = database.historyDao()

    init {
        // Initialize default seed data if DB is freshly created
        CoroutineScope(Dispatchers.IO).launch {
            seedDatabaseIfEmpty()
        }
    }

    private suspend fun seedDatabaseIfEmpty() {
        val trackEntities = DefaultData.sampleTracks.map { it.toEntity() }
        trackDao.insertTracks(trackEntities)

        // Seed default extensions
        val extensionEntities = DefaultData.sampleExtensions.map { it.toEntity() }
        extensionDao.insertExtensions(extensionEntities)

        // Seed default playlists
        val playlistEntities = DefaultData.samplePlaylists.map { it.toEntity() }
        playlistDao.insertPlaylists(playlistEntities)
    }

    // Tracks Flows
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks().map { list -> list.map { it.toDomain() } }
    val likedTracks: Flow<List<Track>> = trackDao.getLikedTracks().map { list -> list.map { it.toDomain() } }
    val downloadedTracks: Flow<List<Track>> = trackDao.getDownloadedTracks().map { list -> list.map { it.toDomain() } }

    // Playlists Flow
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists().map { list -> list.map { it.toDomain() } }

    // Extensions Flow
    val allExtensions: Flow<List<AudioExtension>> = extensionDao.getAllExtensions().map { list -> list.map { it.toDomain() } }

    // History Flow
    val playbackHistory: Flow<List<PlaybackHistoryEntity>> = historyDao.getRecentHistory()

    suspend fun getTrackById(id: String): Track? {
        return trackDao.getTrackById(id)?.toDomain()
    }

    suspend fun toggleLiked(trackId: String, currentStatus: Boolean) {
        trackDao.updateLikedStatus(trackId, !currentStatus)
    }

    suspend fun markAsDownloaded(trackId: String, localPath: String) {
        trackDao.updateDownloadStatus(trackId, true, localPath)
    }

    suspend fun removeDownload(trackId: String) {
        trackDao.updateDownloadStatus(trackId, false, null)
    }

    suspend fun saveTrack(track: Track) {
        trackDao.insertTrack(track.toEntity())
    }

    suspend fun createPlaylist(name: String, description: String, initialTrackIds: List<String> = emptyList()) {
        val newPlaylist = Playlist(
            id = "pl-custom-" + System.currentTimeMillis(),
            name = name,
            description = description,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&auto=format&fit=crop&q=80",
            trackIds = initialTrackIds,
            isCustom = true
        )
        playlistDao.insertPlaylist(newPlaylist.toEntity())
    }

    suspend fun deletePlaylist(id: String) {
        playlistDao.deletePlaylistById(id)
    }

    suspend fun updateExtensionStatus(id: String, isEnabled: Boolean) {
        extensionDao.updateExtensionStatus(id, isEnabled)
    }

    suspend fun setPrimaryAudioSource(id: String) {
        extensionDao.setPrimaryAudioSource(id)
    }

    suspend fun installExtension(extension: AudioExtension) {
        extensionDao.insertExtension(extension.toEntity())
    }

    suspend fun deleteExtension(id: String) {
        extensionDao.deleteExtensionById(id)
    }

    suspend fun recordHistory(track: Track) {
        historyDao.recordHistory(
            PlaybackHistoryEntity(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                coverUrl = track.coverUrl,
                sourceExtension = track.sourceExtension,
                format = track.audioFormat.badgeText
            )
        )
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun deleteHistoryEntry(id: Long) {
        historyDao.deleteHistoryEntryById(id)
    }

    // Mapping Helpers
    private fun Track.toEntity(): TrackEntity {
        val lyricsJsonArray = JSONArray()
        lyrics.forEach { line ->
            val obj = JSONObject()
            obj.put("time", line.timestampMs)
            obj.put("text", line.text)
            lyricsJsonArray.put(obj)
        }

        return TrackEntity(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            coverUrl = coverUrl,
            audioUrl = audioUrl,
            flacAudioUrl = flacAudioUrl,
            bitDepth = bitDepth,
            sampleRate = sampleRate,
            bitrateKbps = bitrateKbps,
            audioFormat = audioFormat.name,
            lyricsJson = lyricsJsonArray.toString(),
            sourceExtension = sourceExtension,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath,
            genre = genre,
            releaseYear = releaseYear
        )
    }

    private fun TrackEntity.toDomain(): Track {
        val parsedLyrics = mutableListOf<SyncedLyricLine>()
        try {
            val array = JSONArray(lyricsJson)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                parsedLyrics.add(SyncedLyricLine(obj.getLong("time"), obj.getString("text")))
            }
        } catch (_: Exception) {}

        val format = try {
            AudioFormat.valueOf(audioFormat)
        } catch (_: Exception) {
            AudioFormat.FLAC_24BIT
        }

        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            coverUrl = coverUrl,
            audioUrl = audioUrl,
            flacAudioUrl = flacAudioUrl,
            bitDepth = bitDepth,
            sampleRate = sampleRate,
            bitrateKbps = bitrateKbps,
            audioFormat = format,
            lyrics = parsedLyrics,
            sourceExtension = sourceExtension,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            localFilePath = localFilePath,
            genre = genre,
            releaseYear = releaseYear
        )
    }

    private fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        trackIdsCsv = trackIds.joinToString(","),
        isCustom = isCustom,
        createdAt = createdAt
    )

    private fun PlaylistEntity.toDomain(): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        coverUrl = coverUrl,
        trackIds = if (trackIdsCsv.isBlank()) emptyList() else trackIdsCsv.split(","),
        isCustom = isCustom,
        createdAt = createdAt
    )

    private fun AudioExtension.toEntity(): ExtensionEntity = ExtensionEntity(
        id = id,
        name = name,
        version = version,
        author = author,
        description = description,
        iconName = iconName,
        repositoryUrl = repositoryUrl,
        isEnabled = isEnabled,
        isPrimarySource = isPrimarySource,
        supportedFormatsCsv = supportedFormats.joinToString(","),
        latencyMs = latencyMs,
        isInstalled = isInstalled,
        type = type.name
    )

    private fun ExtensionEntity.toDomain(): AudioExtension = AudioExtension(
        id = id,
        name = name,
        version = version,
        author = author,
        description = description,
        iconName = iconName,
        repositoryUrl = repositoryUrl,
        isEnabled = isEnabled,
        isPrimarySource = isPrimarySource,
        supportedFormats = if (supportedFormatsCsv.isBlank()) emptyList() else supportedFormatsCsv.split(","),
        latencyMs = latencyMs,
        isInstalled = isInstalled,
        type = try { ExtensionType.valueOf(type) } catch (_: Exception) { ExtensionType.AUDIO_SOURCE }
    )
}
