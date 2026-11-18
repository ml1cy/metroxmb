package com.metrolist.shared.xmb

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.metrolist.shared.listentogether.ListenTogetherManager
import com.metrolist.shared.model.LyricsEntry
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

    fun remove(song: SongItem)
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

/** The platform's music video surface: draws the playing video's frames, scaled to fit. */
interface XmbVideo {
    @Composable
    fun Surface(modifier: Modifier)
}

/**
 * The platform's lyrics: [request] fetches a song's lyrics, [lines] returns them once loaded (null
 * while loading or for another song). Reads are snapshot state, so a view recomposes when they arrive.
 */
interface XmbLyrics {
    fun request(song: SongItem)

    fun lines(songId: String): List<LyricsEntry>?
}

/** The player's volume, 0 to 1, for the volume and mute shortcuts. */
interface XmbVolume {
    val level: Float

    fun set(level: Float)
}

/**
 * Background images (Theme Settings): [pick] asks the user for an image file and passes its
 * path on; [load] reads one (null when it can't).
 */
interface XmbWallpaper {
    fun pick(onPicked: (String) -> Unit)

    suspend fun load(path: String): ImageBitmap?
}

/** Listen Together: the platform's room manager, and a way to copy the invite link. */
class XmbListenTogether(
    val manager: ListenTogetherManager,
    val copyText: (String) -> Unit,
)

/**
 * What the XMB needs from its host: the platform's playback controller, the source for
 * drill-down lists, and optionally downloads, search, settings, sign-in, video, lyrics and
 * Listen Together. Likes go through the
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
    val video: XmbVideo? = null,
    val lyrics: XmbLyrics? = null,
    val listenTogether: XmbListenTogether? = null,
    val volume: XmbVolume? = null,
    val wallpaper: XmbWallpaper? = null,
)
