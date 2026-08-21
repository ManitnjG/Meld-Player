package com.example

import com.example.data.DefaultData
import com.example.extensions.SpotiFlacEngine
import com.example.model.AudioFormat
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testTamilTracksCatalogLoaded() {
        val tracks = DefaultData.sampleTracks
        assertTrue("Catalog should contain at least 10 tracks", tracks.size >= 10)

        val tamilTracks = tracks.filter {
            it.genre.contains("Tamil", ignoreCase = true) ||
            it.genre.contains("Kollywood", ignoreCase = true) ||
            it.artist.contains("Rahman", ignoreCase = true) ||
            it.artist.contains("Anirudh", ignoreCase = true)
        }
        assertTrue("Should contain Tamil/Kollywood tracks", tamilTracks.isNotEmpty())

        val naaReady = tracks.find { it.id == "trk-naa-ready" }
        assertNotNull("Naa Ready track should exist", naaReady)
        assertEquals("Naa Ready (From 'Leo')", naaReady?.title)
        assertEquals(AudioFormat.FLAC_24BIT, naaReady?.audioFormat)
        assertEquals("24-bit", naaReady?.bitDepth)
        assertEquals("96.0 kHz", naaReady?.sampleRate)
        assertTrue("Lyrics should be present", (naaReady?.lyrics?.size ?: 0) > 0)

        val chinnaAasai = tracks.find { it.id == "trk-chinna-chinna-aasai" }
        assertNotNull("Chinna Chinna Aasai should exist", chinnaAasai)
        assertEquals("A.R. Rahman, Minmini", chinnaAasai?.artist)
    }

    @Test
    fun testTamilPlaylistsIntegrity() {
        val playlists = DefaultData.samplePlaylists
        val kollywoodPlaylist = playlists.find { it.id == "pl-tamil-kollywood-flac" }
        assertNotNull("Kollywood playlist should exist", kollywoodPlaylist)
        assertTrue("Kollywood playlist should have tracks", (kollywoodPlaylist?.trackIds?.size ?: 0) >= 5)

        val rahmanPlaylist = playlists.find { it.id == "pl-ar-rahman-hires" }
        assertNotNull("A.R. Rahman playlist should exist", rahmanPlaylist)
    }

    @Test
    fun testSpotiFlacEngineSpotifyUrlParsing() {
        val testUrl = "https://open.spotify.com/track/4cOdK2wGLETKBW3PvgPWqT"
        val isSpotify = SpotiFlacEngine.isSpotifyUrl(testUrl)
        assertTrue("Should recognize valid Spotify track URL", isSpotify)

        val trackId = SpotiFlacEngine.extractSpotifyId(testUrl)
        assertEquals("4cOdK2wGLETKBW3PvgPWqT", trackId)

        val genericQuery = "Anirudh Hukum 24bit"
        assertFalse("Generic text should not be a Spotify URL", SpotiFlacEngine.isSpotifyUrl(genericQuery))
    }

    @Test
    fun testSpotiFlacEngineFlacResolution() = kotlinx.coroutines.runBlocking {
        val engine = SpotiFlacEngine()
        val result = engine.resolveLosslessAudio("https://open.spotify.com/track/sample123", null)
        assertNotNull("Resolved stream must not be null", result.streamUrl)
        assertEquals(AudioFormat.FLAC_24BIT, result.resolvedFormat)
        assertEquals("24-bit", result.bitDepth)
        assertEquals("96.0 kHz", result.sampleRate)
    }

    @Test
    fun testSpotifyCatalogSearchAndDynamicResolution() = kotlinx.coroutines.runBlocking {
        val engine = SpotiFlacEngine()
        
        // Search by query
        val results = engine.searchSpotifyCatalog("Blinding Lights", null)
        assertTrue("Spotify search should return results", results.isNotEmpty())
        val firstResult = results.first()
        assertTrue("Title should contain search term", firstResult.title.contains("Blinding", ignoreCase = true))
        assertEquals(AudioFormat.FLAC_24BIT, firstResult.audioFormat)
        assertNotNull(firstResult.coverUrl)

        // Resolve arbitrary song title
        val dynamicTrack = engine.resolveTrackFromSpotify("Starboy The Weeknd", null)
        assertNotNull(dynamicTrack)
        assertTrue(dynamicTrack.title.contains("Starboy", ignoreCase = true))
        assertTrue(dynamicTrack.lyrics.isNotEmpty())
        assertEquals("24-bit", dynamicTrack.bitDepth)
    }

    @Test
    fun testPlaybackHistoryEntityCreation() {
        val history = com.example.data.local.PlaybackHistoryEntity(
            historyId = 1L,
            trackId = "trk-naa-ready",
            title = "Naa Ready",
            artist = "Anirudh Ravichander",
            album = "Leo",
            coverUrl = "https://example.com/cover.jpg",
            playedAt = System.currentTimeMillis(),
            sourceExtension = "SpotiFLAC Universal Source",
            format = "24-BIT FLAC"
        )

        assertEquals("trk-naa-ready", history.trackId)
        assertEquals("Naa Ready", history.title)
        assertEquals("24-BIT FLAC", history.format)
        assertTrue(history.playedAt > 0)
    }
}

