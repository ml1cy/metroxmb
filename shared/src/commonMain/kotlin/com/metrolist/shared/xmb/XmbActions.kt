package com.metrolist.shared.xmb

import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_album
import com.metrolist.shared.generated.resources.xmb_item_artist
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.xmb.adapters.XmbEntry
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.icon
import com.metrolist.shared.xmb.adapters.playQueueFrom
import com.metrolist.shared.xmb.adapters.song
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbOption
import com.metrolist.shared.xmb.nav.albumId
import com.metrolist.shared.xmb.nav.artistId
import com.metrolist.shared.xmb.nav.xmbOptionsFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** What selecting rows and options does, through the host's [XmbServices]. */
internal class XmbActions(
    private val services: XmbServices,
    private val scope: CoroutineScope,
) {
    /** Plays the list from row [index]; false if that row isn't a song. */
    fun playFrom(
        entries: List<XmbEntry>,
        index: Int,
    ): Boolean {
        val (songs, start) = entries.playQueueFrom(index) ?: return false
        services.playback.playQueue(songs, start)
        return true
    }

    fun options(entry: XmbEntry): List<XmbOption> {
        val song = entry.song
        return xmbOptionsFor(
            entry = entry,
            liked = song != null && services.isLiked(song.id),
            downloaded =
                song != null && services.downloads
                    ?.downloadedSongIds
                    ?.value
                    ?.contains(song.id) == true,
            canDownload = services.downloads != null,
        )
    }

    /** Runs [option] for row [index] of [entries]; View album and View artist return the level to open. */
    fun run(
        option: XmbOption,
        entries: List<XmbEntry>,
        index: Int,
    ): XmbLevel? {
        val entry = entries.getOrNull(index) ?: return null
        val song = entry.song
        val playback = services.playback
        when (option) {
            XmbOption.PLAY -> if (song != null) playFrom(entries, index) else withSongs(entry) { songs -> playCollection(entry, songs) }
            XmbOption.SHUFFLE -> withSongs(entry) { songs -> playback.shuffleQueue(songs, songsOnly = entry.kind == XmbEntryKind.ARTIST) }
            XmbOption.PLAY_NEXT -> if (song != null) playback.playNext(listOf(song)) else withSongs(entry, playback::playNext)
            XmbOption.ADD_TO_QUEUE -> if (song != null) playback.addToQueue(listOf(song)) else withSongs(entry, playback::addToQueue)
            XmbOption.START_RADIO -> song?.let(playback::startRadio)
            XmbOption.LIKE, XmbOption.UNLIKE -> song?.let(services.toggleLike)
            XmbOption.DOWNLOAD -> song?.let { services.downloads?.download(it) }
            XmbOption.REMOVE_DOWNLOAD -> song?.let { services.downloads?.remove(it) }
            XmbOption.VIEW_ALBUM -> return song?.albumLevel()
            XmbOption.VIEW_ARTIST -> return song?.artistLevel()
        }
        return null
    }

    private fun playCollection(
        entry: XmbEntry,
        songs: List<SongItem>,
    ) {
        when (entry.kind) {
            XmbEntryKind.ALBUM -> services.playback.playQueue(songs)
            XmbEntryKind.ARTIST -> services.playback.play(songs.first(), songs, songsOnly = true)
            else -> services.playback.play(songs.first(), songs)
        }
    }

    /** Loads a collection row's songs through the same list the XMB would open for it. */
    private fun withSongs(
        entry: XmbEntry,
        action: (List<SongItem>) -> Unit,
    ) {
        val list = entry.open ?: return
        scope.launch {
            val songs =
                try {
                    services.music
                        .open(list)
                        .first { !it.isLoading }
                        .entries
                        .mapNotNull { it.song }
                        .filterNot { it.isUnavailable }
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    emptyList()
                }
            if (songs.isNotEmpty()) action(songs)
        }
    }
}

/** The level a row opens. */
internal fun XmbEntry.toLevel(): XmbLevel? = open?.let { XmbLevel(it, title, icon, artwork) }

internal fun SongItem.albumLevel(): XmbLevel? {
    val id = albumId() ?: return null
    val album = AlbumItem(id = id, title = album?.name.orEmpty(), thumbnail = thumbnail, artists = artists)
    return XmbLevel(XmbList.Album(album), album.title, XmbIcon.Drawable(Res.drawable.xmb_item_album), thumbnail)
}

internal fun SongItem.artistLevel(): XmbLevel? {
    val id = artistId() ?: return null
    val artist = artists.first { it.id == id }
    val item = ArtistItem(id = id, title = artist.name, thumbnail = artist.thumbnail)
    return XmbLevel(XmbList.Artist(item), item.title, XmbIcon.Drawable(Res.drawable.xmb_item_artist), artist.thumbnail)
}
