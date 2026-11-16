package com.metrolist.shared.xmb

import com.metrolist.shared.db.entities.PlaylistEntity
import com.metrolist.shared.db.entities.PlaylistSongMap
import com.metrolist.shared.db.entities.SongEntity
import com.metrolist.shared.db.getDesktopDatabaseBuilder
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.HomeSection
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.xmb.adapters.DefaultXmbMusicSource
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.song
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbMusicSourceTest {
    @Test
    fun listsComeFromTheDatabaseOrTheRepository() =
        runBlocking {
            val path = Files.createTempFile("xmb-music", ".db")
            Files.deleteIfExists(path)
            val database = getDesktopDatabaseBuilder(path.toFile()).build()
            Settings.replaceSharedForTest(Settings(MapSettings()))
            try {
                val dao = database.dao()
                dao.insert(SongEntity("old", "Old", liked = true, likedDate = LocalDateTime(2026, 1, 1, 0, 0)))
                dao.insert(SongEntity("new", "New", liked = true, likedDate = LocalDateTime(2026, 2, 1, 0, 0)))
                dao.insert(SongEntity("kept", "Kept", isDownloaded = true, dateDownload = LocalDateTime(2026, 3, 1, 0, 0)))
                dao.insert(PlaylistEntity(id = "mine", name = "Mine", isLocal = true))
                dao.insert(PlaylistSongMap(playlistId = "mine", songId = "kept", position = 0))
                val requested = mutableListOf<String>()
                val source =
                    DefaultXmbMusicSource(database, this) { item ->
                        requested += item.id
                        if (item.id == "broken") error("offline") else listOf(song("remote"))
                    }

                suspend fun ids(list: XmbList): List<String?> =
                    source
                        .open(list)
                        .first { !it.isLoading }
                        .entries
                        .map { it.song?.id }

                assertEquals(listOf("new", "old"), ids(XmbList.Liked), "Newest likes first")
                assertEquals(listOf("kept"), ids(XmbList.Downloads))
                assertEquals(listOf("kept"), ids(XmbList.Playlist(PlaylistItem("mine", "Mine", null, isLocal = true))))
                assertEquals(listOf("new", "old"), ids(XmbList.Playlist(PlaylistItem("auto:liked", "Liked", null))))
                assertEquals(
                    XmbListState.loaded(emptyList()),
                    source.open(XmbList.Playlist(PlaylistItem("auto:top100", "Top", null))).first(),
                )
                assertTrue(requested.isEmpty(), "Local lists never touch the network")

                assertEquals(listOf("remote"), ids(XmbList.Album(AlbumItem("MPREb_1", "Album", null, emptyList()))))
                assertEquals(listOf("remote"), ids(XmbList.Playlist(PlaylistItem("PLremote", "Remote", null))))
                assertEquals(
                    XmbListState.failed,
                    source.open(XmbList.Album(AlbumItem("broken", "Broken", null, emptyList()))).first { !it.isLoading },
                )

                val section = HomeSection("Shelf", listOf(song("s1"), AlbumItem("MPREb_2", "A", null, emptyList())))
                assertEquals(section.items.map { it.toXmbEntry() }, source.open(XmbList.Section(section, 0)).first().entries)
            } finally {
                Settings.replaceSharedForTest(null)
                database.close()
                Files.deleteIfExists(path)
            }
        }
}
