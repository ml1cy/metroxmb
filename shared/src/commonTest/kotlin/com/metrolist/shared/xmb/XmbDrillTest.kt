package com.metrolist.shared.xmb

import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.BrowseItem
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.playQueueFrom
import com.metrolist.shared.xmb.adapters.song
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbOption
import com.metrolist.shared.xmb.nav.XmbOptionsPanel
import com.metrolist.shared.xmb.nav.XmbSelection
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.xmbOptionsFor
import com.metrolist.shared.xmb.theme.XmbIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun song(
    id: String,
    album: AlbumTiny? = AlbumTiny("MPREb_$id", "Album $id"),
    artistId: String? = "UC$id",
    unavailable: Boolean = false,
) = SongItem(
    id = id,
    title = "Song $id",
    thumbnail = "https://img.example/$id.jpg",
    artists = listOf(ArtistTiny(artistId, "Artist $id")),
    album = album,
    isUnavailable = unavailable,
)

class XmbDrillTest {
    private val counts = listOf(1, 9, 8)
    private val level = XmbLevel(XmbList.Liked, "Liked", XmbIcon.Vector(XmbIcons.Visualizer))

    @Test
    fun levelsMoveWithinTheirRowsAndCloseWithBackOrLeft() {
        val opened = XmbNavigation.initial(counts, 2).push(level)
        val down = opened.reduce(XmbAction.DOWN, counts, levelSize = 3)
        assertEquals(XmbFeedback.MOVE_DOWN, down.feedback)
        assertEquals(1, down.navigation.level?.focus)
        val bottom =
            down.navigation
                .reduce(XmbAction.DOWN, counts, 3)
                .navigation
                .reduce(XmbAction.DOWN, counts, 3)
        assertEquals(XmbFeedback.NONE, bottom.feedback)
        assertEquals(2, bottom.navigation.level?.focus)
        assertEquals(XmbFeedback.NONE, opened.reduce(XmbAction.RIGHT, counts, 3).feedback)
        for (close in listOf(XmbAction.BACK, XmbAction.LEFT)) {
            val step = down.navigation.reduce(close, counts, 3)
            assertEquals(XmbFeedback.BACK, step.feedback)
            assertTrue(step.navigation.stack.isEmpty())
            assertEquals(2, step.navigation.category, "Closing a level keeps the category")
        }
    }

    @Test
    fun selectAndOptionsReportTheFocusedRow() {
        val navigation = XmbNavigation.initial(counts, 2).push(level.copy(focus = 2)).push(level.copy(focus = 1))
        val select = navigation.reduce(XmbAction.SELECT, counts, levelSize = 4)
        assertEquals(XmbSelection(category = 2, item = 1, depth = 2), select.selected)
        assertTrue(navigation.reduce(XmbAction.OPTIONS, counts, 4).optionsRequested)
        assertEquals(XmbFeedback.ERROR, navigation.reduce(XmbAction.SELECT, counts, 0).feedback)
        assertFalse(navigation.reduce(XmbAction.OPTIONS, counts, 0).optionsRequested)
        assertEquals(2, navigation.pop().level?.focus, "The parent level keeps its own focus")
    }

    @Test
    fun levelFocusFollowsAListThatShrinks() {
        val navigation = XmbNavigation.initial(counts, 2).push(level.copy(focus = 5))
        assertEquals(2, navigation.clampLevel(3).level?.focus)
        assertEquals(0, navigation.clampLevel(0).level?.focus)
        assertEquals(navigation, navigation.clampLevel(9))
    }

    @Test
    fun itemsBecomeRowsThatPlayOrOpen() {
        val track = song("a")
        assertEquals(XmbEntryKind.SONG, track.toXmbEntry().kind)
        assertEquals("Artist a · Album a", track.toXmbEntry().subtitle)
        assertNull(track.toXmbEntry().open)
        assertEquals(track, track.toXmbEntry().song)

        val album = AlbumItem("MPREb_x", "Album", "https://img/x", listOf(ArtistTiny("UC1", "Band")), year = "1999")
        assertEquals(XmbList.Album(album), album.toXmbEntry().open)
        assertEquals("Band · 1999", album.toXmbEntry().subtitle)
        assertEquals(XmbList.Artist(ArtistItem("UC1", "Band", null)), ArtistItem("UC1", "Band", null).toXmbEntry().open)

        val liked = PlaylistItem("auto:liked", "Liked", thumbnail = "auto_playlist_icon_liked")
        assertNull(liked.toXmbEntry().artwork, "Icon names in thumbnail fields aren't artwork")
        for (path in listOf("/covers/1.png", "file:/covers/1.png", "C:\\covers\\1.png", "https://img/1.png")) {
            assertEquals(path, PlaylistItem("local", "Mine", thumbnail = path, isLocal = true).toXmbEntry().artwork)
        }

        val episode = EpisodeItem("ep", "Episode", null).toXmbEntry()
        assertEquals("ep", episode.song?.id, "Episodes play as songs")
        assertEquals(XmbEntryKind.BROWSE, BrowseItem("FEmusic_moods", "Moods").toXmbEntry().kind)
        assertNull(BrowseItem("FEmusic_moods", "Moods").toXmbEntry().open)

        val section = HomeSection("Quick picks", listOf(track)).toXmbEntry(3)
        assertEquals(XmbList.Section(HomeSection("Quick picks", listOf(track)), 3), section.open)
        assertEquals(track.thumbnail, section.artwork)
    }

    @Test
    fun playingARowQueuesThePlayableRowsAroundIt() {
        val rows =
            listOf(
                song("a").toXmbEntry(),
                AlbumItem("MPREb_x", "Album", null, emptyList()).toXmbEntry(),
                song("b", unavailable = true).toXmbEntry(),
                song("c").toXmbEntry(),
            )
        val (queue, start) = checkNotNull(rows.playQueueFrom(3))
        assertEquals(listOf("a", "c"), queue.map { it.id })
        assertEquals(1, start)
        assertNull(rows.playQueueFrom(1), "Albums open instead of playing")
        assertEquals(listOf("b") to 0, rows.playQueueFrom(2)?.let { (songs, index) -> songs.map { it.id } to index })
    }

    @Test
    fun optionsDependOnTheRow() {
        val track = song("a").toXmbEntry()
        assertEquals(
            listOf(
                XmbOption.PLAY,
                XmbOption.PLAY_NEXT,
                XmbOption.ADD_TO_QUEUE,
                XmbOption.START_RADIO,
                XmbOption.LIKE,
                XmbOption.DOWNLOAD,
                XmbOption.VIEW_ALBUM,
                XmbOption.VIEW_ARTIST,
            ),
            xmbOptionsFor(track, liked = false, downloaded = false, canDownload = true),
        )
        val likedDownloaded = xmbOptionsFor(track, liked = true, downloaded = true, canDownload = true)
        assertTrue(XmbOption.UNLIKE in likedDownloaded && XmbOption.REMOVE_DOWNLOAD in likedDownloaded)
        val bare =
            xmbOptionsFor(song("b", album = null, artistId = null).toXmbEntry(), liked = false, downloaded = false, canDownload = false)
        assertFalse(XmbOption.DOWNLOAD in bare || XmbOption.VIEW_ALBUM in bare || XmbOption.VIEW_ARTIST in bare)
        assertEquals(
            listOf(XmbOption.PLAY, XmbOption.SHUFFLE, XmbOption.PLAY_NEXT, XmbOption.ADD_TO_QUEUE),
            xmbOptionsFor(AlbumItem("x", "A", null, emptyList()).toXmbEntry(), liked = false, downloaded = false, canDownload = true),
        )
        assertTrue(xmbOptionsFor(BrowseItem("b", "B").toXmbEntry(), false, false, true).isEmpty())
    }

    @Test
    fun optionsPanelMovesChoosesAndCloses() {
        val panel = XmbOptionsPanel(song("a").toXmbEntry(), listOf(XmbOption.PLAY, XmbOption.PLAY_NEXT))
        assertEquals(XmbFeedback.NONE, panel.reduce(XmbAction.UP).feedback)
        val down = panel.reduce(XmbAction.DOWN)
        assertEquals(1, down.panel?.focus)
        assertEquals(XmbFeedback.NONE, checkNotNull(down.panel).reduce(XmbAction.DOWN).feedback)
        val chosen = checkNotNull(down.panel).reduce(XmbAction.SELECT)
        assertEquals(XmbOption.PLAY_NEXT, chosen.chosen)
        assertNull(chosen.panel)
        for (close in listOf(XmbAction.BACK, XmbAction.OPTIONS, XmbAction.LEFT)) {
            val step = panel.reduce(close)
            assertNull(step.panel)
            assertNull(step.chosen)
            assertEquals(XmbFeedback.BACK, step.feedback)
        }
    }
}
