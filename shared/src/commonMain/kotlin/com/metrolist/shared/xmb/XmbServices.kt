package com.metrolist.shared.xmb

import com.metrolist.shared.model.SongItem
import com.metrolist.shared.state.SongLikeState
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SharedSettingsHost
import com.metrolist.shared.xmb.adapters.XmbMusicSource
import com.metrolist.shared.xmb.adapters.XmbSearch
import kotlinx.coroutines.flow.StateFlow

/** Platform downloads, where the platform has them. */
interface XmbDownloads {
    val downloadedSongIds: StateFlow<Set<String>>

    fun download(song: SongItem)

    fun remove(songId: String)
}

/**
 * The platform's sign-in flows. The account the Users column shows comes from `Settings.shared`
 * (name, email, picture, cookie), which these flows update.
 */
interface XmbAccount {
    fun signIn()

    fun switchChannel()

    fun signOut()
}

/**
 * The settings registry's host for this platform (the same kind of [SharedSettingsHost] the stock
 * settings screen gets; [host] is read in composition, so a host backed by state stays current),
 * and a way to show a stock settings page the XMB doesn't draw itself.
 */
class XmbSettingsHost(
    val host: () -> SharedSettingsHost,
    val openStockPage: (SettingsPageId) -> Unit,
)

/**
 * What the XMB needs from its host: the platform's playback controller, the source for
 * drill-down lists, and optionally downloads, search, settings and sign-in. Likes go through the
 * shared [SongLikeState], like the stock menus.
 */
class XmbServices(
    val playback: PlaybackController,
    val music: XmbMusicSource,
    val downloads: XmbDownloads? = null,
    val isLiked: (String) -> Boolean = SongLikeState::isLiked,
    val toggleLike: (SongItem) -> Unit = { SongLikeState.toggleLike(it) },
    val search: XmbSearch? = null,
    val settings: XmbSettingsHost? = null,
    val account: XmbAccount? = null,
)
