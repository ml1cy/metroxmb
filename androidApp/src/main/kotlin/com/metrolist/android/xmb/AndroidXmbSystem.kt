package com.metrolist.android.xmb

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.imageLoader
import com.metrolist.android.backup.BackupRestoreManager
import com.metrolist.android.playback.AudioCacheManager
import com.metrolist.android.playback.SongDownloadManager
import com.metrolist.android.ui.player.CanvasCache
import com.metrolist.shared.api.artwork.AnimatedArtworkService
import com.metrolist.shared.api.musicvideo.MusicVideoResolver
import com.metrolist.shared.backup.defaultBackupFilename
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.backup_filename_prefix
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.storage.ResizableImageDiskCache
import com.metrolist.shared.storage.SongCacheLimitsMb
import com.metrolist.shared.xmb.settings.XmbBackup
import com.metrolist.shared.xmb.settings.XmbStorage
import com.metrolist.shared.xmb.settings.XmbStorageUsage
import com.metrolist.shared.xmb.settings.XmbStore
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
