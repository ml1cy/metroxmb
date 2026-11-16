package com.metrolist.shared.xmb

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.playback.PlaybackUiState
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.XmbMusicSource
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.nav.XmbOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class XmbActionsTest {
    private class Recorder : PlaybackController {
        override val state = MutableStateFlow(PlaybackUiState())
        val calls = mutableListOf<String>()

        override fun play(
            song: SongItem,
            queue: List<SongItem>,
            songsOnly: Boolean,
        ) {
            calls += "play ${song.id} [${queue.joinToString(",") { it.id }}] songsOnly=$songsOnly"
        }

        override fun playQueue(
            songs: List<SongItem>,
            startIndex: Int,
            queueTitle: String?,
        ) {
            calls += "queue [${songs.joinToString(",") { it.id }}] from $startIndex"
        }

        override fun shuffleQueue(
            songs: List<SongItem>,
            queueTitle: String?,
            songsOnly: Boolean,
        ) {
            calls += "shuffle [${songs.joinToString(",") { it.id }}]"
        }

        override fun togglePlayPause() {
            calls += "toggle"
        }

        override fun seekTo(positionMs: Long) = Unit

        override fun skipToNext() = Unit

        override fun skipToPrevious() = Unit

        override fun setPlaybackSpeed(speed: Float) = Unit

        override fun startRadio(song: SongItem) {
            calls += "radio ${song.id}"
        }

        override fun playNext(songs: List<SongItem>) {
            calls += "next [${songs.joinToString(",") { it.id }}]"
        }

        override fun addToQueue(songs: List<SongItem>) {
            calls += "add [${songs.joinToString(",") { it.id }}]"
        }
    }

    private val album = AlbumItem("MPREb_1", "Album", null, listOf(ArtistTiny("UC1", "Band")))
    private val albumSongs = listOf(song("x"), song("y", unavailable = true), song("z"))
    private val source =
        XmbMusicSource { list ->
            when (list) {
                is XmbList.Album -> flowOf(XmbListState(), XmbListState.loaded(albumSongs.map { it.toXmbEntry() }))
                else -> flowOf(XmbListState.failed)
            }
        }

    @Test
    fun selectingASongPlaysItsListFromThatRow() {
        val recorder = Recorder()
        val actions = XmbActions(XmbServices(recorder, source), kotlinx.coroutines.MainScope())
        val rows = listOf(song("a"), song("b"), song("c")).map { it.toXmbEntry() }
        assertTrue(actions.playFrom(rows, 1))
        assertFalse(actions.playFrom(listOf(album.toXmbEntry()), 0))
        assertEquals(listOf("queue [a,b,c] from 1"), recorder.calls)
    }

    @Test
    fun songOptionsActOnTheRow() {
        val recorder = Recorder()
        val liked = mutableListOf<String>()
        val services = XmbServices(recorder, source, isLiked = { it in liked }, toggleLike = { liked += it.id })
        val actions = XmbActions(services, kotlinx.coroutines.MainScope())
        val rows = listOf(song("a"), song("b")).map { it.toXmbEntry() }
        assertNull(actions.run(XmbOption.PLAY_NEXT, rows, 1))
        actions.run(XmbOption.ADD_TO_QUEUE, rows, 0)
        actions.run(XmbOption.START_RADIO, rows, 0)
        actions.run(XmbOption.LIKE, rows, 0)
        assertEquals(listOf("next [b]", "add [a]", "radio a"), recorder.calls)
        assertEquals(listOf("a"), liked)
        assertTrue(XmbOption.UNLIKE in actions.options(rows[0]))
        assertFalse(XmbOption.DOWNLOAD in actions.options(rows[0]), "No downloads without a platform implementation")
    }

    @Test
    fun viewAlbumAndArtistOpenTheirLevels() {
        val actions = XmbActions(XmbServices(Recorder(), source), kotlinx.coroutines.MainScope())
        val rows = listOf(song("a").toXmbEntry())
        val albumLevel = checkNotNull(actions.run(XmbOption.VIEW_ALBUM, rows, 0))
        assertEquals("MPREb_a", (albumLevel.list as XmbList.Album).album.id)
        assertEquals("Album a", albumLevel.title)
        val artistLevel = checkNotNull(actions.run(XmbOption.VIEW_ARTIST, rows, 0))
        assertEquals(XmbList.Artist(ArtistItem("UCa", "Artist a", null)), artistLevel.list)
    }

    @Test
    fun collectionOptionsLoadTheCollectionThroughItsList() =
        runTest(UnconfinedTestDispatcher()) {
            val recorder = Recorder()
            val actions = XmbActions(XmbServices(recorder, source), this)
            val rows = listOf(album.toXmbEntry())
            actions.run(XmbOption.PLAY, rows, 0)
            actions.run(XmbOption.SHUFFLE, rows, 0)
            actions.run(XmbOption.PLAY_NEXT, rows, 0)
            assertEquals(listOf("queue [x,z] from 0", "shuffle [x,z]", "next [x,z]"), recorder.calls)
        }
}
