package com.metrolist.android.ui

import android.app.SearchManager
import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.android.App
import com.metrolist.android.BuildConfig
import com.metrolist.android.LocalListenTogetherManager
import com.metrolist.android.MainActivity
import com.metrolist.android.extensions.findActivity
import com.metrolist.android.listentogether.AndroidListenTogetherManager
import com.metrolist.android.locale.AppLocale
import com.metrolist.android.playback.AndroidPlaybackController
import com.metrolist.android.playback.LocalPlayerConnection
import com.metrolist.android.playback.PlaybackSnackbarEvents
import com.metrolist.android.playback.SongDownloadManager
import com.metrolist.android.playback.resolveIncomingPlaybackSong
import com.metrolist.android.playback.voiceSearchRequest
import com.metrolist.android.ui.player.MusicVideoSurface
import com.metrolist.android.ui.player.PictureInPictureHelper
import com.metrolist.android.update.AppUpdateManager
import com.metrolist.android.utils.isAndroidTv
import com.metrolist.android.utils.startActivitySafely
import com.metrolist.android.viewmodels.LibraryViewModel
import com.metrolist.android.viewmodels.PlaybackViewModel
import com.metrolist.android.widget.PlaylistWidgetReceiver
import com.metrolist.android.xmb.AndroidXmbLinks
import com.metrolist.android.xmb.AndroidXmbOverlay
import com.metrolist.android.xmb.AndroidXmbOverlays
import com.metrolist.android.xmb.XmbAndroidHost
import com.metrolist.android.xmb.xmbOpenFor
import com.metrolist.innertubex.utils.YouTubeUrlParser
import com.metrolist.shared.auth.AuthManager
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.db.entities.KIND_DOWNLOAD
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.youtube_music
import com.metrolist.shared.listentogether.parseListenTogetherInvitation
import com.metrolist.shared.model.SearchFilter
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.playback.LocalPlayerSession
import com.metrolist.shared.playlistimport.PlaylistImportStatusManager
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.state.GlobalYouTube
import com.metrolist.shared.state.GlobalYouTubeRepository
import com.metrolist.shared.state.UploadNotificationState
import com.metrolist.shared.state.applyContentRegion
import com.metrolist.shared.ui.LocalIsInPipMode
import com.metrolist.shared.ui.LocalIsTv
import com.metrolist.shared.ui.components.LocalDownloadedSongIds
import com.metrolist.shared.ui.playback.LocalPlaybackController
import com.metrolist.shared.ui.player.shouldShowMusicVideoSurface
import com.metrolist.shared.utils.BlockScreenshots
import com.metrolist.shared.utils.extractListenTogetherRoomCode
import com.metrolist.shared.utils.parseYouTubePodcastId
import com.metrolist.shared.utils.parseYouTubeStartPositionMs
import com.metrolist.shared.utils.parseYouTubeUrl
import com.metrolist.shared.viewmodels.PlayerViewModel
import com.metrolist.shared.xmb.XMB_SELF_UPDATES
import com.metrolist.shared.xmb.XmbAppMessages
import com.metrolist.shared.xmb.nav.XmbOpen
import com.metrolist.shared.youtube.playback.PlaybackNetworkStateStore
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * XMB Music on Android: the XMB, and what the app needs behind it (the player connection, the
 * account session, Listen Together, links and shortcuts, picture-in-picture). Metrolist's stock
 * screens are gone; the XMB draws everything, Google's sign-in included (in an XMB frame).
 */
@Composable
fun MetrolistApp(
    incomingIntent: Intent? = null,
    incomingPlaybackSong: Deferred<SongItem>? = null,
    onIncomingIntentConsumed: (Intent) -> Unit = {},
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager() }
    val scope = rememberCoroutineScope()
    PlaybackNetworkStateStore.state.collectAsState()
    LifecycleStartEffect(Unit) {
        App.instance.playbackNetworkMonitor.refresh()
        onStopOrDispose { }
    }
    LaunchedEffect(authManager) {
        authManager.restorePersistedSession()
        authManager.restoreSession()
    }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            runCatching { UploadNotificationState.refreshOnAppOpen(GlobalYouTubeRepository.instance) }
        }
    }
    GlobalYouTube.applyContentRegion(
        contentCountry = Settings.shared.contentCountry,
        contentLanguage = Settings.shared.contentLanguage,
        systemLocale = AppLocale.systemLocale(context),
    )
    val playbackViewModel: PlaybackViewModel = koinViewModel()
    val libraryViewModel: LibraryViewModel = koinViewModel()
    LaunchedEffect(Settings.shared.dataSyncId, Settings.shared.accountEmail, Settings.shared.innerTubeAuthUser) {
        libraryViewModel.refresh(force = false)
    }
    if (Settings.shared.blockScreenshots) BlockScreenshots()

    LaunchedEffect(Unit) {
        if (!App.claimStartupUpdateCheck()) return@LaunchedEffect
        // XMB Music: Metrolist's releases would replace this app, so it never checks them.
        AppUpdateManager.checkAtStartup(context = context, networkEnabled = XMB_SELF_UPDATES && Settings.shared.checkForUpdates)
        Settings.shared.lastRunAppVersion = BuildConfig.VERSION_NAME
    }

    val playerConnection = playbackViewModel.playerConnection
    val playbackController = remember(playerConnection) { AndroidPlaybackController(playerConnection, scope) }
    // The XMB's Friends column and Listen Together settings need the room manager from the start.
    var listenTogetherManager by remember(playerConnection) { mutableStateOf(AndroidListenTogetherManager.currentManager) }
    LaunchedEffect(playerConnection) {
        listenTogetherManager = AndroidListenTogetherManager.initialize(context, playerConnection)
    }
    val playerViewModel: PlayerViewModel = koinInject()
    val downloadManager: SongDownloadManager = koinInject()
    val database: MusicDatabase = koinInject()
    val downloadedSongIds by
        remember(database) {
            database.dao().cachedAudioIdsByKind(KIND_DOWNLOAD).distinctUntilChanged().map { ids ->
                withContext(Dispatchers.Default) { ids.toSet() }
            }
        }.collectAsStateWithLifecycle(initialValue = emptySet())

    // Messages the stock screens showed as snackbars reach the XMB as toasts (XmbAndroidHost).
    val snackbars = remember { SnackbarHostState() }
    XmbAppMessages(snackbars, playerViewModel.lyricsTranslationFailures, listenTogetherManager)
    LaunchedEffect(Unit) {
        PlaybackSnackbarEvents.messages.collect { snackbars.showSnackbar(it) }
    }

    // A playlist import keeps the screen on, as the stock import screen did.
    val activeImport by PlaylistImportStatusManager.activeImport.collectAsState()
    val hostView = LocalView.current
    val keepScreenOn = activeImport?.isRunning == true
    DisposableEffect(hostView, keepScreenOn) {
        hostView.keepScreenOn = keepScreenOn
        onDispose { hostView.keepScreenOn = false }
    }

    val isPlaying by playerConnection.isPlaying.collectAsState()
    val currentSong by playerConnection.currentSong.collectAsState(initial = null)
    val isPlayerConnected by playerConnection.isConnected.collectAsState()
    val videoPlayer = if (isPlayerConnected) playerConnection.videoPlayer else null
    val playbackResource by playerConnection.currentPlaybackResource.collectAsState()
    val isVideoModeActive by playerConnection.isVideoModeActive.collectAsState()
    val isVideoViewerVisible by playerConnection.isVideoViewerVisible.collectAsState()
    val activity = remember(context) { context.findActivity() }
    val videoSize = videoPlayer?.videoSize
    val hasVideo =
        shouldShowMusicVideoSurface(
            isVideoModeActive = isVideoModeActive,
            isVideoViewerVisible = isVideoViewerVisible,
            hasVideo = playbackResource?.hasVideo == true,
        )
    val isInPipMode = LocalIsInPipMode.current
    LaunchedEffect(isInPipMode, isPlaying, videoSize, hasVideo) {
        if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureHelper.updatePictureInPictureParams(
                activity = activity,
                isPlaying = isPlaying,
                videoSize = videoSize,
                hasVideo = hasVideo,
            )
        }
    }

    val youtubeMusicLabel = remember { mutableStateOf("") }
    LaunchedEffect(Unit) { youtubeMusicLabel.value = getString(Res.string.youtube_music) }
    LaunchedEffect(incomingIntent) {
        val intent = incomingIntent ?: return@LaunchedEffect
        handleIncomingIntent(
            intent = intent,
            incomingPlaybackSong = incomingPlaybackSong,
            fallbackArtist = youtubeMusicLabel.value,
            playerConnection = playerConnection,
            showMessage = { scope.launch { snackbars.showSnackbar(it) } },
        )
        onIncomingIntentConsumed(intent)
    }

    val isAndroidTv = remember(context) { context.isAndroidTv() }
    CompositionLocalProvider(
        LocalPlaybackController provides playbackController,
        LocalDownloadedSongIds provides downloadedSongIds,
        LocalPlayerConnection provides playerConnection,
        LocalPlayerSession provides playerConnection,
        LocalListenTogetherManager provides listenTogetherManager,
        LocalIsTv provides isAndroidTv,
    ) {
        if (isInPipMode) {
            // Picture in picture: only the video (or the artwork) shows.
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MusicVideoSurface(
                    player = if (hasVideo) videoPlayer else null,
                    artworkUrl = currentSong?.thumbnail,
                    modifier = Modifier.fillMaxSize(),
                    fillContainer = true,
                    useSurfaceView = true,
                    showControls = false,
                    isPip = true,
                    cornerRadius = 0.dp,
                )
            }
            return@CompositionLocalProvider
        }
        XmbAndroidHost(
            playback = playbackController,
            playerConnection = playerConnection,
            listenTogether = listenTogetherManager,
            database = database,
            downloads = downloadManager,
            downloadedSongIds = downloadedSongIds,
            playerViewModel = playerViewModel,
            snackbars = snackbars,
            onSignOut = {
                scope.launch {
                    authManager.clearAllCredentials()
                    context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
                        ?.let(context::startActivitySafely)
                }
            },
        )
    }
}

/** Opens what [intent] asks for in the XMB, or plays it. */
private suspend fun handleIncomingIntent(
    intent: Intent,
    incomingPlaybackSong: Deferred<SongItem>?,
    fallbackArtist: String,
    playerConnection: com.metrolist.android.playback.PlayerConnection,
    showMessage: (String) -> Unit,
) {
    if (intent.action == MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH) {
        playerConnection.playFromSearch(voiceSearchRequest(intent.getStringExtra(SearchManager.QUERY).orEmpty(), intent.extras))
        return
    }
    when (val destination = intent.toIncomingDestination()) {
        null -> {}

        // Room links do nothing while Listen Together is off, as in the stock app.
        is IncomingDestination.ListenTogether -> {
            if (Settings.shared.listenTogetherEnabled) AndroidXmbLinks.open(XmbOpen.joinRoom(destination.roomCode))
        }

        IncomingDestination.ListenTogetherRoom -> {
            if (Settings.shared.listenTogetherEnabled) xmbOpenFor(destination)?.let(AndroidXmbLinks::open)
        }

        IncomingDestination.Login -> {
            AndroidXmbOverlays.show(AndroidXmbOverlay.SignIn())
        }

        is IncomingDestination.PlaylistImport -> {
            AndroidXmbLinks.open(XmbOpen.settings(com.metrolist.shared.ui.settings.SettingsPageId.BACKUP_RESTORE))
            destination.resultMessage?.let(showMessage)
        }

        is IncomingDestination.PlaySearch -> {
            val songs =
                runCatching {
                    GlobalYouTubeRepository.instance
                        .searchFiltered(destination.query, SearchFilter.SONG)
                        .items
                        .filterIsInstance<SongItem>()
                }.getOrDefault(emptyList())
            if (songs.isNotEmpty()) {
                playerConnection.playQueue(
                    songs,
                    finite = true,
                )
            } else {
                AndroidXmbLinks.open(XmbOpen.search(destination.query))
            }
        }

        is IncomingDestination.PlayVideo -> {
            val song =
                incomingPlaybackSong?.await() ?: resolveIncomingPlaybackSong(
                    videoId = destination.videoId,
                    fallbackArtist = fallbackArtist,
                    loadMetadata = GlobalYouTubeRepository.instance::getSongMetadata,
                    loadPlayerInformation = GlobalYouTubeRepository.instance::getSongPlayerInformation,
                )
            playerConnection.playSong(song, "", startPositionMs = destination.startPositionMs)
        }

        else -> {
            xmbOpenFor(destination)?.let(AndroidXmbLinks::open)
        }
    }
}

sealed class IncomingDestination {
    data class Podcast(
        val id: String,
    ) : IncomingDestination()

    object Home : IncomingDestination()

    object Library : IncomingDestination()

    object Downloads : IncomingDestination()

    data class PlaylistImport(
        val resultMessage: String?,
    ) : IncomingDestination()

    object ListenTogetherRoom : IncomingDestination()

    object Updates : IncomingDestination()

    object Settings : IncomingDestination()

    object Login : IncomingDestination()

    object BackupRestore : IncomingDestination()

    object Recognition : IncomingDestination()

    object Player : IncomingDestination()

    data class Search(
        val query: String?,
    ) : IncomingDestination()

    data class PlaySearch(
        val query: String,
    ) : IncomingDestination()

    data class Album(
        val id: String,
    ) : IncomingDestination()

    data class Artist(
        val id: String,
    ) : IncomingDestination()

    data class Playlist(
        val id: String,
    ) : IncomingDestination()

    data class AutoPlaylist(
        val type: String,
    ) : IncomingDestination()

    data class Browse(
        val id: String,
        val params: String?,
        val title: String?,
    ) : IncomingDestination()

    data class ListenTogether(
        val roomCode: String,
    ) : IncomingDestination()

    data class PlayVideo(
        val videoId: String,
        val playlistId: String?,
        val startPositionMs: Long = 0L,
    ) : IncomingDestination()
}

fun Intent.toIncomingDestination(): IncomingDestination? {
    if (action == MainActivity.ACTION_OPEN_WIDGET_TARGET) {
        return widgetTargetDestination()
    }

    val urls =
        when (action) {
            Intent.ACTION_VIEW, NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                listOfNotNull(dataString)
            }

            Intent.ACTION_SEND -> {
                if (type == "text/plain") {
                    getCharSequenceExtra(Intent.EXTRA_TEXT)
                        ?.toString()
                        ?.trim()
                        ?.split(Regex("\\s+"))
                        .orEmpty()
                } else {
                    emptyList()
                }
            }

            else -> {
                emptyList()
            }
        }
    urls.firstNotNullOfOrNull(::incomingUrlDestination)?.let { return it }

    return when (action) {
        MainActivity.ACTION_SEARCH -> IncomingDestination.Search(null)
        MainActivity.ACTION_LIBRARY -> IncomingDestination.Library
        MainActivity.ACTION_PLAYLIST_IMPORT -> IncomingDestination.PlaylistImport(getStringExtra(MainActivity.EXTRA_IMPORT_RESULT_MESSAGE))
        MainActivity.ACTION_DOWNLOADS -> IncomingDestination.Downloads
        MainActivity.ACTION_LISTEN_TOGETHER -> IncomingDestination.ListenTogetherRoom
        MainActivity.ACTION_UPDATES -> IncomingDestination.Updates
        MainActivity.ACTION_LOGIN -> IncomingDestination.Login
        Intent.ACTION_APPLICATION_PREFERENCES -> IncomingDestination.Settings
        MainActivity.ACTION_BACKUP_RESTORE -> IncomingDestination.BackupRestore
        MainActivity.ACTION_RECOGNITION -> IncomingDestination.Recognition
        else -> null
    }
}

private fun incomingUrlDestination(rawUri: String): IncomingDestination? {
    parseYouTubePodcastId(rawUri)?.let { return IncomingDestination.Podcast(it) }
    parseListenTogetherInvitation(rawUri)?.let { return IncomingDestination.ListenTogether(it) }
    extractListenTogetherRoomCode(rawUri)?.let { return IncomingDestination.ListenTogether(it) }
    when (val parsed = parseYouTubeUrl(rawUri)) {
        is YouTubeUrlParser.ParsedUrl.Video -> {
            return IncomingDestination.PlayVideo(parsed.id, null, parseYouTubeStartPositionMs(rawUri))
        }

        is YouTubeUrlParser.ParsedUrl.Playlist -> {
            return IncomingDestination.Playlist(parsed.id)
        }

        is YouTubeUrlParser.ParsedUrl.Album -> {
            return IncomingDestination.Album(parsed.id)
        }

        is YouTubeUrlParser.ParsedUrl.Artist -> {
            return IncomingDestination.Artist(parsed.id)
        }

        null -> {}
    }
    val uri = rawUri.toUri()
    val host = uri.host?.lowercase()
    if ((uri.scheme == "https" || uri.scheme == "http") &&
        (host == "youtube.com" || host?.endsWith(".youtube.com") == true) &&
        uri.path == "/search"
    ) {
        return uri
            .getQueryParameter("q")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let(IncomingDestination::Search)
    }
    return null
}

fun Intent.widgetTargetDestination(): IncomingDestination? {
    val id = getStringExtra(MainActivity.EXTRA_WIDGET_TARGET_ID) ?: return null
    return when (getStringExtra(MainActivity.EXTRA_WIDGET_TARGET_TYPE)) {
        "ALBUM" -> IncomingDestination.Album(id)

        "ARTIST" -> IncomingDestination.Artist(id)

        "PLAYLIST" -> IncomingDestination.Playlist(id)

        "AUTO_PLAYLIST" -> IncomingDestination.AutoPlaylist(id)

        PlaylistWidgetReceiver.TARGET_TYPE_LOCAL,
        PlaylistWidgetReceiver.TARGET_TYPE_ONLINE,
        PlaylistWidgetReceiver.TARGET_TYPE_COLLECTION,
        -> IncomingDestination.Playlist(id)

        PlaylistWidgetReceiver.TARGET_TYPE_LIKED -> IncomingDestination.AutoPlaylist("liked")

        PlaylistWidgetReceiver.TARGET_TYPE_DOWNLOADED -> IncomingDestination.AutoPlaylist("downloaded")

        PlaylistWidgetReceiver.TARGET_TYPE_TOP -> IncomingDestination.AutoPlaylist("top100")

        "SONG" -> IncomingDestination.PlayVideo(id, null)

        else -> null
    }
}

fun String.sanitizeRoomCode(): String = filter { it.isLetterOrDigit() }.uppercase().take(8)
