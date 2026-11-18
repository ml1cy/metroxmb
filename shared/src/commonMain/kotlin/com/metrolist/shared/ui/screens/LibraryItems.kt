package com.metrolist.shared.ui.screens

import androidx.compose.runtime.key
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.EpisodeItem
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.PodcastItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.SortDirection
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.filterForLibrarySearch
import com.metrolist.shared.model.isPodcast
import com.metrolist.shared.model.isSystemPlaylist
import com.metrolist.shared.settings.HiddenLibraryItem
import com.metrolist.shared.settings.HiddenLibraryItemKey
import com.metrolist.shared.settings.orderLibraryItems
import com.metrolist.shared.ui.components.CollectionPreviews

/** The library's filters, sorting and item order (split from the stock library screen, which XMB Music doesn't show). */
enum class LibraryFilter {
    TRACKS,
    PLAYLISTS,
    ARTISTS,
    ALBUMS,
    PODCASTS,
}

enum class LibrarySortType {
    RECENTLY_ADDED,
    NAME,
    LAST_UPDATED,
    ARTIST,
    YEAR,
    SONG_COUNT,
    LENGTH,
    PLAYTIME,
    MANUAL,
}

internal fun List<YTItem>.filterForLibrary(
    filter: LibraryFilter?,
    hiddenItemKeys: Set<HiddenLibraryItemKey>,
): List<YTItem> =
    filter { item -> (item !is PlaylistItem || !item.isSystemPlaylist()) && HiddenLibraryItem.from(item).key !in hiddenItemKeys }
        .let { visible ->
            when (filter) {
                LibraryFilter.TRACKS -> {
                    visible.filterIsInstance<SongItem>().filterNot { it.isPodcast }
                }

                LibraryFilter.PLAYLISTS -> {
                    visible.filterIsInstance<PlaylistItem>()
                }

                LibraryFilter.ARTISTS -> {
                    visible.filterIsInstance<ArtistItem>()
                }

                LibraryFilter.ALBUMS -> {
                    visible.filterIsInstance<AlbumItem>()
                }

                LibraryFilter.PODCASTS -> {
                    visible.filterIsInstance<PodcastItem>()
                }

                null -> {
                    visible
                }
            }
        }

internal fun sortLibraryItems(
    items: List<YTItem>,
    sort: LibrarySortType,
    manualOrder: String,
    direction: SortDirection = sort.defaultDirection,
): List<YTItem> =
    when (sort) {
        LibrarySortType.RECENTLY_ADDED -> {
            items.sortedByLibraryMetadata(direction, YTItem::libraryAddedAt)
        }

        LibrarySortType.NAME -> {
            items.sortedByLibraryMetadata(direction) { it.title.lowercase() }
        }

        LibrarySortType.LAST_UPDATED -> {
            items.sortedByLibraryMetadata(direction, YTItem::libraryUpdatedAt)
        }

        LibrarySortType.ARTIST -> {
            items.sortedByLibraryMetadata(direction) {
                (it as? AlbumItem)
                    ?.artists
                    ?.firstOrNull()
                    ?.name
                    ?.lowercase()
            }
        }

        LibrarySortType.YEAR -> {
            items.sortedByLibraryMetadata(direction) { (it as? AlbumItem)?.year?.toIntOrNull() }
        }

        LibrarySortType.SONG_COUNT -> {
            items.sortedByLibraryMetadata(direction, ::librarySongCount)
        }

        LibrarySortType.LENGTH -> {
            items.sortedByLibraryMetadata(direction) { (it as? AlbumItem)?.duration }
        }

        LibrarySortType.PLAYTIME -> {
            items.sortedByLibraryMetadata(direction, ::libraryPlaytime)
        }

        LibrarySortType.MANUAL -> {
            orderLibraryItems(items, manualOrder)
        }
    }

internal fun <T : Comparable<T>> List<YTItem>.sortedByLibraryMetadata(
    direction: SortDirection,
    value: (YTItem) -> T?,
): List<YTItem> =
    map { it to value(it) }
        .sortedWith { first, second ->
            val left = first.second
            val right = second.second
            when {
                left == null && right == null -> 0
                left == null -> 1
                right == null -> -1
                direction == SortDirection.ASCENDING -> left.compareTo(right)
                else -> right.compareTo(left)
            }
        }.map { it.first }

internal val LibrarySortType.defaultDirection: SortDirection
    get() =
        if (this in
            setOf(LibrarySortType.RECENTLY_ADDED, LibrarySortType.LAST_UPDATED)
        ) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }

private fun librarySongCount(item: YTItem): Int? =
    when (item) {
        is ArtistItem -> item.songCount
        is AlbumItem -> item.songCount
        is PlaylistItem -> item.songCount?.toIntOrNull()
        else -> null
    }

private fun libraryPlaytime(item: YTItem): Long? =
    when (item) {
        is ArtistItem -> item.playtime
        is AlbumItem -> item.playtime
        is PlaylistItem -> item.playtime
        else -> null
    }

internal fun prepareLibraryItems(
    items: List<YTItem>,
    filter: LibraryFilter?,
    hiddenItemKeys: Set<HiddenLibraryItemKey>,
    sort: LibrarySortType,
    manualOrder: String,
    direction: SortDirection,
    query: String,
    contentMatchedSongs: List<SongItem>,
): List<YTItem> {
    val visibleItems =
        items
            .filterNot { it is EpisodeItem || (it is SongItem && filter != LibraryFilter.TRACKS) }
            .filterForLibrary(filter, hiddenItemKeys)
    val matches =
        if (query.isBlank()) {
            visibleItems
        } else {
            visibleItems.filterForLibrarySearch(query) + contentMatchedSongs.filterForLibrary(filter, hiddenItemKeys)
        }
    return sortLibraryItems(matches.distinctBy { it::class to it.id }, sort, manualOrder, direction)
}

internal fun dispatchLibraryItemClick(
    displayedItems: List<YTItem>,
    index: Int,
    onPlaySongs: (List<SongItem>, Int) -> Unit,
    onOpenItem: (YTItem) -> Unit,
) {
    val item = displayedItems.getOrNull(index) ?: return
    if (item is SongItem) {
        val songs = displayedItems.filterIsInstance<SongItem>()
        onPlaySongs(songs, songs.indexOf(item))
    } else {
        CollectionPreviews.record(item)
        onOpenItem(item)
    }
}
