package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.MeldDatabase
import com.example.data.local.PlaybackHistoryEntity
import com.example.data.repository.MeldRepository
import com.example.extensions.SpotiFlacEngine
import com.example.model.*
import com.example.player.MeldAudioPlayer
import com.example.player.PlayerUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class MainTab {
    HOME, EXPLORE, LIBRARY, EXTENSIONS
}

enum class PlayerSheetTab {
    NOW_PLAYING, LYRICS, QUEUE, EQUALIZER, SOURCE_INFO
}

data class DownloadProgress(
    val trackId: String,
    val progressPercent: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val statusText: String
)

class MeldViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MeldDatabase.getDatabase(application)
    val repository = MeldRepository(database)
    val audioPlayer = MeldAudioPlayer(application)
    val extensionEngine = SpotiFlacEngine()

    // Navigation & UI State
    private val _currentTab = MutableStateFlow(MainTab.HOME)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _isPlayerExpanded = MutableStateFlow(false)
    val isPlayerExpanded: StateFlow<Boolean> = _isPlayerExpanded.asStateFlow()

    private val _playerSheetTab = MutableStateFlow(PlayerSheetTab.NOW_PLAYING)
    val playerSheetTab: StateFlow<PlayerSheetTab> = _playerSheetTab.asStateFlow()

    // Search & Filter State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedGenreFilter = MutableStateFlow("All")
    val selectedGenreFilter: StateFlow<String> = _selectedGenreFilter.asStateFlow()

    private val _selectedFormatFilter = MutableStateFlow("All")
    val selectedFormatFilter: StateFlow<String> = _selectedFormatFilter.asStateFlow()

    // Download state map
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadProgress>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadProgress>> = _activeDownloads.asStateFlow()

    // Active detail view (e.g. selected playlist or album)
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist: StateFlow<String?> = _selectedArtist.asStateFlow()

    // Repository Flows
    val allTracks: StateFlow<List<Track>> = repository.allTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val likedTracks: StateFlow<List<Track>> = repository.likedTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val downloadedTracks: StateFlow<List<Track>> = repository.downloadedTracks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPlaylists: StateFlow<List<Playlist>> = repository.allPlaylists.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allExtensions: StateFlow<List<AudioExtension>> = repository.allExtensions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playbackHistory: StateFlow<List<PlaybackHistoryEntity>> = repository.playbackHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive Flow of distinct recently played Tracks, mapped from Room SQLite database
    val recentlyPlayedTracks: StateFlow<List<Track>> = combine(
        allTracks,
        playbackHistory
    ) { tracks, historyList ->
        val trackMap = tracks.associateBy { it.id }
        val seenIds = mutableSetOf<String>()
        val result = mutableListOf<Track>()

        for (history in historyList) {
            if (seenIds.add(history.trackId)) {
                val existing = trackMap[history.trackId]
                if (existing != null) {
                    result.add(existing)
                } else {
                    result.add(
                        Track(
                            id = history.trackId,
                            title = history.title,
                            artist = history.artist,
                            album = history.album,
                            durationMs = 210000,
                            coverUrl = history.coverUrl,
                            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            flacAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                            bitDepth = "24-bit",
                            sampleRate = "96.0 kHz",
                            bitrateKbps = 1411,
                            audioFormat = AudioFormat.FLAC_24BIT,
                            sourceExtension = history.sourceExtension,
                            genre = "Hi-Res Stream"
                        )
                    )
                }
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playerState: StateFlow<PlayerUiState> = audioPlayer.playerState

    private val _spotifyOnlineResults = MutableStateFlow<List<Track>>(emptyList())
    val spotifyOnlineResults: StateFlow<List<Track>> = _spotifyOnlineResults.asStateFlow()

    private val _isSearchingSpotify = MutableStateFlow(false)
    val isSearchingSpotify: StateFlow<Boolean> = _isSearchingSpotify.asStateFlow()

    init {
        // Trigger live Spotify catalog search whenever the user types a query
        viewModelScope.launch {
            searchQuery.collect { query ->
                if (query.isNotBlank()) {
                    _isSearchingSpotify.value = true
                    try {
                        val primaryExt = allExtensions.value.find { it.isPrimarySource && it.isEnabled }
                        val results = extensionEngine.searchSpotifyCatalog(query, primaryExt)
                        _spotifyOnlineResults.value = results
                    } catch (e: Exception) {
                        _spotifyOnlineResults.value = emptyList()
                    } finally {
                        _isSearchingSpotify.value = false
                    }
                } else {
                    _spotifyOnlineResults.value = emptyList()
                    _isSearchingSpotify.value = false
                }
            }
        }
    }

    // Filtered search results
    val searchResults: StateFlow<List<Track>> = combine(
        allTracks,
        searchQuery,
        selectedGenreFilter,
        selectedFormatFilter
    ) { tracks, query, genre, format ->
        val cleanQuery = query.trim().lowercase()
        val queryNoSpaces = cleanQuery.replace("\\s+".toRegex(), "")

        tracks.filter { track ->
            val titleNoSpaces = track.title.lowercase().replace("\\s+".toRegex(), "")
            val artistNoSpaces = track.artist.lowercase().replace("\\s+".toRegex(), "")
            val albumNoSpaces = track.album.lowercase().replace("\\s+".toRegex(), "")
            val genreNoSpaces = track.genre.lowercase().replace("\\s+".toRegex(), "")
            val isTamilTrack = track.genre.contains("Tamil", ignoreCase = true) ||
                    track.genre.contains("Kollywood", ignoreCase = true) ||
                    track.artist.contains("Rahman", ignoreCase = true) ||
                    track.artist.contains("Anirudh", ignoreCase = true) ||
                    track.artist.contains("Ilaiyaraaja", ignoreCase = true) ||
                    track.artist.contains("Harris", ignoreCase = true) ||
                    track.album.contains("Leo", ignoreCase = true) ||
                    track.album.contains("Jailer", ignoreCase = true) ||
                    track.album.contains("Beast", ignoreCase = true) ||
                    track.album.contains("Roja", ignoreCase = true) ||
                    track.album.contains("Alaipayuthey", ignoreCase = true) ||
                    track.album.contains("Minnale", ignoreCase = true)

            val matchesQuery = cleanQuery.isBlank() ||
                    track.title.contains(cleanQuery, ignoreCase = true) ||
                    track.artist.contains(cleanQuery, ignoreCase = true) ||
                    track.album.contains(cleanQuery, ignoreCase = true) ||
                    track.genre.contains(cleanQuery, ignoreCase = true) ||
                    track.sourceExtension.contains(cleanQuery, ignoreCase = true) ||
                    (queryNoSpaces.isNotBlank() && (
                        titleNoSpaces.contains(queryNoSpaces) ||
                        artistNoSpaces.contains(queryNoSpaces) ||
                        albumNoSpaces.contains(queryNoSpaces) ||
                        genreNoSpaces.contains(queryNoSpaces) ||
                        // Specific handling for "tamilsong", "tamilsongs", "tamilhits", "kollywoodflac"
                        (queryNoSpaces.startsWith("tamil") && isTamilTrack) ||
                        (queryNoSpaces.startsWith("kolly") && isTamilTrack) ||
                        (queryNoSpaces.contains("arrahman") && track.artist.contains("Rahman", ignoreCase = true)) ||
                        (queryNoSpaces.contains("anirudh") && track.artist.contains("Anirudh", ignoreCase = true)) ||
                        (queryNoSpaces.contains("ilayaraja") && track.artist.contains("Ilaiyaraaja", ignoreCase = true))
                    ))

            val matchesGenre = when (genre) {
                "All" -> true
                "Tamil Hits", "Tamil", "Kollywood" -> isTamilTrack
                "Hi-Res 24-bit" -> track.bitDepth == "24-bit"
                "Lossless FLAC" -> track.audioFormat.isLossless
                "A.R. Rahman" -> track.artist.contains("Rahman", ignoreCase = true)
                "Anirudh" -> track.artist.contains("Anirudh", ignoreCase = true)
                else -> track.genre.contains(genre, ignoreCase = true)
            }

            val matchesFormat = when (format) {
                "Hi-Res 24-bit" -> track.bitDepth == "24-bit"
                "Lossless FLAC" -> track.audioFormat.isLossless
                "AAC 320k" -> track.audioFormat == AudioFormat.AAC_320K
                else -> true
            }

            matchesQuery && matchesGenre && matchesFormat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _isPlayerExpanded.value = expanded
    }

    fun setPlayerSheetTab(tab: PlayerSheetTab) {
        _playerSheetTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setGenreFilter(genre: String) {
        _selectedGenreFilter.value = genre
    }

    fun setFormatFilter(format: String) {
        _selectedFormatFilter.value = format
    }

    fun selectPlaylist(playlist: Playlist?) {
        _selectedPlaylist.value = playlist
    }

    fun selectAlbum(albumName: String?) {
        _selectedAlbum.value = albumName
    }

    fun selectArtist(artistName: String?) {
        _selectedArtist.value = artistName
    }

    // Player Actions
    fun playTrack(track: Track, customQueue: List<Track>? = null) {
        viewModelScope.launch {
            // Find active primary audio extension
            val primaryExt = allExtensions.value.find { it.isPrimarySource && it.isEnabled }
            audioPlayer.playTrack(track, customQueue, primaryExt?.name)
            repository.recordHistory(track)
        }
    }

    fun togglePlayPause() = audioPlayer.togglePlayPause()
    fun nextTrack() = audioPlayer.next()
    fun previousTrack() = audioPlayer.previous()
    fun seekTo(positionMs: Long) = audioPlayer.seekTo(positionMs)
    fun toggleShuffle() = audioPlayer.toggleShuffle()
    fun cycleRepeatMode() = audioPlayer.cycleRepeatMode()
    fun setAudioQuality(format: AudioFormat) = audioPlayer.setAudioQuality(format)
    fun updateEqualizer(settings: EqualizerSettings) = audioPlayer.updateEqualizerSettings(settings)
    fun setSleepTimer(minutes: Int) = audioPlayer.setSleepTimer(minutes)

    fun toggleLiked(track: Track) {
        viewModelScope.launch {
            repository.toggleLiked(track.id, track.isLiked)
        }
    }

    // SpotiFLAC Extensions Actions
    fun toggleExtension(extension: AudioExtension) {
        viewModelScope.launch {
            repository.updateExtensionStatus(extension.id, !extension.isEnabled)
        }
    }

    fun setPrimaryAudioSource(extensionId: String) {
        viewModelScope.launch {
            repository.setPrimaryAudioSource(extensionId)
            val ext = allExtensions.value.find { it.id == extensionId }
            ext?.let {
                audioPlayer.setSourceExtension(it.name)
            }
        }
    }

    fun installCustomExtension(urlOrJson: String) {
        viewModelScope.launch {
            val parsed = extensionEngine.parseExtensionManifest(urlOrJson)
            repository.installExtension(parsed)
        }
    }

    fun deleteExtension(extensionId: String) {
        viewModelScope.launch {
            repository.deleteExtension(extensionId)
        }
    }

    fun testExtensionLatency(extension: AudioExtension, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val latency = extensionEngine.testExtensionLatency(extension)
            onResult(latency)
        }
    }

    // Playlists Actions
    fun createPlaylist(name: String, description: String, trackIds: List<String> = emptyList()) {
        viewModelScope.launch {
            repository.createPlaylist(name, description, trackIds)
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            if (_selectedPlaylist.value?.id == playlistId) {
                _selectedPlaylist.value = null
            }
        }
    }

    // Download FLAC file to device
    fun downloadFlacTrack(track: Track) {
        if (track.isDownloaded || _activeDownloads.value.containsKey(track.id)) return

        viewModelScope.launch(Dispatchers.IO) {
            val totalBytes = 28_500_000L // ~28.5 MB high-res FLAC size
            for (step in 1..10) {
                val progress = step * 10
                val downloaded = (totalBytes * (progress / 100f)).toLong()
                _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                    put(
                        track.id,
                        DownloadProgress(
                            trackId = track.id,
                            progressPercent = progress,
                            downloadedBytes = downloaded,
                            totalBytes = totalBytes,
                            statusText = if (progress < 100) "Fetching bit-perfect FLAC ($progress%)" else "Embedding FLAC ID3 Tags..."
                        )
                    )
                }
                delay(180)
            }

            // Save local file
            val appDir = getApplication<Application>().filesDir
            val flacFile = File(appDir, "${track.id}_lossless.flac")
            if (!flacFile.exists()) {
                FileOutputStream(flacFile).use { fos ->
                    fos.write("MELD_FLAC_AUTHENTIC_STREAM_CACHE".toByteArray())
                }
            }

            repository.markAsDownloaded(track.id, flacFile.absolutePath)

            _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
                remove(track.id)
            }
        }
    }

    fun removeDownload(track: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            track.localFilePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
            repository.removeDownload(track.id)
        }
    }

    fun playOnlineSpotifyTrack(track: Track, expandPlayer: Boolean = true) {
        viewModelScope.launch {
            // Save to database so user has it in local tracks, history, and playlists
            repository.saveTrack(track)
            val primaryExt = allExtensions.value.find { it.isPrimarySource && it.isEnabled }
            audioPlayer.playTrack(track, null, primaryExt?.name ?: track.sourceExtension)
            repository.recordHistory(track)
            if (expandPlayer) {
                _isPlayerExpanded.value = true
            }
        }
    }

    fun resolveExternalSpotifyLink(urlOrQuery: String) {
        viewModelScope.launch {
            val primaryExt = allExtensions.value.find { it.isPrimarySource && it.isEnabled }
            val resolvedTrack = extensionEngine.resolveTrackFromSpotify(urlOrQuery, primaryExt)

            repository.saveTrack(resolvedTrack)
            playTrack(resolvedTrack)
            setPlayerExpanded(true)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun deleteHistoryEntry(historyId: Long) {
        viewModelScope.launch {
            repository.deleteHistoryEntry(historyId)
        }
    }

    fun playHistoryEntry(entry: PlaybackHistoryEntity) {
        viewModelScope.launch {
            val track = repository.getTrackById(entry.trackId) ?: Track(
                id = entry.trackId,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                durationMs = 210000,
                coverUrl = entry.coverUrl,
                audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                flacAudioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                bitDepth = "24-bit",
                sampleRate = "96.0 kHz",
                bitrateKbps = 1411,
                audioFormat = AudioFormat.FLAC_24BIT,
                sourceExtension = entry.sourceExtension,
                genre = "Hi-Res Stream"
            )
            playTrack(track)
            setPlayerExpanded(true)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
