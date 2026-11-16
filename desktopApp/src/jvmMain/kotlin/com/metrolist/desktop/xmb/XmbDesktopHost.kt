package com.metrolist.desktop.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import coil3.compose.setSingletonImageLoaderFactory
import com.metrolist.desktop.state.app.AppState
import com.metrolist.desktop.storage.DownloadManager
import com.metrolist.desktop.storage.DownloadState
import com.metrolist.desktop.ui.theme.MetrolistTheme
import com.metrolist.desktop.utils.DesktopImageLoader
import com.metrolist.desktop.youtube.DesktopPageBoundTokenHost
import com.metrolist.desktop.youtube.DesktopPageBoundTokenRuntime
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.playback.LocalPlaybackController
import com.metrolist.shared.xmb.InterfaceStyle
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.XmbDownloads
import com.metrolist.shared.xmb.XmbServices
import com.metrolist.shared.xmb.adapters.DefaultXmbMusicSource
import com.metrolist.shared.xmb.sound.DesktopXmbSoundPlayer
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.context.GlobalContext

/**
 * Desktop glue for the XMB: shown by `App()` in Main.kt instead of the stock content when the
 * interface style is XMB and onboarding is done. Supplies the playback controller `App()`
 * provides, the music lists, downloads and the image loader, and keeps the page-bound token host
 * the stock content runs, so stream resolution works the same.
 */
@Composable
internal fun XmbDesktopHost(checkDarkMode: (Boolean) -> Unit) {
    SideEffect { checkDarkMode(true) }
    setSingletonImageLoaderFactory { context -> DesktopImageLoader.create(context) }
    val sounds = remember { DesktopXmbSoundPlayer() }
    DisposableEffect(sounds) { onDispose { sounds.close() } }
    val playback = LocalPlaybackController.current
    val services =
        remember(playback) {
            val downloads = DesktopXmbDownloads()
            XmbServices(
                playback = playback,
                music = DefaultXmbMusicSource(GlobalContext.get().get<MusicDatabase>(), AppState.scope, downloads.downloadedSongIds),
                downloads = downloads,
            )
        }
    MetrolistTheme {
        DesktopPageBoundTokenHost(GlobalContext.get().get<DesktopPageBoundTokenRuntime>())
        CompositionLocalProvider(LocalXmbSoundPlayer provides sounds) {
            XmbApp(
                onSwitchToStock = { Settings.shared.interfaceStyle = InterfaceStyle.METROLIST },
                services = services,
            )
        }
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

/** Whether `App()` shows the XMB. Onboarding always runs in the stock interface first. */
internal fun showsXmb(settings: Settings = Settings.shared): Boolean =
    settings.interfaceStyle == InterfaceStyle.XMB && settings.desktopOnboardingComplete
