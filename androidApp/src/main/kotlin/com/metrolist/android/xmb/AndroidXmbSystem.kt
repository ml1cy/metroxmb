package com.metrolist.android.xmb

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import com.metrolist.android.backup.BackupRestoreManager
import com.metrolist.android.playback.AudioCacheManager
import com.metrolist.android.playback.MusicService
import com.metrolist.android.playback.SongDownloadManager
import com.metrolist.android.ui.player.CanvasCache
import com.metrolist.shared.api.artwork.AnimatedArtworkService
import com.metrolist.shared.api.musicvideo.MusicVideoResolver
import com.metrolist.shared.backup.defaultBackupFilename
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.android_auto_pinned_playlist_mode
import com.metrolist.shared.generated.resources.android_auto_pinned_playlist_mode_desc
import com.metrolist.shared.generated.resources.android_auto_pinned_playlists
import com.metrolist.shared.generated.resources.android_auto_pinned_playlists_count
import com.metrolist.shared.generated.resources.android_auto_play_in_order
import com.metrolist.shared.generated.resources.android_auto_quick_add_playlist
import com.metrolist.shared.generated.resources.backup_filename_prefix
import com.metrolist.shared.generated.resources.cached_playlist
import com.metrolist.shared.generated.resources.downloaded_songs
import com.metrolist.shared.generated.resources.liked_songs
import com.metrolist.shared.generated.resources.my_top_100
import com.metrolist.shared.generated.resources.none
import com.metrolist.shared.generated.resources.podcasts
import com.metrolist.shared.generated.resources.settings_quick_add_summary
import com.metrolist.shared.generated.resources.shuffle
import com.metrolist.shared.generated.resources.uploaded_playlist_name
import com.metrolist.shared.settings.AutomotiveAutoPlaylist
import com.metrolist.shared.settings.AutomotivePinnedPlaylist
import com.metrolist.shared.settings.AutomotivePinnedPlaylistPlaybackMode
import com.metrolist.shared.settings.AutomotivePlaylistSource
import com.metrolist.shared.settings.MAX_AUTOMOTIVE_PINNED_PLAYLISTS
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.settings.getAutomotivePinnedPlaylists
import com.metrolist.shared.settings.setAutomotivePinnedPlaylists
import com.metrolist.shared.storage.ResizableImageDiskCache
import com.metrolist.shared.storage.SongCacheLimitsMb
import com.metrolist.shared.ui.settings.SettingsChoice
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsItemRegistration
import com.metrolist.shared.xmb.settings.XmbBackup
import com.metrolist.shared.xmb.settings.XmbStorage
import com.metrolist.shared.xmb.settings.XmbStorageUsage
import com.metrolist.shared.xmb.settings.XmbStore
import com.metrolist.shared.xmb.settings.XmbSystemHooks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/** Android's caches and downloads for the XMB's Storage page, read and cleared as the stock page does. */
internal class AndroidXmbStorage(
    private val context: Context,
    private val audio: AudioCacheManager,
    private val downloads: SongDownloadManager,
    private val videos: MusicVideoResolver,
    private val animatedArtwork: AnimatedArtworkService,
) : XmbStorage {
    private val imageCache get() = context.imageLoader.diskCache

    override val limitChoices: Map<XmbStore, List<Int>> =
        mapOf(
            XmbStore.SONGS to SongCacheLimitsMb,
            XmbStore.IMAGES to listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192),
            XmbStore.ANIMATED_ARTWORK to listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, 16384),
        )

    override suspend fun usage(): XmbStorageUsage =
        withContext(Dispatchers.IO) {
            XmbStorageUsage(
                bytes =
                    mapOf(
                        XmbStore.DOWNLOADS to audio.getDownloadSizeBytes(),
                        XmbStore.SONGS to audio.getCacheSizeBytes(),
                        XmbStore.IMAGES to (imageCache?.size ?: 0L),
                        XmbStore.ANIMATED_ARTWORK to CanvasCache.get(context).sizeBytes(),
                    ),
                limitsMb =
                    mapOf(
                        XmbStore.SONGS to Settings.shared.maxSongCacheSize,
                        XmbStore.IMAGES to Settings.shared.maxImageCacheSize,
                        XmbStore.ANIMATED_ARTWORK to Settings.shared.animatedArtworkCacheSize,
                    ),
                videoEntries = videos.getCacheEntryCount(),
            )
        }

    override suspend fun clear(store: XmbStore): Boolean =
        withContext(Dispatchers.IO) {
            when (store) {
                XmbStore.DOWNLOADS -> {
                    downloads.clearDownloads()
                }

                XmbStore.SONGS -> {
                    audio.clearCache()
                }

                XmbStore.IMAGES -> {
                    imageCache?.clear()
                    context.imageLoader.memoryCache?.clear()
                    true
                }

                XmbStore.ANIMATED_ARTWORK -> {
                    CanvasCache.get(context).clear()
                    animatedArtwork.clearCache()
                    true
                }

                XmbStore.MUSIC_VIDEOS -> {
                    videos.clearCache()
                    true
                }
            }
        }

    override suspend fun setLimit(
        store: XmbStore,
        megabytes: Int,
    ) {
        when (store) {
            XmbStore.SONGS -> {
                Settings.shared.maxSongCacheSize = megabytes
                audio.enforceCacheLimit()
            }

            XmbStore.IMAGES -> {
                withContext(Dispatchers.IO) {
                    (imageCache as? ResizableImageDiskCache)?.resize(megabytes * 1024L * 1024L)
                    Settings.shared.maxImageCacheSize = megabytes
                }
            }

            XmbStore.ANIMATED_ARTWORK -> {
                withContext(Dispatchers.IO) {
                    CanvasCache.get(context).setLimit(megabytes)
                    Settings.shared.animatedArtworkCacheSize = megabytes
                }
            }

            XmbStore.DOWNLOADS, XmbStore.MUSIC_VIDEOS -> {
                Unit
            }
        }
    }
}

@Composable
internal fun rememberAndroidXmbStorage(): AndroidXmbStorage {
    val context = LocalContext.current.applicationContext
    val audio: AudioCacheManager = koinInject()
    val downloads: SongDownloadManager = koinInject()
    val videos: MusicVideoResolver = koinInject()
    val animatedArtwork: AnimatedArtworkService = koinInject()
    return remember(context, audio, downloads, videos, animatedArtwork) {
        AndroidXmbStorage(context, audio, downloads, videos, animatedArtwork)
    }
}

/**
 * Backups through the system document pickers, with the stock page's manager. A successful restore
 * restarts the app (RestoreRestartActivity), as on the stock page.
 */
@Composable
internal fun rememberAndroidXmbBackup(database: MusicDatabase): XmbBackup {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember(context, database) { BackupRestoreManager(context.applicationContext, database) }
    val prefix = stringResource(Res.string.backup_filename_prefix)
    var pending by remember { mutableStateOf<((Boolean?) -> Unit)?>(null) }
    val create =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            val done = pending ?: return@rememberLauncherForActivityResult
            pending = null
            if (uri == null) return@rememberLauncherForActivityResult done(null)
            scope.launch { done(manager.backup(uri).isSuccess) }
        }
    val restore =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val done = pending ?: return@rememberLauncherForActivityResult
            pending = null
            if (uri == null) return@rememberLauncherForActivityResult done(null)
            scope.launch { done(manager.restore(uri).isSuccess) }
        }
    return remember(create, restore, prefix) {
        object : XmbBackup {
            override fun create(done: (Boolean?) -> Unit) {
                pending = done
                create.launch("${defaultBackupFilename(prefix)}.zip")
            }

            override fun restore(done: (Boolean?) -> Unit) {
                pending = done
                restore.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }
        }
    }
}

/**
 * Android Auto as an XMB page: the playlist Quick Add saves to, how pinned playlists play, and one
 * row per playlist to pin or unpin it in the car (up to the stock limit).
 */
@Composable
internal fun androidAutoXmbRows(
    database: MusicDatabase,
    hooks: XmbSystemHooks,
): List<SettingsItemRegistration> {
    val context = LocalContext.current
    val settings = Settings.shared
    val playlists by database.dao().playlistsByNameAsc().collectAsState(initial = emptyList())
    val editable = playlists.filter { it.playlist.isEditable }
    val none = stringResource(Res.string.none)
    val inOrder = stringResource(Res.string.android_auto_play_in_order)
    val shuffle = stringResource(Res.string.shuffle)
    val pinnedTitle = stringResource(Res.string.android_auto_pinned_playlists)
    val pinnedFull = stringResource(Res.string.android_auto_pinned_playlists_count)
    val pins = settings.getAutomotivePinnedPlaylists()
    val automatic =
        listOf(
            AutomotiveAutoPlaylist.LIKED to stringResource(Res.string.liked_songs),
            AutomotiveAutoPlaylist.DOWNLOADED to stringResource(Res.string.downloaded_songs),
            AutomotiveAutoPlaylist.TOP to stringResource(Res.string.my_top_100),
            AutomotiveAutoPlaylist.CACHED to stringResource(Res.string.cached_playlist),
            AutomotiveAutoPlaylist.UPLOADED to stringResource(Res.string.uploaded_playlist_name),
            AutomotiveAutoPlaylist.PODCASTS to stringResource(Res.string.podcasts),
        ).map { (auto, title) -> AutomotivePinnedPlaylist(AutomotivePlaylistSource.AUTO, auto.id, title) }
    val local = playlists.map { AutomotivePinnedPlaylist(AutomotivePlaylistSource.LOCAL, it.id, it.title) }
    val quickAddChoices = listOf(SettingsChoice("", none)) + editable.map { SettingsChoice(it.id, it.title) }
    val quickAdd = settings.androidAutoTargetPlaylist.takeIf { id -> editable.any { it.id == id } }.orEmpty()
    return buildList {
        add(
            SettingsItemRegistration(
                id = "xmb_auto_quick_add",
                title = stringResource(Res.string.android_auto_quick_add_playlist),
                description = stringResource(Res.string.settings_quick_add_summary),
                control = SettingsControl.CHOICE,
                choices = quickAddChoices,
                selectedChoiceId = quickAdd,
                valueLabel = quickAddChoices.first { it.id == quickAdd }.label,
                onChoiceSelected = { settings.androidAutoTargetPlaylist = it },
            ),
        )
        val mode = settings.androidAutoPinnedPlaylistPlaybackMode
        add(
            SettingsItemRegistration(
                id = "xmb_auto_pinned_mode",
                title = stringResource(Res.string.android_auto_pinned_playlist_mode),
                description = stringResource(Res.string.android_auto_pinned_playlist_mode_desc),
                control = SettingsControl.CHOICE,
                choices =
                    listOf(
                        SettingsChoice(AutomotivePinnedPlaylistPlaybackMode.IN_ORDER.name, inOrder),
                        SettingsChoice(AutomotivePinnedPlaylistPlaybackMode.SHUFFLE.name, shuffle),
                    ),
                selectedChoiceId = mode.name,
                valueLabel = if (mode == AutomotivePinnedPlaylistPlaybackMode.SHUFFLE) shuffle else inOrder,
                onChoiceSelected = { id ->
                    settings.androidAutoPinnedPlaylistPlaybackMode = AutomotivePinnedPlaylistPlaybackMode.valueOf(id)
                    notifyAutomotiveChanged(context)
                },
            ),
        )
        (automatic + local).distinctBy { it.source to it.id }.forEach { playlist ->
            val pinned = pins.any { it.source == playlist.source && it.id == playlist.id }
            add(
                SettingsItemRegistration(
                    id = "xmb_auto_pin_${playlist.source}_${playlist.id}",
                    title = playlist.title,
                    description = pinnedTitle,
                    control = SettingsControl.SWITCH,
                    checked = pinned,
                    onCheckedChange = { pin ->
                        val current = settings.getAutomotivePinnedPlaylists()
                        val others = current.filterNot { it.source == playlist.source && it.id == playlist.id }
                        if (pin && others.size >= MAX_AUTOMOTIVE_PINNED_PLAYLISTS) {
                            hooks.toast(
                                pinnedFull
                                    .replace(
                                        "%1\$d",
                                        others.size.toString(),
                                    ).replace("%2\$d", MAX_AUTOMOTIVE_PINNED_PLAYLISTS.toString()),
                            )
                        } else {
                            settings.setAutomotivePinnedPlaylists(if (pin) others + playlist else others)
                            notifyAutomotiveChanged(context)
                        }
                    },
                ),
            )
        }
    }
}

/** Tells the music service the car's library changed, as the stock page does. */
private fun notifyAutomotiveChanged(context: Context) {
    if (!MusicService.isRunning) return
    context.startService(Intent(context, MusicService::class.java).setAction(MusicService.ACTION_REFRESH_AUTOMOTIVE_LIBRARY))
}
