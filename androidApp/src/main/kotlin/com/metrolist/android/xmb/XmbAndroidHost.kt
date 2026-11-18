package com.metrolist.android.xmb

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metrolist.android.LocalStartupPresentation
import com.metrolist.android.deviceconnect.AndroidDeviceConnect
import com.metrolist.android.extensions.findActivity
import com.metrolist.android.playback.PlayerConnection
import com.metrolist.android.playback.SongDownloadManager
import com.metrolist.android.ui.player.MusicVideoSurface
import com.metrolist.android.ui.screens.rememberAndroidSettingsHost
import com.metrolist.android.ui.screens.settings.rememberAndroidIntegrationsHost
import com.metrolist.android.viewmodels.HomeViewModel
import com.metrolist.shared.db.MusicDatabase
import com.metrolist.shared.listentogether.ListenTogetherManager
import com.metrolist.shared.model.LyricsEntry
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.ui.components.LocalVisualWorkActive
import com.metrolist.shared.ui.navigation.Navigator
import com.metrolist.shared.ui.navigation.Screen
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.viewmodels.PlayerViewModel
import com.metrolist.shared.xmb.SnackbarsAsXmbToasts
import com.metrolist.shared.xmb.XMB_INTEGRATION_PAGES
import com.metrolist.shared.xmb.XmbAccount
import com.metrolist.shared.xmb.XmbApp
import com.metrolist.shared.xmb.XmbDownloads
import com.metrolist.shared.xmb.XmbInputController
import com.metrolist.shared.xmb.XmbListenTogether
import com.metrolist.shared.xmb.XmbLyrics
import com.metrolist.shared.xmb.XmbServices
import com.metrolist.shared.xmb.XmbSettingsHost
import com.metrolist.shared.xmb.XmbVideo
import com.metrolist.shared.xmb.XmbVolume
import com.metrolist.shared.xmb.XmbWallpaper
import com.metrolist.shared.xmb.adapters.DefaultXmbMusicSource
import com.metrolist.shared.xmb.adapters.XmbSearch
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.settings.XmbSystemPages
import com.metrolist.shared.xmb.sound.AndroidXmbSoundPlayer
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel
import java.io.File

/**
 * Android glue for the XMB (M7). `MetrolistApp` shows it whenever its navigation is at a top-level
 * route, keeping its player connection, Listen Together, incoming links and the rest of its setup;
 * a screen the XMB opens (sign-in, a settings page the XMB doesn't draw) shows as before, and back
 * returns here. The XMB runs full screen with the system bars hidden, takes Android's back (gesture
 * or TV remote) while it has somewhere to go back to, and pauses its animation while the app is in
 * the background.
 */
@Composable
internal fun XmbAndroidHost(
    playback: PlaybackController,
    playerConnection: PlayerConnection,
    listenTogether: ListenTogetherManager?,
    database: MusicDatabase,
    downloads: SongDownloadManager,
    downloadedSongIds: Set<String>,
    navigator: Navigator,
    playerViewModel: PlayerViewModel,
    snackbars: SnackbarHostState,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    // One per activity: kept while a stock screen (sign-in, a settings page) is open over the XMB,
    // and stopped with the activity. Rebuilding these on every return left the old copies running
    // on the main thread, all reloading at once when signing in changed the library.
    // The settings host below uses the app's home feed too; the XMB's lists share it.
    val home: HomeViewModel = koinViewModel()
    val model = viewModel { XmbAndroidModel(context.applicationContext, database, home) }
    SideEffect { model.downloaded.value = downloadedSongIds }
    val input = model.input
    BackHandler(enabled = input.canGoBack) { input.send(XmbAction.BACK) }
    SnackbarsAsXmbToasts(snackbars, input)
    val notice = rememberAndroidXmbNotice(playerConnection) { navigator.navigate(Screen.Login) }
    // Links, app shortcuts and notifications MetrolistApp handed over.
    val link by AndroidXmbLinks.pending.collectAsState()
    LaunchedEffect(link) {
        val target = link ?: return@LaunchedEffect
        AndroidXmbLinks.pending.value = null
        input.open(target)
    }
    val wallpaper = rememberAndroidXmbWallpaper(context)
    val signOut by rememberUpdatedState(onSignOut)
    // Lyrics come from the player's view model, as on the stock player; its state is read as
    // Compose state so the lyrics view updates when they arrive.
    val playerState = playerViewModel.uiState.collectAsState()
    val lyrics = remember(playerViewModel) { AndroidXmbLyrics(playerViewModel, playerState) }
    val video = remember(playerConnection) { AndroidXmbVideo(playerConnection) }
    // The stock settings screen's host, so a setting changed in the XMB applies the same way
    // (audio preferences, playback speed, content region, app language and icon...).
    val settingsHost by rememberUpdatedState(
        // MetroEQ is added by the stock settings screen itself, so it's added here too.
        rememberAndroidSettingsHost(onNavigateBack = {}, onNavigateToDownloads = {}, onSpotifySignIn = {}).let { host ->
            host.copy(availablePages = host.availablePages + SettingsPageId.EQUALIZER)
        },
    )
    val storage = rememberAndroidXmbStorage()
    val backup = rememberAndroidXmbBackup(database)
    val system = remember(storage, backup) { XmbSystemPages(storage = storage, backup = backup) }
    val services =
        remember(model, playback, listenTogether, lyrics, video, system) {
            XmbServices(
                playback = playback,
                music = model.music,
                downloads = AndroidXmbDownloads(downloads, model.downloaded, model.viewModelScope),
                search = model.search,
                settings =
                    XmbSettingsHost(
                        { settingsHost },
                        system = system,
                        integrations = {
                            rememberAndroidIntegrationsHost(
                                onNavigateBack = {},
                                onNavigateToListenTogether = { navigator.navigate(Screen.ListenTogetherSettings) },
                            )
                        },
                    ) { page ->
                        navigator.navigate(
                            when (page) {
                                SettingsPageId.LISTEN_TOGETHER -> Screen.ListenTogetherSettings
                                SettingsPageId.DISCORD -> Screen.Discord
                                in XMB_INTEGRATION_PAGES -> Screen.Integrations
                                else -> Screen.SettingsPage(page)
                            },
                        )
                    },
                account =
                    object : XmbAccount {
                        override fun signIn() = navigator.navigate(Screen.Login)

                        override fun switchChannel() = navigator.navigate(Screen.SwitchChannel)

                        override fun signOut() = signOut()
                    },
                listenTogether = listenTogether?.let { manager -> XmbListenTogether(manager) { text -> context.copyText(text) } },
                volume =
                    object : XmbVolume {
                        override val level: Float get() = playerConnection.volume.value

                        override fun set(level: Float) = playerConnection.setVolume(level)
                    },
                wallpaper = wallpaper,
                lyrics = lyrics,
                video = video,
                deviceConnect = AndroidDeviceConnect.manager,
            )
        }
    ImmersiveSystemBars()
    CompositionLocalProvider(
        LocalXmbSoundPlayer provides model.sounds,
        LocalVisualWorkActive provides rememberStartedState(),
    ) {
        // The splash screen waits for the stock Home screen to be laid out (StartupPresentation);
        // the XMB takes its place, so it says so once it's laid out, or the splash never lifts.
        val startup = LocalStartupPresentation.current
        XmbApp(
            Modifier.fillMaxSize().onGloballyPositioned {
                startup?.homeLaidOut = true
                startup?.homeCacheReady = true
            },
            services = services,
            input = input,
            notice = notice,
        )
    }
}

/** The XMB's long-lived parts for one activity: its lists, search, input and sounds. */
internal class XmbAndroidModel(
    context: Context,
    database: MusicDatabase,
    home: HomeViewModel,
) : ViewModel() {
    val downloaded = MutableStateFlow<Set<String>>(emptySet())
    val search = XmbSearch(viewModelScope)
    val music = DefaultXmbMusicSource(database, viewModelScope, downloaded, search = search, homeViewModel = { home.shared })
    val input = XmbInputController(systemBack = true)
    val sounds = AndroidXmbSoundPlayer(context)

    override fun onCleared() {
        sounds.close()
    }
}

/** The player's music video, filling the screen without the stock player's controls. */
private class AndroidXmbVideo(
    private val connection: PlayerConnection,
) : XmbVideo {
    @Composable
    override fun Surface(modifier: Modifier) {
        val connected by connection.isConnected.collectAsState()
        val song by connection.currentSong.collectAsState()
        MusicVideoSurface(
            player = if (connected) connection.videoPlayer else null,
            artworkUrl = song?.thumbnail,
            modifier = modifier,
            cornerRadius = 0.dp,
            fillContainer = true,
            showControls = false,
        )
    }
}

/** Lyrics through the player's view model: loaded per song, null until this song's lines are in. */
private class AndroidXmbLyrics(
    private val viewModel: PlayerViewModel,
    private val state: State<PlayerViewModel.PlayerUiState>,
) : XmbLyrics {
    override fun request(song: SongItem) {
        viewModel.loadLyrics(
            songId = song.id,
            title = song.title,
            artist = song.artists.joinToString { it.name },
            duration = song.duration?.toLong(),
            album = song.album?.name,
        )
    }

    override fun lines(songId: String): List<LyricsEntry>? {
        val current = state.value
        return if (current.currentSong?.id == songId && !current.isLyricsLoading) current.lyrics else null
    }
}

private class AndroidXmbDownloads(
    private val manager: SongDownloadManager,
    override val downloadedSongIds: StateFlow<Set<String>>,
    private val scope: CoroutineScope,
) : XmbDownloads {
    override fun download(song: SongItem) {
        scope.launch { manager.download(song) }
    }

    override fun remove(song: SongItem) {
        scope.launch { manager.removeDownload(song) }
    }
}

/** True while the app is at least started: the XMB stops animating in the background. */
@Composable
private fun rememberStartedState(): StateFlow<Boolean> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val scope = rememberCoroutineScope()
    return remember(lifecycle) {
        lifecycle.currentStateFlow
            .map { it.isAtLeast(Lifecycle.State.STARTED) }
            .stateIn(scope, SharingStarted.Eagerly, lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
}

/** The XMB fills the screen like the console's: system bars hide, and a swipe shows them for a moment. */
@Composable
private fun ImmersiveSystemBars() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val bars = WindowCompat.getInsetsController(window, view)
        bars.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        bars.hide(WindowInsetsCompat.Type.systemBars())
        onDispose { bars.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

/**
 * Background images from the system photo picker. The chosen image is copied into the app's files
 * (picker links don't last), and loaded scaled down to about the screen's size.
 */
@Composable
private fun rememberAndroidXmbWallpaper(context: Context): XmbWallpaper {
    val scope = rememberCoroutineScope()
    val wallpaper = remember(context) { AndroidXmbWallpaper(context.applicationContext) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            val onPicked = wallpaper.pending ?: return@rememberLauncherForActivityResult
            wallpaper.pending = null
            if (uri != null) scope.launch { wallpaper.copy(uri)?.let(onPicked) }
        }
    SideEffect {
        wallpaper.launch = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
    return wallpaper
}

private class AndroidXmbWallpaper(
    private val context: Context,
) : XmbWallpaper {
    var pending: ((String) -> Unit)? = null
    var launch: () -> Unit = {}

    override fun pick(onPicked: (String) -> Unit) {
        pending = onPicked
        launch()
    }

    /** Copies the picked image to a new file (a new name, so the XMB reloads it) and removes older ones. */
    suspend fun copy(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val directory = File(context.filesDir, "xmb").apply { mkdirs() }
                val target = File(directory, "wallpaper-${System.currentTimeMillis()}")
                context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
                    ?: return@runCatching null
                directory.listFiles()?.filter { it.name.startsWith("wallpaper-") && it != target }?.forEach(File::delete)
                target.absolutePath
            }.getOrNull()
        }

    override suspend fun load(path: String): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val metrics = context.resources.displayMetrics
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(path, bounds)
                var sample = 1
                val longest = maxOf(metrics.widthPixels, metrics.heightPixels)
                while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= longest) sample *= 2
                BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
            }.getOrNull()
        }
}

private fun Context.copyText(text: String) {
    getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Listen Together", text))
}
