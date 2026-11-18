package com.metrolist.desktop.xmb

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil3.PlatformContext
import com.metrolist.desktop.auth.DesktopDiscordLogin
import com.metrolist.desktop.auth.registerDesktopDiscordCallback
import com.metrolist.desktop.backup.DesktopBackupRestoreManager
import com.metrolist.desktop.crash.DesktopCrashRecovery
import com.metrolist.desktop.state.app.AppState
import com.metrolist.desktop.state.app.UiState
import com.metrolist.desktop.state.playback.DiscordRpcState
import com.metrolist.desktop.state.playback.ListenTogetherState
import com.metrolist.desktop.state.playback.PlayerState
import com.metrolist.desktop.storage.DownloadManager
import com.metrolist.desktop.storage.SongCacheManager
import com.metrolist.desktop.ui.screens.settings.rememberDeviceConnectSettingsHost
import com.metrolist.desktop.utils.DesktopImageLoader
import com.metrolist.desktop.utils.DesktopRenderBackend
import com.metrolist.desktop.utils.desktopRenderBackends
import com.metrolist.shared.api.musicvideo.MusicVideoResolver
import com.metrolist.shared.backup.defaultBackupFilename
import com.metrolist.shared.discord.openDiscordUrl
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.album
import com.metrolist.shared.generated.resources.artist
import com.metrolist.shared.generated.resources.backup_filename_prefix
import com.metrolist.shared.generated.resources.crash_logs
import com.metrolist.shared.generated.resources.crash_restart
import com.metrolist.shared.generated.resources.devtools_render_backend
import com.metrolist.shared.generated.resources.devtools_render_backend_auto
import com.metrolist.shared.generated.resources.devtools_render_backend_description
import com.metrolist.shared.generated.resources.devtools_render_backend_direct3d
import com.metrolist.shared.generated.resources.devtools_render_backend_metal
import com.metrolist.shared.generated.resources.devtools_render_backend_opengl
import com.metrolist.shared.generated.resources.devtools_render_backend_software
import com.metrolist.shared.generated.resources.playlist
import com.metrolist.shared.generated.resources.podcast
import com.metrolist.shared.generated.resources.sign_in_discord
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.storage.DesktopAnimatedArtworkCache
import com.metrolist.shared.storage.ResizableImageDiskCache
import com.metrolist.shared.storage.SongCacheLimitsMb
import com.metrolist.shared.ui.settings.IntegrationPlaybackState
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.ui.settings.SettingsPlatform
import com.metrolist.shared.ui.settings.SharedIntegrationsHost
import com.metrolist.shared.utils.tryOpenUrl
import com.metrolist.shared.xmb.XmbInputController
import com.metrolist.shared.xmb.nav.XmbOpen
import com.metrolist.shared.xmb.settings.XmbBackup
import com.metrolist.shared.xmb.settings.XmbStorage
import com.metrolist.shared.xmb.settings.XmbStorageUsage
import com.metrolist.shared.xmb.settings.XmbStore
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openDirectoryPicker
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.GlobalContext
import java.io.File

/** The desktop's caches and downloads for the XMB's Storage page, as the stock page reads them. */
internal object DesktopXmbStorage : XmbStorage {
    private val imageCache: ResizableImageDiskCache?
        get() = DesktopImageLoader.create(PlatformContext.INSTANCE).diskCache as? ResizableImageDiskCache

    private val videos: MusicVideoResolver get() = GlobalContext.get().get()

    override val limitChoices: Map<XmbStore, List<Int>> =
        mapOf(
            XmbStore.SONGS to SongCacheLimitsMb,
            XmbStore.IMAGES to listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
            XmbStore.ANIMATED_ARTWORK to listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, 16384),
        )

    override suspend fun usage(): XmbStorageUsage =
        withContext(Dispatchers.IO) {
            val downloads =
                DownloadManager.downloadedFiles.value.sumOf {
                    File(it.audioFilePath).length() + (it.thumbnailFilePath?.let { path -> File(path).length() } ?: 0L)
                }
            XmbStorageUsage(
                bytes =
                    mapOf(
                        XmbStore.DOWNLOADS to downloads,
                        XmbStore.SONGS to SongCacheManager.getCacheSizeBytes(),
                        XmbStore.IMAGES to (imageCache?.size ?: 0L),
                        XmbStore.ANIMATED_ARTWORK to DesktopAnimatedArtworkCache.get().sizeBytes(),
                    ),
                limitsMb =
                    mapOf(
                        XmbStore.SONGS to SongCacheManager.getMaxCacheSizeMB().toInt(),
                        XmbStore.IMAGES to Settings.shared.maxImageCacheSize,
                        XmbStore.ANIMATED_ARTWORK to Settings.shared.animatedArtworkCacheSize,
                    ),
                videoEntries = videos.getCacheEntryCount(),
                location = DownloadManager.getDownloadDir().absolutePath,
                customLocation = Settings.shared.customDownloadPath.isNotBlank(),
            )
        }

    override fun changeLocation(done: (Boolean?) -> Unit) {
        AppState.scope.launch {
            val directory =
                runCatching { FileKit.openDirectoryPicker(directory = PlatformFile(DownloadManager.getDownloadDir())) }
                    .getOrNull()
                    ?.file
                    ?: return@launch done(null)
            done(runCatching { withContext(Dispatchers.IO) { DownloadManager.changeDownloadDirectory(directory) } }.isSuccess)
        }
    }

    override suspend fun resetLocation(): Boolean =
        runCatching { withContext(Dispatchers.IO) { DownloadManager.changeDownloadDirectory(null) } }.isSuccess

    override suspend fun clear(store: XmbStore): Boolean =
        withContext(Dispatchers.IO) {
            when (store) {
                XmbStore.DOWNLOADS -> DownloadManager.clearAllDownloads()
                XmbStore.SONGS -> SongCacheManager.clearCache()
                XmbStore.IMAGES -> DesktopImageLoader.clearCache()
                XmbStore.ANIMATED_ARTWORK -> DesktopAnimatedArtworkCache.get().clear()
                XmbStore.MUSIC_VIDEOS -> videos.clearCache()
            }
            true
        }

    override suspend fun setLimit(
        store: XmbStore,
        megabytes: Int,
    ) = withContext(Dispatchers.IO) {
        when (store) {
            XmbStore.SONGS -> {
                SongCacheManager.setMaxCacheSizeMB(megabytes.toLong())
            }

            XmbStore.IMAGES -> {
                imageCache?.resize(megabytes * 1024L * 1024L)
                Settings.shared.maxImageCacheSize = megabytes
            }

            XmbStore.ANIMATED_ARTWORK -> {
                DesktopAnimatedArtworkCache.get().resize(megabytes * 1024L * 1024L)
                Settings.shared.animatedArtworkCacheSize = megabytes
            }

            XmbStore.DOWNLOADS, XmbStore.MUSIC_VIDEOS -> {
                Unit
            }
        }
    }
}

/** Backups through the system file dialogs; restoring stages the backup and restarts, as the stock page does. */
internal class DesktopXmbBackup(
    private val scope: CoroutineScope,
) : XmbBackup {
    override fun create(done: (Boolean?) -> Unit) {
        scope.launch {
            val result =
                runCatching {
                    val name = defaultBackupFilename(getString(Res.string.backup_filename_prefix))
                    val target =
                        FileKit
                            .openFileSaver(suggestedName = name, defaultExtension = "zip", allowedExtensions = setOf("zip"))
                            ?.file
                            ?: return@runCatching null
                    withContext(Dispatchers.IO) { DesktopBackupRestoreManager.backup(target) }.isSuccess
                }
            done(result.getOrElse { false })
        }
    }

    override fun restore(done: (Boolean?) -> Unit) {
        scope.launch {
            val result =
                runCatching {
                    val source =
                        FileKit.openFilePicker(type = FileKitType.File(listOf("zip", "backup")))?.file
                            ?: return@runCatching null
                    // On success the app restarts with the restored data and this never returns.
                    withContext(Dispatchers.IO) { DesktopBackupRestoreManager.stageRestoreAndRestart(source) }.isSuccess
                }
            done(result.getOrElse { false })
        }
    }
}

/**
 * Links opened from outside (the system URL handler, a second launch): the XMB's input, and the
 * places it shows. The XMB takes a request when it's shown, so one sent at startup isn't lost.
 */
internal object DesktopXmbLinks {
    val input = XmbInputController()

    fun open(target: XmbOpen) = input.open(target)

    fun openAlbum(id: String) = AppState.scope.launch { open(XmbOpen.album(id, getString(Res.string.album))) }

    fun openArtist(id: String) = AppState.scope.launch { open(XmbOpen.artist(id, getString(Res.string.artist))) }

    fun openPlaylist(id: String) = AppState.scope.launch { open(XmbOpen.playlist(id, getString(Res.string.playlist))) }

    fun openPodcast(id: String) = AppState.scope.launch { open(XmbOpen.podcast(id, getString(Res.string.podcast))) }
}

/**
 * The integrations dialog's host, for the XMB's settings column. Starting a Discord sign-in opens
 * the integrations dialog, which shows its code; the rest is as in `IntegrationsOverlay`.
 */
@Composable
internal fun rememberDesktopXmbIntegrationsHost(): SharedIntegrationsHost {
    val discordLogin = DesktopDiscordLogin.flow
    val discordTitle = stringResource(Res.string.sign_in_discord)
    val playbackSpeed by PlayerState.player.playbackSpeed.collectAsState()
    val playbackDuration by PlayerState.playbackDuration.collectAsState()
    return SharedIntegrationsHost(
        platform = SettingsPlatform.DESKTOP,
        onClose = {},
        playbackPosition = PlayerState.playbackPosition,
        currentPositionMs = PlayerState::currentMediaPositionMs,
        playback =
            PlayerState.currentTrack?.let { song ->
                IntegrationPlaybackState(
                    song = song,
                    artworkUrl = PlayerState.currentArtworkUrl,
                    artistArtworkUrl = song.artists.firstOrNull()?.thumbnail,
                    durationMs = playbackDuration,
                    isPlaying = PlayerState.isPlaying,
                    playbackSpeed = playbackSpeed,
                )
            },
        onStartDiscordLogin = { session ->
            discordLogin.start(session)
            // The browser sign-in returns through the app's link handler; the XMB shows a notice meanwhile.
            AppState.scope.launch {
                val registered = withContext(Dispatchers.IO) { registerDesktopDiscordCallback(discordTitle) }
                if (registered) tryOpenUrl(session.authorizeUrl) {} else DesktopXmbDiscord.unavailable = true
            }
        },
        discordLoginInProgress = discordLogin.session != null || discordLogin.exchanging,
        onDiscordSettingsChanged = {
            DiscordRpcState.discordRpcEnabled = Settings.shared.enableDiscordIntegration
            DiscordRpcState.updateDiscordRpc()
        },
        deviceConnect = rememberDeviceConnectSettingsHost(),
        listenTogetherManager = ListenTogetherState.listenTogetherManager,
        onOpenUrl = { openDiscordUrl(it, null) },
        onCopyText = { _, text -> AppState.copyToClipboard(text) },
    )
}

/**
 * Developer Tools rows only the desktop has: the render backend (applied after a restart), a
 * restart, and sharing the last crash report.
 */
@Composable
internal fun desktopXmbDevtoolsRows(): List<SettingsItemRegistration> {
    val backends = remember { desktopRenderBackends() }
    val labels =
        backends.associateWith { backend ->
            stringResource(
                when (backend) {
                    DesktopRenderBackend.AUTO -> Res.string.devtools_render_backend_auto
                    DesktopRenderBackend.DIRECT3D -> Res.string.devtools_render_backend_direct3d
                    DesktopRenderBackend.OPENGL -> Res.string.devtools_render_backend_opengl
                    DesktopRenderBackend.SOFTWARE -> Res.string.devtools_render_backend_software
                    DesktopRenderBackend.METAL -> Res.string.devtools_render_backend_metal
                },
            )
        }
    val selected = backends.firstOrNull { it.name == Settings.shared.desktopRenderBackend } ?: DesktopRenderBackend.AUTO
    val lastCrash = remember { runCatching { DesktopCrashRecovery.store.list().firstOrNull() }.getOrNull() }
    return listOfNotNull(
        SettingsItemRegistration(
            id = "xmb_render_backend",
            title = stringResource(Res.string.devtools_render_backend),
            description = stringResource(Res.string.devtools_render_backend_description),
            control = SettingsControl.CHOICE,
            choices = backends.map { SettingsChoice(it.name, labels.getValue(it)) },
            selectedChoiceId = selected.name,
            valueLabel = labels[selected],
            onChoiceSelected = { Settings.shared.desktopRenderBackend = it },
        ),
        SettingsItemRegistration(
            id = "xmb_restart",
            title = stringResource(Res.string.crash_restart),
            control = SettingsControl.ACTION,
            onClick = { UiState.restartRequested = true },
        ),
        lastCrash?.let { report ->
            SettingsItemRegistration(
                id = "xmb_crash_report",
                title = stringResource(Res.string.crash_logs),
                description = report.stackTrace.lineSequence().firstOrNull(),
                control = SettingsControl.ACTION,
                onClick = { DesktopCrashRecovery.share(report.text) },
            )
        },
    )
}

/** Whether the system wouldn't take the Discord sign-in's return link (the browser can't finish it). */
internal object DesktopXmbDiscord {
    var unavailable by mutableStateOf(false)
}
