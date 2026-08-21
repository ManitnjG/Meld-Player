package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.net.Uri
import android.util.Log
import com.example.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class PlayerUiState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<Track> = emptyList(),
    val queueIndex: Int = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val activeQuality: AudioFormat = AudioFormat.FLAC_24BIT,
    val activeExtensionName: String = "SpotiFLAC Lossless",
    val equalizerSettings: EqualizerSettings = EqualizerSettings(),
    val sleepTimerSecondsRemaining: Int = 0,
    val currentLyricIndex: Int = -1,
    val playbackSpeed: Float = 1.0f
)

class MeldAudioPlayer(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        releasePlayer()
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            setOnPreparedListener { mp ->
                _playerState.value = _playerState.value.copy(
                    isBuffering = false,
                    isPlaying = true,
                    durationMs = mp.duration.toLong()
                )
                mp.start()
                startProgressTracker()
            }
            setOnCompletionListener {
                handleTrackCompletion()
            }
            setOnErrorListener { _, what, extra ->
                Log.e("MeldAudioPlayer", "Error during playback: what=$what, extra=$extra")
                _playerState.value = _playerState.value.copy(isBuffering = false, isPlaying = false)
                true
            }
            setOnBufferingUpdateListener { _, percent ->
                // Handled in progress tracker if needed
            }
        }
        setupEqualizer()
    }

    private fun setupEqualizer() {
        try {
            mediaPlayer?.audioSessionId?.let { sessionId ->
                if (sessionId != 0) {
                    equalizer = Equalizer(0, sessionId).apply {
                        enabled = true
                    }
                    bassBoost = BassBoost(0, sessionId).apply {
                        enabled = true
                    }
                    applyEqualizerSettings(_playerState.value.equalizerSettings)
                }
            }
        } catch (e: Exception) {
            Log.w("MeldAudioPlayer", "Equalizer not supported on this device/session: ${e.message}")
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null, sourceExtension: String? = null) {
        val queue = newQueue ?: if (_playerState.value.queue.isEmpty()) listOf(track) else _playerState.value.queue
        val index = queue.indexOfFirst { it.id == track.id }.let { if (it == -1) 0 else it }

        _playerState.value = _playerState.value.copy(
            currentTrack = track,
            queue = queue,
            queueIndex = index,
            isBuffering = true,
            currentPositionMs = 0L,
            durationMs = track.durationMs,
            activeQuality = track.audioFormat,
            activeExtensionName = sourceExtension ?: track.sourceExtension,
            currentLyricIndex = -1
        )

        try {
            mediaPlayer?.reset()
            val uriToPlay = if (track.isDownloaded && track.localFilePath != null) {
                Uri.fromFile(File(track.localFilePath))
            } else {
                Uri.parse(track.audioUrl)
            }
            mediaPlayer?.setDataSource(context, uriToPlay)
            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e("MeldAudioPlayer", "Failed to start track: ${e.message}")
            _playerState.value = _playerState.value.copy(isBuffering = false)
        }
    }

    fun togglePlayPause() {
        val player = mediaPlayer ?: return
        val currentTrack = _playerState.value.currentTrack ?: return

        if (player.isPlaying) {
            player.pause()
            _playerState.value = _playerState.value.copy(isPlaying = false)
            stopProgressTracker()
        } else {
            if (_playerState.value.currentPositionMs > 0 && player.currentPosition > 0) {
                player.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressTracker()
            } else {
                playTrack(currentTrack, _playerState.value.queue)
            }
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
        updateLyricIndex(positionMs)
    }

    fun next() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            state.queue.indices.filter { it != state.queueIndex }.randomOrNull() ?: 0
        } else {
            (state.queueIndex + 1) % state.queue.size
        }

        val nextTrack = state.queue.getOrNull(nextIndex) ?: return
        playTrack(nextTrack, state.queue, state.activeExtensionName)
    }

    fun previous() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if (state.currentPositionMs > 3000) {
            seekTo(0)
            return
        }

        val prevIndex = if (state.queueIndex - 1 < 0) state.queue.size - 1 else state.queueIndex - 1
        val prevTrack = state.queue.getOrNull(prevIndex) ?: return
        playTrack(prevTrack, state.queue, state.activeExtensionName)
    }

    fun toggleShuffle() {
        _playerState.value = _playerState.value.copy(isShuffle = !_playerState.value.isShuffle)
    }

    fun cycleRepeatMode() {
        val newMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = newMode)
    }

    fun setAudioQuality(format: AudioFormat) {
        _playerState.value = _playerState.value.copy(activeQuality = format)
        // If current track is playing, seamlessly re-resolve bitrate
    }

    fun setSourceExtension(extensionName: String) {
        _playerState.value = _playerState.value.copy(activeExtensionName = extensionName)
    }

    fun updateEqualizerSettings(newSettings: EqualizerSettings) {
        _playerState.value = _playerState.value.copy(equalizerSettings = newSettings)
        applyEqualizerSettings(newSettings)
    }

    private fun applyEqualizerSettings(settings: EqualizerSettings) {
        try {
            equalizer?.let { eq ->
                eq.enabled = settings.isEnabled
                if (settings.isEnabled) {
                    val numBands = eq.numberOfBands.toInt()
                    val minLevel = eq.bandLevelRange[0]
                    val maxLevel = eq.bandLevelRange[1]

                    settings.bands.forEachIndexed { index, band ->
                        if (index < numBands) {
                            // Map -12dB..+12dB to minLevel..maxLevel
                            val level = ((band.gainDb / 12f) * maxLevel).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())
                            eq.setBandLevel(index.toShort(), level.toShort())
                        }
                    }
                }
            }

            bassBoost?.let { bb ->
                bb.enabled = settings.isEnabled
                if (settings.isEnabled) {
                    bb.setStrength((settings.bassBoost * 10).toShort().coerceIn(0, 1000))
                }
            }
        } catch (e: Exception) {
            Log.w("MeldAudioPlayer", "Error applying EQ: ${e.message}")
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _playerState.value = _playerState.value.copy(sleepTimerSecondsRemaining = 0)
            return
        }

        val totalSeconds = minutes * 60
        _playerState.value = _playerState.value.copy(sleepTimerSecondsRemaining = totalSeconds)

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _playerState.value = _playerState.value.copy(sleepTimerSecondsRemaining = remaining)
            }
            // Stop playback when timer hits 0
            if (_playerState.value.isPlaying) {
                togglePlayPause()
            }
            _playerState.value = _playerState.value.copy(sleepTimerSecondsRemaining = 0)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val current = player.currentPosition.toLong()
                        val duration = if (player.duration > 0) player.duration.toLong() else _playerState.value.durationMs
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = current,
                            durationMs = duration
                        )
                        updateLyricIndex(current)
                    }
                }
                delay(200)
            }
        }
    }

    private fun updateLyricIndex(currentPositionMs: Long) {
        val lyrics = _playerState.value.currentTrack?.lyrics ?: return
        if (lyrics.isEmpty()) return

        var activeIndex = -1
        for (i in lyrics.indices) {
            if (lyrics[i].timestampMs <= currentPositionMs) {
                activeIndex = i
            } else {
                break
            }
        }
        if (activeIndex != _playerState.value.currentLyricIndex) {
            _playerState.value = _playerState.value.copy(currentLyricIndex = activeIndex)
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
    }

    private fun handleTrackCompletion() {
        when (_playerState.value.repeatMode) {
            RepeatMode.ONE -> {
                seekTo(0)
                mediaPlayer?.start()
                _playerState.value = _playerState.value.copy(isPlaying = true)
                startProgressTracker()
            }
            RepeatMode.ALL -> {
                next()
            }
            RepeatMode.OFF -> {
                if (_playerState.value.queueIndex < _playerState.value.queue.size - 1) {
                    next()
                } else {
                    _playerState.value = _playerState.value.copy(isPlaying = false, currentPositionMs = 0)
                    stopProgressTracker()
                }
            }
        }
    }

    private fun releasePlayer() {
        try {
            equalizer?.release()
            bassBoost?.release()
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        equalizer = null
        bassBoost = null
    }

    fun release() {
        stopProgressTracker()
        sleepTimerJob?.cancel()
        releasePlayer()
        scope.cancel()
    }
}
