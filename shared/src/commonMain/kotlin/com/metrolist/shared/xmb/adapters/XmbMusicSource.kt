package com.metrolist.shared.xmb.adapters

import com.metrolist.shared.api.YouTubeRepository
import com.metrolist.shared.constants.SongSortType
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.StoredHistoryRow
import com.metrolist.shared.db.entities.toSongItem
import com.metrolist.shared.db.visibleHistoryRows
import com.metrolist.shared.model.AlbumItem
import com.metrolist.shared.model.AlbumTiny
import com.metrolist.shared.model.ArtistItem
import com.metrolist.shared.model.ArtistTiny
import com.metrolist.shared.model.BuiltinPlaylist
import com.metrolist.shared.model.PlaylistItem
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.model.YTItem
import com.metrolist.shared.model.isVideoSong
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.ui.menu.loadMediaItemSongs
import com.metrolist.shared.utils.Logger
import com.metrolist.shared.viewmodels.HomeViewModel
import com.metrolist.shared.viewmodels.LibraryViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** A drill-down list's rows and whether they're still loading or failed. */
data class XmbListState(
    val entries: List<XmbEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isError: Boolean = false,
) {
    companion object {
        fun loaded(entries: List<XmbEntry>) = XmbListState(entries, isLoading = false)

        val failed = XmbListState(isLoading = false, isError = true)
    }
}

/** Supplies the rows of each list the XMB can open. */
fun interface XmbMusicSource {
    fun open(list: XmbList): Flow<XmbListState>
}

/**
 * The real source: the shared Home and Library ViewModels (created on first use, so the XMB costs
 * nothing until a list opens), the database for liked songs, downloads, local playlists and
 * history, the YouTube repository for albums, artists and remote playlists, and [search] for
 * search results.
 */
class DefaultXmbMusicSource(
    private val database: MusicDatabase,
    private val scope: CoroutineScope,
    private val downloadedSongIds: Flow<Set<String>> = flowOf(emptySet()),
    private val search: XmbSearch? = null,
    private val repository: () -> YouTubeRepository = { GlobalYouTubeRepository.instance },
    /** The app's own home feed when it has one, so the feed isn't loaded twice. */
    private val homeViewModel: (() -> HomeViewModel)? = null,
    private val loadSongs: suspend (YTItem) -> List<SongItem> = ::loadMediaItemSongs,
) : XmbMusicSource {
    private val home by lazy { homeViewModel?.invoke() ?: HomeViewModel(database, scope, likedSongsName = "Liked Songs") }
    private val library by lazy {
        LibraryViewModel(scope, database, downloadedSongIds).also {
            it.setActive(true)
            it.refresh(force = false)
        }
    }

    override fun open(list: XmbList): Flow<XmbListState> =
        when (list) {
            XmbList.Home -> {
                home.uiState.map { state ->
                    val sections =
                        (state.data.orEmpty() + state.discoverySections)
                            .filter { it.items.isNotEmpty() }
                            .mapIndexed { index, section -> section.toXmbEntry(index) }
                    XmbListState(
                        entries = sections,
                        isLoading = sections.isEmpty() && state.showLoadingPlaceholder,
                        isError = sections.isEmpty() && state.error != null,
                    )
                }
            }

            XmbList.QuickPicks -> {
                home.uiState.map { state ->
                    val songs = state.quickPicks.map(YTItem::toXmbEntry)
                    XmbListState(
                        entries = songs,
                        isLoading = songs.isEmpty() && (state.showLoadingPlaceholder || state.isContentLoading),
                        isError = songs.isEmpty() && state.error != null,
                    )
                }
            }

            XmbList.Liked -> {
                database.dao().likedSongs(SongSortType.CREATE_DATE, descending = true).map { songs ->
                    XmbListState.loaded(songs.map { it.toSongItem().toXmbEntry() })
                }
            }

            XmbList.Downloads -> {
                database.dao().downloadedSongs(SongSortType.CREATE_DATE, descending = true).map { songs ->
                    XmbListState.loaded(songs.map { it.toSongItem().toXmbEntry() })
                }
            }

            XmbList.Playlists -> {
                libraryItems<PlaylistItem>()
            }

            XmbList.Albums -> {
                libraryItems<AlbumItem>()
            }

            XmbList.Artists -> {
                libraryItems<ArtistItem>()
            }

            XmbList.History -> {
                once {
                    database
                        .visibleHistoryRows(limit = HISTORY_ROWS, sinceMs = null, untilMs = null, before = null, catalogArtistIds = false)
                        .distinctBy(StoredHistoryRow::songId)
                        .map { it.toSongItem().toXmbEntry() }
                }
            }

            is XmbList.Section -> {
                flowOf(XmbListState.loaded(list.section.items.map(YTItem::toXmbEntry)))
            }

            is XmbList.Album -> {
                once { loadSongs(list.album).map(YTItem::toXmbEntry) }
            }

            is XmbList.Artist -> {
                once { loadSongs(list.artist).map(YTItem::toXmbEntry) }
            }

            is XmbList.Playlist -> {
                playlist(list.playlist)
            }

            XmbList.LikedVideos -> {
                database.dao().likedSongs(SongSortType.CREATE_DATE, descending = true).map { songs ->
                    XmbListState.loaded(songs.map { it.toSongItem() }.filter { it.isVideoSong }.map(YTItem::toXmbEntry))
                }
            }

            XmbList.FeedVideos -> {
                home.uiState.map { state ->
                    val videos =
                        (state.data.orEmpty() + state.discoverySections)
                            .flatMap { it.items }
                            .filterIsInstance<SongItem>()
                            .filter { it.isVideoSong }
                            .distinctBy { it.id }
                            .map(YTItem::toXmbEntry)
                    XmbListState(
                        entries = videos,
                        isLoading = videos.isEmpty() && state.showLoadingPlaceholder,
                        isError = videos.isEmpty() && state.error != null,
                    )
                }
            }

            // Built in XmbApp from the playback and room state.
            XmbList.CastDevices, XmbList.RoomMembers, XmbList.Queue -> {
                flowOf(XmbListState.loaded(emptyList()))
            }

            XmbList.Podcasts -> {
                once { repository().getLibraryPodcasts().map(YTItem::toXmbEntry) }
            }

            XmbList.NewEpisodes -> {
                once { repository().getNewPodcastEpisodes().map(YTItem::toXmbEntry) }
            }

            XmbList.SavedEpisodes -> {
                once { repository().getSavedEpisodes().map(YTItem::toXmbEntry) }
            }

            is XmbList.Podcast -> {
                once {
                    repository()
                        .getPodcast(list.podcast.id)
                        ?.episodes
                        .orEmpty()
                        .map(YTItem::toXmbEntry)
                }
            }

            is XmbList.SearchResults -> {
                search?.results(list.query) ?: flowOf(XmbListState.failed)
            }

            is XmbList.SearchSection -> {
                flowOf(XmbListState.loaded(list.summary.items.map(YTItem::toXmbEntry)))
            }

            // Settings rows come from the settings registry, which XmbApp builds in composition.
            is XmbList.SettingsPage, is XmbList.XmbSettings -> {
                flowOf(XmbListState.loaded(emptyList()))
            }
        }.catch { error ->
            if (error is CancellationException) throw error
            Logger.w(TAG, "Could not load XMB list ${list.key}: $error")
            emit(XmbListState.failed)
        }

    private fun playlist(playlist: PlaylistItem): Flow<XmbListState> {
        val builtin = BuiltinPlaylist.fromId(playlist.id)
        return when {
            builtin == BuiltinPlaylist.LIKED -> {
                open(XmbList.Liked)
            }

            builtin == BuiltinPlaylist.DOWNLOADED -> {
                open(XmbList.Downloads)
            }

            builtin == BuiltinPlaylist.HISTORY -> {
                open(XmbList.History)
            }

            // Other built-in collections (top 100, cached, uploads, episodes) arrive with later milestones.
            builtin?.collectionType != null -> {
                flowOf(XmbListState.loaded(emptyList()))
            }

            playlist.isLocal -> {
                database.dao().playlistSongs(playlist.id).map { rows ->
                    XmbListState.loaded(rows.map { it.toSongItem().toXmbEntry() })
                }
            }

            else -> {
                once { loadSongs(playlist).map(YTItem::toXmbEntry) }
            }
        }
    }

    private inline fun <reified T : YTItem> libraryItems(): Flow<XmbListState> =
        library.uiState.map { state ->
            val entries = state.items.filterIsInstance<T>().map(YTItem::toXmbEntry)
            XmbListState(
                entries = entries,
                isLoading = entries.isEmpty() && (state.isLoading || state.isRefreshing),
                isError = entries.isEmpty() && state.error,
            )
        }

    private fun once(load: suspend () -> List<XmbEntry>): Flow<XmbListState> =
        flow {
            emit(XmbListState())
            emit(XmbListState.loaded(load()))
        }

    private companion object {
        const val TAG = "XmbMusic"
        const val HISTORY_ROWS = 200
    }
}

internal fun StoredHistoryRow.toSongItem(): SongItem =
    SongItem(
        id = songId,
        title = title,
        thumbnail = thumbnailUrl,
        artists = (artistNames ?: artists.split(", ")).filter(String::isNotBlank).map { ArtistTiny(id = null, name = it) },
        album = album?.let { AlbumTiny(id = albumId, name = it) },
        duration = duration,
    )
