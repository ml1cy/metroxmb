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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.setSingletonImageLoaderFactory
import com.metrolist.desktop.deviceconnect.DesktopDeviceConnect
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
import com.metrolist.desktop.utils.AppLogger
import com.metrolist.desktop.utils.DesktopImageLoader
import com.metrolist.desktop.youtube.DesktopPageBoundTokenHost
import com.metrolist.desktop.youtube.DesktopPageBoundTokenRuntime
import com.metrolist.shared.auth.YouTubeSessionState
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.action_login
import com.metrolist.shared.generated.resources.action_not_now
import com.metrolist.shared.generated.resources.copy
import com.metrolist.shared.generated.resources.ok
import com.metrolist.shared.generated.resources.playback_error
import com.metrolist.shared.generated.resources.youtube_session_expired_message
import com.metrolist.shared.generated.resources.youtube_session_expired_title
import com.metrolist.shared.model.LyricsEntry
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.ui.playback.LocalPlaybackController
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.xmb.SnackbarsAsXmbToasts
import com.metrolist.shared.xmb.XMB_INTEGRATION_PAGES
import com.metrolist.shared.xmb.XmbAccount
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.XmbDownloads
import com.metrolist.shared.xmb.XmbListenTogether
import com.metrolist.shared.xmb.XmbLyrics
import com.metrolist.shared.xmb.XmbServices
import com.metrolist.shared.xmb.XmbSettingsHost
import com.metrolist.shared.xmb.XmbVideo
import com.metrolist.shared.xmb.XmbVolume
import com.metrolist.shared.xmb.XmbWallpaper
import com.metrolist.shared.xmb.adapters.DatabaseXmbPlaylists
import com.metrolist.shared.xmb.adapters.DefaultXmbMusicSource
import com.metrolist.shared.xmb.adapters.XmbSearch
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.XmbNoticeRequest
import com.metrolist.shared.xmb.settings.XmbSystemPages
import com.metrolist.shared.xmb.sound.DesktopXmbSoundPlayer
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import org.jetbrains.skia.Image as SkiaImage

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
    remember { DesktopXmbPower.keepSmoothInBackground() }
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
                    settings =
                        XmbSettingsHost(
                            { settingsHost },
                            system = XmbSystemPages(storage = DesktopXmbStorage, backup = DesktopXmbBackup(AppState.scope)),
                            integrations = { rememberDesktopXmbIntegrationsHost() },
                            openStockPage = ::openStockSettings,
                        ),
                    account = DesktopXmbAccount,
                    video = DesktopXmbVideo,
                    lyrics = DesktopXmbLyrics,
                    listenTogether = XmbListenTogether(ListenTogetherState.listenTogetherManager, AppState::copyToClipboard),
                    volume = DesktopXmbVolume,
                    wallpaper = DesktopXmbWallpaper,
                    deviceConnect = DesktopDeviceConnect.manager,
                    playlists = DatabaseXmbPlaylists(GlobalContext.get().get<MusicDatabase>()),
                )
            }
        DesktopPageBoundTokenHost(GlobalContext.get().get<DesktopPageBoundTokenRuntime>())
        SnackbarsAsXmbToasts(AppState.snackbarHostState, DesktopXmbLinks.input, keepActions = true)
        Box(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalXmbSoundPlayer provides sounds) {
                XmbApp(
                    services = services,
                    suspended =
                        UiState.showSettings || UiState.showSignIn || UiState.showSwitchChannel ||
                            UiState.showLogViewer || UiState.showTokenLogin || UiState.showIntegrationsSettings,
                    notice = desktopXmbNotice(),
                    input = DesktopXmbLinks.input,
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

/**
 * The desktop's popups as XMB notices: a playback error (OK, or Copy when there are details) and
 * an expired YouTube sign-in (Log in or Not now). Main.kt no longer draws its own dialogs for these.
 */
@Composable
private fun desktopXmbNotice(): XmbNoticeRequest? {
    val ok = stringResource(Res.string.ok)
    val copy = stringResource(Res.string.copy)
    val errorTitle = stringResource(Res.string.playback_error)
    val expiredTitle = stringResource(Res.string.youtube_session_expired_title)
    val expiredMessage = stringResource(Res.string.youtube_session_expired_message)
    val logIn = stringResource(Res.string.action_login)
    val notNow = stringResource(Res.string.action_not_now)
    return when {
        PlayerState.showPlayerError -> {
            val message = PlayerState.playerErrorMessage
            val details = PlayerState.playerErrorDetails.takeIf(String::isNotBlank)
            XmbNoticeRequest(
                XmbNotice(
                    id = "player_error:$message",
                    title = errorTitle,
                    message = message,
                    details = details,
                    buttons = listOfNotNull(ok, copy.takeIf { details != null }),
                    cancel = 0,
                ),
            ) { chosen ->
                if (chosen == 1) AppState.copyToClipboard("$message\n\n$details")
                PlayerState.showPlayerError = false
            }
        }

        YouTubeSessionState.showExpiredSessionNotice -> {
            XmbNoticeRequest(
                XmbNotice(
                    id = "session_expired",
                    title = expiredTitle,
                    message = expiredMessage,
                    buttons = listOf(logIn, notNow),
                    cancel = 1,
                ),
            ) { chosen ->
                YouTubeSessionState.dismissExpiredSessionNotice()
                if (chosen == 0) {
                    UiState.showSignIn = true
                    UiState.showSwitchChannel = false
                }
            }
        }

        else -> {
            null
        }
    }
}

/** Opens the stock settings overlay at [page]. */
private fun openStockSettings(page: SettingsPageId) {
    // Integrations pages (sign-ins, the Listen Together screen) live in the integrations dialog.
    if (page in XMB_INTEGRATION_PAGES) {
        UiState.showIntegrationsSettings = true
        return
    }
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

/** The player's volume, as the stock volume slider sets it. */
private object DesktopXmbVolume : XmbVolume {
    override val level: Float get() = PlayerState.volumeState.value

    override fun set(level: Float) = PlayerState.setVolumeLevel(level)
}

/** Background images: the system file dialog, then decoded with Skia. */
private object DesktopXmbWallpaper : XmbWallpaper {
    override fun pick(onPicked: (String) -> Unit) {
        val dialog = FileDialog(null as Frame?, "", FileDialog.LOAD)
        dialog.setFilenameFilter { _, name -> name.substringAfterLast('.').lowercase() in IMAGE_EXTENSIONS }
        // Windows ignores the filter above; this pattern narrows its dialog instead.
        dialog.file = IMAGE_EXTENSIONS.joinToString(";") { "*.$it" }
        dialog.isVisible = true
        val file = dialog.file ?: return
        val directory = dialog.directory ?: return
        onPicked(File(directory, file).absolutePath)
    }

    override suspend fun load(path: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching { SkiaImage.makeFromEncoded(File(path).readBytes()).toComposeImageBitmap().fitWithin(MAX_WALLPAPER_SIDE) }
                .onFailure { AppLogger.warning("XmbWallpaper", "Could not load $path", it) }
                .getOrNull()
        }

    private val IMAGE_EXTENSIONS = listOf("png", "jpg", "jpeg", "webp", "bmp", "gif")

    /** Wallpapers larger than 4K are scaled down: a camera photo would otherwise hold hundreds of MB. */
    private const val MAX_WALLPAPER_SIDE = 3840
}

/** This image scaled down (smoothly) so its longer side is at most [limit] pixels. */
private fun ImageBitmap.fitWithin(limit: Int): ImageBitmap {
    val longest = maxOf(width, height)
    if (longest <= limit) return this
    val scale = limit.toFloat() / longest
    val target = ImageBitmap((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(target), Size(target.width.toFloat(), target.height.toFloat())) {
        drawImage(this@fitWithin, dstSize = IntSize(target.width, target.height), filterQuality = FilterQuality.High)
    }
    return target
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

    override fun remove(song: SongItem) = DownloadManager.deleteDownload(song.id)
}
