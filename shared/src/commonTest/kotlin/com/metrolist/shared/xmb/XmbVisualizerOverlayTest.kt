package com.metrolist.shared.xmb

import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.xmb.screens.artistAlbumLine
import com.metrolist.shared.xmb.screens.codecBadge
import com.metrolist.shared.xmb.screens.formatPlaybackClock
import com.metrolist.shared.xmb.screens.queueCounter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class XmbVisualizerOverlayTest {
    @Test
    fun clockIsHoursMinutesSecondsLikeTheConsole() {
        assertEquals("00:01:50", formatPlaybackClock(110_400))
        assertEquals("00:03:51", formatPlaybackClock(231_000))
        assertEquals("01:02:03", formatPlaybackClock(3_723_000))
        assertEquals("00:00:00", formatPlaybackClock(-5))
    }

    @Test
    fun queueCounterIsOneBasedAndNeedsAQueue() {
        assertEquals("3/10", queueCounter(index = 2, size = 10))
        assertNull(queueCounter(index = -1, size = 10))
        assertNull(queueCounter(index = 0, size = 0))
        assertNull(queueCounter(index = 10, size = 10))
    }

    @Test
    fun codecBadgeNamesTheStream() {
        assertEquals("OPUS", codecBadge("audio/webm; codecs=\"opus\"", null))
        assertEquals("AAC", codecBadge("audio/mp4", "mp4a.40.2"))
        assertEquals("AAC", codecBadge("audio/mp4", null))
        assertEquals("MP3", codecBadge("audio/mpeg", null))
        assertEquals("FLAC", codecBadge("audio/flac", null))
        assertNull(codecBadge(null, null))
        assertNull(codecBadge("audio/webm", null))
    }

    @Test
    fun artistAndAlbumAreJoinedWithASlash() {
        val song = SongItem("s", "Talking to Myself", null, listOf(ArtistTiny("a", "LINKIN PARK")), AlbumTiny("b", "One More Light"))
        assertEquals("LINKIN PARK / One More Light", artistAlbumLine(song))
        assertEquals("LINKIN PARK", artistAlbumLine(song.copy(album = null)))
        assertEquals("Custom / One More Light", artistAlbumLine(song.copy(customArtist = "Custom")))
        assertNull(artistAlbumLine(song.copy(artists = emptyList(), album = null)))
    }
}
