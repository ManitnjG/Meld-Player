package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUrl: String,
    val audioUrl: String,
    val flacAudioUrl: String,
    val bitDepth: String = "24-bit",
    val sampleRate: String = "96 kHz",
    val bitrateKbps: Int = 1411,
    val audioFormat: AudioFormat = AudioFormat.FLAC_24BIT,
    val lyrics: List<SyncedLyricLine> = emptyList(),
    val sourceExtension: String = "SpotiFLAC Lossless",
    val isLiked: Boolean = false,
    val isDownloaded: Boolean = false,
    val localFilePath: String? = null,
    val genre: String = "Pop",
    val releaseYear: String = "2024"
)

enum class AudioFormat(val displayName: String, val badgeText: String, val isLossless: Boolean) {
    FLAC_24BIT("FLAC 24-bit / 96kHz Hi-Res", "HI-RES FLAC", true),
    FLAC_16BIT("FLAC 16-bit / 44.1kHz Lossless", "LOSSLESS", true),
    AAC_320K("AAC 320 kbps High Quality", "AAC 320K", false),
    OPUS_160K("Opus 160 kbps Standard", "OPUS", false)
}

data class SyncedLyricLine(
    val timestampMs: Long,
    val text: String
)

data class AudioExtension(
    val id: String,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val iconName: String,
    val repositoryUrl: String,
    val isEnabled: Boolean = true,
    val isPrimarySource: Boolean = false,
    val supportedFormats: List<String> = listOf("FLAC 24-bit", "FLAC 16-bit", "AAC 320k"),
    val latencyMs: Int = 45,
    val isInstalled: Boolean = true,
    val type: ExtensionType = ExtensionType.AUDIO_SOURCE
)

enum class ExtensionType(val label: String) {
    AUDIO_SOURCE("Audio Stream Source"),
    METADATA("Metadata & Search"),
    LYRICS("Lyrics Provider"),
    DOWNLOAD_ENGINE("Download Engine")
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val coverUrl: String,
    val trackIds: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class EqualizerBand(
    val bandIndex: Int,
    val frequencyRange: String,
    val centerFreqHz: Int,
    val gainDb: Float // -12dB to +12dB
)

data class EqualizerSettings(
    val isEnabled: Boolean = true,
    val presetName: String = "Audiophile Hi-Fi",
    val bassBoost: Int = 60, // 0 - 100
    val virtualizer: Int = 40, // 0 - 100
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, "60 Hz", 60, 3.5f),
        EqualizerBand(1, "230 Hz", 230, 2.0f),
        EqualizerBand(2, "910 Hz", 910, 0.0f),
        EqualizerBand(3, "3.6 kHz", 3600, 2.5f),
        EqualizerBand(4, "14 kHz", 14000, 4.0f)
    )
)

enum class RepeatMode {
    OFF, ALL, ONE
}
