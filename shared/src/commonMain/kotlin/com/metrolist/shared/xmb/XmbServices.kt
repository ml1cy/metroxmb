package com.metrolist.shared.xmb

import com.metrolist.shared.model.SongItem
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.xmb.adapters.XmbMusicSource
import kotlinx.coroutines.flow.StateFlow

/** Platform downloads, where the platform has them. */
interface XmbDownloads {
    val downloadedSongIds: StateFlow<Set<String>>

    fun download(song: SongItem)

    fun remove(songId: String)
}

/**
 * What the XMB needs from its host to play music: the platform's playback controller, the
 * source for drill-down lists, and optional downloads. Likes go through the shared
 * [SongLikeState], like the stock menus.
 */
class XmbServices(
    val playback: PlaybackController,
    val music: XmbMusicSource,
    val downloads: XmbDownloads? = null,
    val isLiked: (String) -> Boolean = SongLikeState::isLiked,
    val toggleLike: (SongItem) -> Unit = { SongLikeState.toggleLike(it) },
)
