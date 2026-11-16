package com.metrolist.desktop.xmb

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import coil3.compose.setSingletonImageLoaderFactory
import com.metrolist.desktop.state.app.AppState
import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.state.auth.AuthState
import com.metrolist.desktop.state.playback.ListenTogetherState
import com.metrolist.desktop.state.playback.LyricsState
import com.metrolist.desktop.state.playback.PlayerState
import com.metrolist.desktop.storage.DownloadManager
import com.metrolist.desktop.storage.DownloadState
import com.metrolist.desktop.ui.components.VideoRenderer
import com.metrolist.desktop.ui.screens.LoginDialog
import com.metrolist.desktop.ui.screens.settings.SettingsOverlay
import com.metrolist.desktop.ui.theme.MetrolistTheme
import com.metrolist.desktop.utils.DesktopImageLoader
import com.metrolist.desktop.youtube.DesktopPageBoundTokenHost
import com.metrolist.desktop.youtube.DesktopPageBoundTokenRuntime
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.model.LyricsEntry
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.ui.playback.LocalPlaybackController
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.XmbAccount
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.XmbDownloads
import com.metrolist.shared.xmb.XmbListenTogether
import com.metrolist.shared.xmb.XmbLyrics
import com.metrolist.shared.xmb.XmbServices
import com.metrolist.shared.xmb.XmbSettingsHost
import com.metrolist.shared.xmb.XmbVideo
import com.metrolist.shared.xmb.adapters.DefaultXmbMusicSource
import com.metrolist.shared.xmb.adapters.XmbSearch
import com.metrolist.shared.xmb.sound.DesktopXmbSoundPlayer
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

/**
 * Desktop glue for the XMB, which `App()` in Main.kt shows once onboarding is done. Supplies the playback controller `App()`
 * provides, the music lists, search, downloads, the settings registry's host, sign-in and the
 * image loader, and keeps the page-bound token host the stock content runs, so stream resolution
 * works the same. The stock settings overlay and the sign-in dialog open over the XMB for what
 * the XMB doesn't draw itself; keys go to them while they're open.
 */
@Composable
internal fun XmbDesktopHost(checkDarkMode: (Boolean) -> Unit) {
    SideEffect { checkDarkMode(true) }
    setSingletonImageLoaderFactory { context -> DesktopImageLoader.create(context) }
    val sounds = remember { DesktopXmbSoundPlayer() }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    val playback = LocalPlaybackController.current
    MetrolistTheme {
        val settingsHost by rememberUpdatedState(rememberDesktopXmbSettingsHost())
        val services =
            remember(playback) {
                val downloads = DesktopXmbDownloads()
                val search = XmbSearch(AppState.scope)
                XmbServices(
                    playback = playback,
                    music =
                        DefaultXmbMusicSource(
                            GlobalContext.get().get<MusicDatabase>(),
                            AppState.scope,
                            downloads.downloadedSongIds,
                            search = search,
                        ),
                    downloads = downloads,
                    search = search,
                    settings = XmbSettingsHost({ settingsHost }, ::openStockSettings),
                    account = DesktopXmbAccount,
                    video = DesktopXmbVideo,
                    lyrics = DesktopXmbLyrics,
                    listenTogether = XmbListenTogether(ListenTogetherState.listenTogetherManager, AppState::copyToClipboard),
                )
            }
        DesktopPageBoundTokenHost(GlobalContext.get().get<DesktopPageBoundTokenRuntime>())
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalXmbSoundPlayer provides sounds) {
                XmbApp(
                    services = services,
                    suspended = UiState.showSettings || UiState.showSignIn || UiState.showSwitchChannel,
                )
            }
            if (UiState.showSettings) SettingsOverlay(MaterialTheme.colorScheme)
            if (UiState.showSignIn || UiState.showSwitchChannel) {
                // Same flow as the stock content's dialog in Main.kt.
                LoginDialog(
                    onDismissRequest = {
                        UiState.showSignIn = false
                        UiState.showSwitchChannel = false
                    },
                    onAuthDataExtracted = { cookie, visitorData, dataSyncId, authUser ->
                        AuthState.updateAuth(
                            cookie,
                            visitorData,
                            dataSyncId,
                            authUser,
                            accountInfo = GlobalYouTubeRepository.instance.lastAccountInfo,
                        )
                        UiState.showSignIn = false
                        UiState.showSwitchChannel = false
                        UiState.appRefreshKey++
                    },
                    isSwitchingChannel = UiState.showSwitchChannel,
                )
            }
        }
    }
}

/** Opens the stock settings overlay at [page]. */
private fun openStockSettings(page: SettingsPageId) {
    UiState.selectedSettingsPage = page
    UiState.showSettings = true
}

/** The player's video frames, drawn with the desktop's video renderer; the player is told the size. */
private object DesktopXmbVideo : XmbVideo {
    @Composable
    override fun Surface(modifier: Modifier) {
        VideoRenderer(modifier = modifier, onViewportSizeChanged = { width, height -> PlayerState.setVideoTargetSize(width, height) })
    }
}

/** The desktop's lyrics state: fetched per song; null until this song's lines are in. */
private object DesktopXmbLyrics : XmbLyrics {
    override fun request(song: SongItem) {
        if (LyricsState.currentLyricsSongId != song.id) LyricsState.fetchLyrics(song)
    }

    override fun lines(songId: String): List<LyricsEntry>? =
        if (LyricsState.currentLyricsSongId == songId && !LyricsState.isLyricsLoading) LyricsState.currentLyricsEntries else null
}

private object DesktopXmbAccount : XmbAccount {
    override fun signIn() {
        UiState.showSignIn = true
    }

    override fun switchChannel() {
        UiState.showSwitchChannel = true
    }

    override fun signOut() {
        AppState.scope.launch { AuthState.signOut() }
    }
}

private class DesktopXmbDownloads : XmbDownloads {
    override val downloadedSongIds: StateFlow<Set<String>> =
        DownloadManager.downloadStates
            .map { states -> states.filterValues { it == DownloadState.DOWNLOADED }.keys }
            .stateIn(AppState.scope, SharingStarted.Eagerly, emptySet())

    override fun download(song: SongItem) = DownloadManager.download(song)

    override fun remove(songId: String) = DownloadManager.deleteDownload(songId)
}
