package com.metrolist.shared.xmb

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.metrolist.shared.auth.AuthManager
import com.metrolist.shared.cast.CastState
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.add_to_playlist
import com.metrolist.shared.generated.resources.album
import com.metrolist.shared.generated.resources.artist
import com.metrolist.shared.generated.resources.create_playlist
import com.metrolist.shared.generated.resources.device_connect_title
import com.metrolist.shared.generated.resources.downloads
import com.metrolist.shared.generated.resources.history
import com.metrolist.shared.generated.resources.lastfm_invalid_credentials
import com.metrolist.shared.generated.resources.lastfm_login_session_invalid
import com.metrolist.shared.generated.resources.lastfm_service_error
import com.metrolist.shared.generated.resources.liked_songs
import com.metrolist.shared.generated.resources.listenbrainz_connect_failed
import com.metrolist.shared.generated.resources.listenbrainz_user_token
import com.metrolist.shared.generated.resources.no
import com.metrolist.shared.generated.resources.password
import com.metrolist.shared.generated.resources.playlist
import com.metrolist.shared.generated.resources.podcast
import com.metrolist.shared.generated.resources.scrobble_connection_failed
import com.metrolist.shared.generated.resources.sleep_timer
import com.metrolist.shared.generated.resources.username
import com.metrolist.shared.generated.resources.xmb_cat_search
import com.metrolist.shared.generated.resources.xmb_item_album
import com.metrolist.shared.generated.resources.xmb_item_artist
import com.metrolist.shared.generated.resources.xmb_item_playlist
import com.metrolist.shared.generated.resources.xmb_item_subsetting
import com.metrolist.shared.generated.resources.xmb_playlist_added
import com.metrolist.shared.generated.resources.xmb_playlist_failed
import com.metrolist.shared.generated.resources.xmb_playlist_new
import com.metrolist.shared.generated.resources.xmb_queue_playing
import com.metrolist.shared.generated.resources.xmb_signed_in
import com.metrolist.shared.generated.resources.xmb_sleep_end_of_song
import com.metrolist.shared.generated.resources.xmb_sleep_minutes
import com.metrolist.shared.generated.resources.xmb_sleep_off
import com.metrolist.shared.generated.resources.xmb_sleep_set
import com.metrolist.shared.generated.resources.xmb_ui_clock
import com.metrolist.shared.generated.resources.xmb_users_sign_out_confirm
import com.metrolist.shared.generated.resources.xmb_value_off
import com.metrolist.shared.generated.resources.xmb_value_on
import com.metrolist.shared.generated.resources.yes
import com.metrolist.shared.model.RepeatMode
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.LocalVisualWorkActive
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.SettingsPageId
import com.metrolist.shared.ui.settings.SettingsUiRegistry
import com.metrolist.shared.ui.settings.XmbScrobbleMessages
import com.metrolist.shared.ui.settings.rememberIntegrationsRegistry
import com.metrolist.shared.ui.settings.rememberMetrolistSettingsRegistry
import com.metrolist.shared.ui.settings.xmbScrobbleSignIn
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.background.XmbWaveNudge
import com.metrolist.shared.visualizer.background.setRibbonPasses
import com.metrolist.shared.visualizer.gaia.GaiaVisualizer
import com.metrolist.shared.visualizer.rememberVisualizerAudio
import com.metrolist.shared.visualizer.valley.ValleyVisualizer
import com.metrolist.shared.visualizer.waves.XmbBackdrop
import com.metrolist.shared.xmb.adapters.XmbEntry
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.XmbSettingLabels
import com.metrolist.shared.xmb.adapters.XmbSettingStep
import com.metrolist.shared.xmb.adapters.castEntries
import com.metrolist.shared.xmb.adapters.currentLyricIndex
import com.metrolist.shared.xmb.adapters.matches
import com.metrolist.shared.xmb.adapters.memberEntries
import com.metrolist.shared.xmb.adapters.peerEntries
import com.metrolist.shared.xmb.adapters.peerId
import com.metrolist.shared.xmb.adapters.plainNumber
import com.metrolist.shared.xmb.adapters.song
import com.metrolist.shared.xmb.adapters.toXmbEntries
import com.metrolist.shared.xmb.adapters.toXmbEntry
import com.metrolist.shared.xmb.adapters.xmbSettingStep
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.XmbBar
import com.metrolist.shared.xmb.bar.XmbBarGeometry
import com.metrolist.shared.xmb.bar.XmbLevelView
import com.metrolist.shared.xmb.bar.XmbTaps
import com.metrolist.shared.xmb.bar.rememberXmbRowStyles
import com.metrolist.shared.xmb.nav.XmbAccountInfo
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbFullScreen
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbKeyboard
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.XmbNoticeRequest
import com.metrolist.shared.xmb.nav.XmbOption
import com.metrolist.shared.xmb.nav.XmbOptionsPanel
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.nav.XmbTouchGesture
import com.metrolist.shared.xmb.nav.XmbTouchTracker
import com.metrolist.shared.xmb.nav.XmbVisualizerKind
import com.metrolist.shared.xmb.nav.focusCategory
import com.metrolist.shared.xmb.nav.focusRow
import com.metrolist.shared.xmb.nav.opening
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.resolve
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.nav.xmbActionForScroll
import com.metrolist.shared.xmb.nav.xmbCategories
import com.metrolist.shared.xmb.nav.xmbFriendItems
import com.metrolist.shared.xmb.nav.xmbSearchItems
import com.metrolist.shared.xmb.nav.xmbSettingsItems
import com.metrolist.shared.xmb.nav.xmbUserItems
import com.metrolist.shared.xmb.screens.XmbKeyboardView
import com.metrolist.shared.xmb.screens.XmbLyricsView
import com.metrolist.shared.xmb.screens.XmbNoticeView
import com.metrolist.shared.xmb.screens.XmbOptionsPanelView
import com.metrolist.shared.xmb.screens.XmbPickerView
import com.metrolist.shared.xmb.screens.XmbToast
import com.metrolist.shared.xmb.screens.XmbTrackInfo
import com.metrolist.shared.xmb.settings.XMB_HIDDEN_SETTINGS_PAGES
import com.metrolist.shared.xmb.settings.XmbKeyChord
import com.metrolist.shared.xmb.settings.XmbPrefs
import com.metrolist.shared.xmb.settings.XmbSettingsHooks
import com.metrolist.shared.xmb.settings.XmbShortcut
import com.metrolist.shared.xmb.settings.XmbShortcutCapture
import com.metrolist.shared.xmb.settings.XmbSystemHooks
import com.metrolist.shared.xmb.settings.XmbTrackOverlay
import com.metrolist.shared.xmb.settings.rememberXmbShortcutText
import com.metrolist.shared.xmb.settings.title
import com.metrolist.shared.xmb.settings.xmbClockText
import com.metrolist.shared.xmb.settings.xmbCustomRows
import com.metrolist.shared.xmb.settings.xmbOwnSettingsItems
import com.metrolist.shared.xmb.settings.xmbSettingsRows
import com.metrolist.shared.xmb.settings.xmbShortcutCapture
import com.metrolist.shared.xmb.settings.xmbSystemRows
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import com.metrolist.shared.xmb.sound.SilentXmbSoundPlayer
import com.metrolist.shared.xmb.sound.XmbSound
import com.metrolist.shared.xmb.sound.sound
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import com.metrolist.shared.xmb.theme.XmbPalette
import com.metrolist.shared.xmb.theme.rememberXmbFontFamily
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Clock

/** Local wall-clock time, which picks the month's color, the brightness and the clock text. */
data class XmbTime(
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
) {
    val hourOfDay: Float get() = hour + minute / 60f

    /** Month/day and 24-hour time, e.g. "11/16 21:05". */
    val clockText: String get() = "$month/$day $hour:${minute.toString().padStart(2, '0')}"

    companion object {
        fun now(): XmbTime {
            val local = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            return XmbTime(local.month.number, local.day, local.hour, local.minute)
        }
    }
}

/** The on-screen keyboard and what its text is for. */
private class KeyboardSession(
    val keyboard: XmbKeyboard,
    val forSearch: Boolean,
    val onSubmit: (String) -> Unit,
)

/**
 * The XMB front end: background, category row with item columns, drill-down levels, the △
 * options panel, value pickers, the on-screen keyboard, now-playing info and the clock.
 * Without [services] the lists stay closed (dev snapshots). While [suspended] (a stock dialog is
 * over the XMB) keys are left alone. [time], [animate] and the initial state exist so dev snapshots render a fixed scene.
 */
@Composable
fun XmbApp(
    modifier: Modifier = Modifier,
    services: XmbServices? = null,
    suspended: Boolean = false,
    notice: XmbNoticeRequest? = null,
    input: XmbInputController? = null,
    time: (() -> XmbTime)? = null,
    animate: Boolean = true,
    initialCategory: XmbCategoryId = XmbCategoryId.MUSIC,
    initialItem: Int = 0,
    initialLevels: List<XmbLevel> = emptyList(),
    initialOptions: Boolean = false,
    initialPicker: XmbPicker? = null,
    initialKeyboard: XmbKeyboard? = null,
    initialFullScreen: XmbFullScreen? = null,
) {
    // The XMB's own settings (Settings column: Theme, Date and Time, Sound, Keyboard Shortcuts, Music Player).
    val prefs = XmbPrefs.current()
    val platformSounds = LocalXmbSoundPlayer.current
    val sounds = if (prefs.menuSounds) platformSounds else SilentXmbSoundPlayer
    LaunchedEffect(platformSounds, prefs.menuSoundVolume) { platformSounds.setVolume(prefs.menuSoundVolume) }
    val now = rememberXmbTime(time)
    val fontFamily = rememberXmbFontFamily()
    val motion = remember { XmbWaveMotion() }
    var fullScreen by remember { mutableStateOf(initialFullScreen) }
    val visualizerMode = fullScreen as? XmbFullScreen.Visualizer
    // The visualizer on screen, kept while it fades out; Random avoids it, Auto-cycle starts from it.
    var lastVisualizer by remember { mutableStateOf(XmbVisualizerKind.WAVES) }
    visualizerMode?.let { lastVisualizer = it.kind }
    LaunchedEffect(visualizerMode?.cycle, visualizerMode?.kind) {
        val mode = visualizerMode ?: return@LaunchedEffect
        if (!mode.cycle) return@LaunchedEffect
        delay(CYCLE_MS)
        val kinds = XmbVisualizerKind.entries
        (fullScreen as? XmbFullScreen.Visualizer)?.takeIf { it.cycle }?.let { current ->
            fullScreen = current.copy(kind = kinds[(kinds.indexOf(current.kind) + 1) % kinds.size])
        }
    }
    val audio = rememberVisualizerAudio(active = visualizerMode != null && animate)
    val waveSpeed by rememberUpdatedState(prefs.waveSpeed)
    val frameRate by rememberUpdatedState(prefs.frameRate)
    val seconds = rememberAnimationSeconds(animate, motion, audio, { frameRate }) { waveSpeed }
    SideEffect { setRibbonPasses(prefs.visualizerQuality.ribbonPasses) }
    // The full-screen visualizer's track overlay shows until this time, then fades.
    var overlayUntil by remember { mutableFloatStateOf(VISUALIZER_OVERLAY_SECONDS) }
    val focusRequester = remember { FocusRequester() }
    // Keyboard Shortcuts: the shortcut waiting for its new key, and whether the last key was refused.
    var recording by remember { mutableStateOf<XmbShortcut?>(null) }
    var recordingRefused by remember { mutableStateOf(false) }
    // A short message at the top ("Shuffle on", "Volume 70%") until [toastUntil].
    var toast by remember { mutableStateOf<String?>(null) }
    var toastUntil by remember { mutableFloatStateOf(0f) }
    // Visualizer when idle: the last key or click, and whether the open visualizer came from idling.
    var lastInput by remember { mutableFloatStateOf(0f) }
    var idleVisualizer by remember { mutableStateOf(false) }
    var unmuteLevel by remember { mutableFloatStateOf(DEFAULT_UNMUTE_VOLUME) }
    // A host notice (playback error, expired sign-in) takes the keys until a button is chosen.
    val noticeId = notice?.notice?.id
    var noticeFocus by remember(noticeId) { mutableIntStateOf(notice?.notice?.focus ?: 0) }
    LaunchedEffect(noticeId) { if (noticeId != null) sounds.play(XmbSound.NOTICE) }
    val scope = rememberCoroutineScope()
    val actions = remember(services, scope) { services?.let { XmbActions(it, scope) } }

    // Live columns: settings pages from the registry, recent searches, the signed-in account.
    var settingsRevision by remember { mutableIntStateOf(0) }
    val settingsRegistry = services?.settings?.let { rememberMetrolistSettingsRegistry(it.host(), settingsRevision) }
    // A Last.fm (true) or ListenBrainz (false) sign-in to ask for on the keyboard.
    var scrobbleSignIn by remember { mutableStateOf<Boolean?>(null) }
    val integrationsRegistry =
        services?.settings?.let { host ->
            host.integrations?.let { integrations ->
                rememberIntegrationsRegistry(integrations(), settingsRevision) { lastFm -> scrobbleSignIn = lastFm }
            }
        }
    // Integrations join the settings column as one more group of pages.
    val registry =
        remember(settingsRegistry, integrationsRegistry) {
            if (settingsRegistry == null || integrationsRegistry == null) {
                settingsRegistry
            } else {
                SettingsUiRegistry(
                    settingsRegistry.pages + integrationsRegistry.pages,
                    settingsRegistry.rootGroups + integrationsRegistry.rootGroups,
                )
            }
        }
    val systemPages = services?.settings?.system
    val drawnPages = systemPages?.drawn.orEmpty()
    val labels =
        XmbSettingLabels(
            on = stringResource(Res.string.xmb_value_on),
            off = stringResource(Res.string.xmb_value_off),
            yes = stringResource(Res.string.yes),
            no = stringResource(Res.string.no),
        )
    val history by remember(services) { services?.search?.history ?: flowOf(emptyList()) }.collectAsState(emptyList())
    val suggestions by remember(services) { services?.search?.suggestions ?: flowOf(emptyList()) }.collectAsState(emptyList())
    val account = services?.account?.let { xmbAccountInfo() }
    val signOutQuestion = stringResource(Res.string.xmb_users_sign_out_confirm)
    val together = services?.listenTogether
    val friends = together?.let { rememberXmbFriendsInfo(it) }
    val playbackState =
        services
            ?.playback
            ?.state
            ?.collectAsState()
            ?.value
    val text = rememberXmbM6Text()
    val deviceConnectLabel = stringResource(Res.string.device_connect_title)
    val scrobbleUsername = stringResource(Res.string.username)
    val scrobblePassword = stringResource(Res.string.password)
    val scrobbleToken = stringResource(Res.string.listenbrainz_user_token)
    val scrobbleSignedIn = stringResource(Res.string.xmb_signed_in)
    val scrobbleMessages =
        XmbScrobbleMessages(
            invalidLastFm = stringResource(Res.string.lastfm_invalid_credentials),
            invalidLastFmSession = stringResource(Res.string.lastfm_login_session_invalid),
            connectionFailed = stringResource(Res.string.scrobble_connection_failed),
            serviceError = stringResource(Res.string.lastfm_service_error),
            listenBrainzFailed = stringResource(Res.string.listenbrainz_connect_failed),
        )
    val queuePlaying = stringResource(Res.string.xmb_queue_playing)
    val sleepTitle = stringResource(Res.string.sleep_timer)
    val sleepOff = stringResource(Res.string.xmb_sleep_off)
    val sleepMinutes = stringResource(Res.string.xmb_sleep_minutes)
    val sleepEndOfSong = stringResource(Res.string.xmb_sleep_end_of_song)
    val sleepSet = stringResource(Res.string.xmb_sleep_set)
    val playlistTitle = stringResource(Res.string.add_to_playlist)
    val playlistNew = stringResource(Res.string.xmb_playlist_new)
    val playlistCreate = stringResource(Res.string.create_playlist)
    val playlistAdded = stringResource(Res.string.xmb_playlist_added)
    val playlistFailed = stringResource(Res.string.xmb_playlist_failed)
    val shortcutText = rememberXmbShortcutText()
    val categories =
        xmbCategories(
            users = xmbUserItems(account),
            settings =
                xmbOwnSettingsItems() + registry?.let { xmbSettingsItems(it, drawnPages, hidden = XMB_HIDDEN_SETTINGS_PAGES) }.orEmpty(),
            search = xmbSearchItems(history),
            friends = xmbFriendItems(friends),
        )
    val itemCounts = categories.map { it.items.size }
    val itemTitles = categories.map { category -> category.items.map { it.title.resolve() } }

    var navigation by remember {
        val start = XmbNavigation.initial(itemCounts, initialCategory.ordinal)
        val item = initialItem.coerceIn(0, (itemCounts[start.category] - 1).coerceAtLeast(0))
        mutableStateOf(start.copy(items = start.items.toMutableList().also { it[start.category] = item }, stack = initialLevels))
    }
    LaunchedEffect(itemCounts) { navigation = navigation.clampItems(itemCounts) }
    var options by remember { mutableStateOf<XmbOptionsPanel?>(null) }
    var picker by remember { mutableStateOf(initialPicker) }
    var keyboard by remember {
        mutableStateOf(initialKeyboard?.let { KeyboardSession(it, forSearch = true) {} })
    }

    val level = navigation.level
    val settingsPage = (level?.list as? XmbList.SettingsPage)?.page
    val xmbPage = (level?.list as? XmbList.XmbSettings)?.page
    val musicState by remember(level?.list?.key, services) {
        val list = level?.list
        if (list == null || services == null || settingsPage != null || xmbPage != null || list == XmbList.CastDevices ||
            list == XmbList.RoomMembers || list == XmbList.Queue
        ) {
            flowOf(XmbListState.loaded(emptyList()))
        } else {
            services.music.open(list)
        }
    }.collectAsState(XmbListState())
    val wallpapers = services?.wallpaper
    val settingsHooks =
        XmbSettingsHooks(
            pickWallpaper =
                wallpapers?.let { picker ->
                    {
                        picker.pick { path ->
                            Settings.shared.xmbWallpaper = path
                            Settings.shared.xmbBackground = XmbPrefs.IMAGE_BACKGROUND
                        }
                    }
                },
            recordShortcut = {
                recording = it
                recordingRefused = false
            },
        )
    val listState =
        if (xmbPage != null) {
            XmbListState.loaded(xmbSettingsRows(xmbPage, prefs, settingsHooks).map { it.toXmbEntry(labels) })
        } else if (settingsPage != null && systemPages != null && settingsPage in drawnPages) {
            val hooks =
                XmbSystemHooks(
                    toast = { message ->
                        toast = message
                        toastUntil = seconds.floatValue + TOAST_SECONDS
                    },
                    openStock = { services.settings.openStockPage(it) },
                )
            XmbListState.loaded(xmbSystemRows(settingsPage, systemPages, hooks, settingsRevision).orEmpty().map { it.toXmbEntry(labels) })
        } else if (settingsPage != null) {
            val hooks =
                XmbSystemHooks(
                    toast = { message ->
                        toast = message
                        toastUntil = seconds.floatValue + TOAST_SECONDS
                    },
                    openStock = { services?.settings?.openStockPage?.invoke(it) },
                )
            // Rows the registry draws with its own widgets become XMB rows (or are left out), and
            // rows leading to pages that only set up the stock screens are left out.
            val page =
                registry?.get(settingsPage)?.let { registered ->
                    registered.copy(
                        groups =
                            registered.groups
                                .map { group ->
                                    group.copy(
                                        items =
                                            group.items.flatMap { item ->
                                                when {
                                                    item.destination in XMB_HIDDEN_SETTINGS_PAGES -> {
                                                        emptyList()
                                                    }

                                                    item.control == SettingsControl.CUSTOM -> {
                                                        xmbCustomRows(
                                                            item,
                                                            hooks,
                                                            services?.deviceConnect,
                                                        )
                                                    }

                                                    else -> {
                                                        listOf(item)
                                                    }
                                                }
                                            },
                                    )
                                }.filter { it.items.isNotEmpty() },
                    )
                }
            // Slider formatters are composable, so their labels are made here.
            val sliderLabels = mutableMapOf<String, String>()
            page?.groups?.forEach { group ->
                group.items.forEach { item ->
                    if (item.control == SettingsControl.SLIDER) {
                        val value = item.sliderValue
                        if (value != null) sliderLabels[item.id] = item.sliderValueFormatter?.invoke(value) ?: plainNumber(value)
                    }
                }
            }
            XmbListState.loaded(page?.toXmbEntries(labels, sliderLabels).orEmpty())
        } else if (level?.list == XmbList.Queue) {
            val queue = playbackState?.queue.orEmpty()
            val current = playbackState?.currentQueueIndex ?: -1
            XmbListState.loaded(
                queue.mapIndexed { index, song ->
                    song.toXmbEntry().copy(key = "queue:$index:${song.id}", value = queuePlaying.takeIf { index == current })
                },
            )
        } else if (level?.list == XmbList.CastDevices) {
            val casts =
                castEntries(playbackState?.castDevices.orEmpty(), playbackState?.castState ?: CastState(), text.connected, text.connecting)
            // The user's other devices (Device Connect) follow the cast devices.
            val peers =
                services
                    ?.deviceConnect
                    ?.let { manager ->
                        val state by manager.state.collectAsState()
                        val sync by manager.remote.state.collectAsState()
                        peerEntries(state.peers, state.localDeviceId, sync.targetId, deviceConnectLabel, text.connected)
                    }.orEmpty()
            val devices = casts + peers
            XmbListState(devices, isLoading = devices.isEmpty())
        } else if (level?.list == XmbList.RoomMembers) {
            val room =
                together
                    ?.manager
                    ?.roomState
                    ?.collectAsState()
                    ?.value
            XmbListState.loaded(memberEntries(room?.users.orEmpty(), text.host))
        } else {
            musicState
        }
    // Cast devices are only searched for while their list is open.
    val castListOpen = level?.list == XmbList.CastDevices
    DisposableEffect(castListOpen, services) {
        val playback = services?.playback
        if (castListOpen) playback?.startCastDiscovery()
        onDispose { if (castListOpen) playback?.stopCastDiscovery() }
    }
    // Visualizer when idle: while music plays and nothing is open, Waves after the chosen time.
    val idleAfter = prefs.idleVisualizerSeconds
    val playing = playbackState?.isPlaying == true
    val idleReady =
        idleAfter > 0 && animate && playing && !suspended && fullScreen == null && keyboard == null &&
            picker == null && options == null && notice == null && recording == null
    LaunchedEffect(idleReady, idleAfter) {
        if (!idleReady) return@LaunchedEffect
        lastInput = seconds.floatValue
        while (seconds.floatValue - lastInput < idleAfter) delay(IDLE_CHECK_MS)
        fullScreen = XmbFullScreen.Visualizer(XmbVisualizerKind.WAVES)
        idleVisualizer = true
        overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
    }
    LaunchedEffect(fullScreen) { if (fullScreen == null) idleVisualizer = false }
    val drill by animateFloatAsState(if (navigation.stack.isEmpty()) 0f else 1f, tween(XmbMotion.FADE_MS))
    val pickerShown by animateFloatAsState(if (picker != null) 1f else 0f, tween(XmbMotion.FADE_MS))
    val keyboardShown by animateFloatAsState(if (keyboard != null) 1f else 0f, tween(XmbMotion.FADE_MS))
    val fullShown by animateFloatAsState(if (fullScreen != null) 1f else 0f, tween(VISUALIZER_FADE_MS))
    val visualizerShown by animateFloatAsState(if (visualizerMode != null) 1f else 0f, tween(VISUALIZER_FADE_MS))
    val videoShown by animateFloatAsState(if (fullScreen == XmbFullScreen.Video) 1f else 0f, tween(VISUALIZER_FADE_MS))
    val lyricsOn = fullScreen == XmbFullScreen.Lyrics || visualizerMode?.lyrics == true
    val lyricsShown by animateFloatAsState(if (lyricsOn) 1f else 0f, tween(VISUALIZER_FADE_MS))
    val playingSong = playbackState?.currentSong
    LaunchedEffect(lyricsOn, playingSong?.id) {
        if (lyricsOn && playingSong != null) services?.lyrics?.request(playingSong)
    }

    LaunchedEffect(level?.list?.key, listState.entries.size, listState.isLoading) {
        if (!listState.isLoading) navigation = navigation.clampLevel(listState.entries.size)
        if (initialOptions && options == null && actions != null) {
            val entry = listState.entries.getOrNull(navigation.level?.focus ?: -1)
            if (entry != null) options = XmbOptionsPanel(entry, actions.options(entry))
        }
    }
    LaunchedEffect(suggestions) {
        if (services?.search == null) return@LaunchedEffect
        keyboard?.takeIf { it.forSearch }?.let { session ->
            keyboard =
                KeyboardSession(
                    session.keyboard.withSuggestions(if (session.keyboard.text.isBlank()) emptyList() else suggestions),
                    true,
                    session.onSubmit,
                )
        }
    }

    fun feedback(
        action: XmbAction,
        result: XmbFeedback,
    ) {
        result.sound()?.let(sounds::play)
        val nudge =
            when (result) {
                XmbFeedback.MOVE_SIDE -> if (action == XmbAction.LEFT) XmbWaveNudge.LEFT else XmbWaveNudge.RIGHT
                XmbFeedback.MOVE_UP -> XmbWaveNudge.UP
                XmbFeedback.MOVE_DOWN -> XmbWaveNudge.DOWN
                XmbFeedback.SELECT -> XmbWaveNudge.SELECT
                XmbFeedback.BACK -> XmbWaveNudge.BACK
                XmbFeedback.ERROR -> XmbWaveNudge.ERROR
                XmbFeedback.NONE -> null
            }
        if (prefs.waveReacts) nudge?.let(motion::nudge)
    }

    fun openKeyboard(session: KeyboardSession) {
        keyboard = session
        if (session.forSearch) services?.search?.type(session.keyboard.text)
    }

    fun search(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        navigation = navigation.copy(category = XmbCategoryId.SEARCH.ordinal, stack = emptyList())
        navigation = navigation.push(XmbLevel(XmbList.SearchResults(trimmed), trimmed, XmbIcon.Drawable(Res.drawable.xmb_cat_search)))
    }

    fun showToast(message: String) {
        toast = message
        toastUntil = seconds.floatValue + TOAST_SECONDS
    }

    // Last.fm asks for the username, then the password; ListenBrainz for the user token.
    LaunchedEffect(scrobbleSignIn) {
        val lastFm = scrobbleSignIn ?: return@LaunchedEffect
        scrobbleSignIn = null

        fun finish(
            user: String,
            credential: String,
        ) {
            scope.launch {
                val error = xmbScrobbleSignIn(lastFm, user, credential, scrobbleMessages)
                showToast(error ?: scrobbleSignedIn)
                settingsRevision++
            }
        }
        if (lastFm) {
            openKeyboard(
                KeyboardSession(XmbKeyboard(title = scrobbleUsername, text = ""), forSearch = false) { user ->
                    if (user.isNotBlank()) {
                        openKeyboard(
                            KeyboardSession(
                                XmbKeyboard(title = scrobblePassword, text = "", secret = true),
                                forSearch = false,
                            ) { password ->
                                if (password.isNotEmpty()) finish(user, password)
                            },
                        )
                    }
                },
            )
        } else {
            openKeyboard(
                KeyboardSession(XmbKeyboard(title = scrobbleToken, text = "", secret = true), forSearch = false) { token ->
                    if (token.isNotBlank()) finish("", token)
                },
            )
        }
    }

    /** Add to Playlist: the user's playlists in a picker, after New Playlist (named on the keyboard). */
    fun addToPlaylist(song: SongItem) {
        val playlists = services?.playlists ?: return

        fun report(
            added: Boolean,
            name: String,
        ) = showToast(if (added) playlistAdded.replace("%1\$s", name) else playlistFailed)
        scope.launch {
            val existing =
                try {
                    playlists.editable()
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    emptyList()
                }
            picker =
                XmbPicker.Choice(playlistTitle, listOf(playlistNew) + existing.map { it.second }, focus = 0) { index ->
                    if (index == 0) {
                        openKeyboard(
                            KeyboardSession(XmbKeyboard(title = playlistCreate, text = ""), forSearch = false) { typed ->
                                val name = typed.trim()
                                if (name.isNotEmpty()) {
                                    scope.launch {
                                        val id = playlists.create(name)
                                        report(id != null && playlists.add(id, song), name)
                                    }
                                }
                            },
                        )
                    } else {
                        val (id, name) = existing[index - 1]
                        scope.launch { report(playlists.add(id, song), name) }
                    }
                }
        }
    }

    fun commitSetting(change: () -> Unit) {
        change()
        settingsRevision++
    }

    /** Full-screen video of the playing song; false when there's nothing to show. */
    fun watchVideo(): Boolean {
        val playback = services?.playback ?: return false
        if (services.video == null || playback.state.value.currentSong == null) return false
        playback.setVideoMode(true)
        fullScreen = XmbFullScreen.Video
        overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
        return true
    }

    /** Runs [then] with the Listen Together name, asking for one on the keyboard first if it's blank. */
    fun withName(then: (String) -> Unit) {
        val manager = together?.manager ?: return
        val name = manager.username.value
        if (name.isNotBlank()) {
            then(name)
        } else {
            openKeyboard(
                KeyboardSession(XmbKeyboard(title = text.yourName), forSearch = false) { typed ->
                    if (typed.isNotBlank()) {
                        manager.setUsername(typed)
                        then(typed.trim())
                    }
                },
            )
        }
    }

    fun selectTopLevel(
        category: Int,
        index: Int,
    ): XmbFeedback {
        val item = categories[category].items[index]
        val list = item.list
        val title = itemTitles[category][index]
        when (val action = item.action) {
            XmbItemAction.None -> {
                if (list == null || actions == null) return XmbFeedback.ERROR
                navigation = navigation.push(XmbLevel(list, title, item.icon, item.artwork))
            }

            XmbItemAction.OpenSearch -> {
                if (services?.search == null) return XmbFeedback.ERROR
                openKeyboard(KeyboardSession(XmbKeyboard(title = title), forSearch = true, onSubmit = ::search))
            }

            XmbItemAction.SignIn -> {
                services?.account?.signIn() ?: return XmbFeedback.ERROR
            }

            XmbItemAction.SwitchChannel -> {
                services?.account?.switchChannel() ?: return XmbFeedback.ERROR
            }

            XmbItemAction.SignOut -> {
                val signOut = services?.account ?: return XmbFeedback.ERROR
                picker = XmbPicker.Choice(signOutQuestion, listOf(labels.yes, labels.no), focus = 1) { if (it == 0) signOut.signOut() }
            }

            is XmbItemAction.OpenVisualizer -> {
                fullScreen = XmbFullScreen.Visualizer(action.kind)
                overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            }

            XmbItemAction.OpenRandomVisualizer -> {
                fullScreen = XmbFullScreen.Visualizer(XmbVisualizerKind.entries.filter { it != lastVisualizer }.random())
                overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            }

            XmbItemAction.CycleVisualizers -> {
                fullScreen = XmbFullScreen.Visualizer(lastVisualizer, cycle = true)
                overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            }

            XmbItemAction.WatchVideo -> {
                if (!watchVideo()) return XmbFeedback.ERROR
            }

            XmbItemAction.SleepTimer -> {
                val playback = services?.playback ?: return XmbFeedback.ERROR
                val minutes = SLEEP_MINUTES
                val choices = listOf(sleepOff) + minutes.map { sleepMinutes.replace("%1\$d", it.toString()) } + sleepEndOfSong
                val timer = playback.state.value.sleepTimerState
                val focus =
                    when {
                        !timer.isActive -> 0
                        timer.stopAfterCurrentSong || timer.finishCurrentSong -> choices.lastIndex
                        else -> minutes.indexOfFirst { it * 60_000L == timer.totalDurationMs } + 1
                    }.coerceIn(0, choices.lastIndex)
                picker =
                    XmbPicker.Choice(sleepTitle, choices, focus) { index ->
                        when (index) {
                            0 -> playback.cancelSleepTimer()
                            choices.lastIndex -> playback.stopSleepTimerAfterCurrentSong(fadeOut = true)
                            else -> playback.startSleepTimer(minutes[index - 1] * 60_000L, fadeOut = true)
                        }
                        toast = sleepSet.replace("%1\$s", choices[index])
                        toastUntil = seconds.floatValue + TOAST_SECONDS
                    }
            }

            XmbItemAction.OpenLyrics -> {
                if (services?.lyrics == null) return XmbFeedback.ERROR
                fullScreen = XmbFullScreen.Lyrics
                overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            }

            XmbItemAction.EnableListenTogether -> {
                together?.manager?.setEnabled(true) ?: return XmbFeedback.ERROR
            }

            XmbItemAction.CreateRoom -> {
                val manager = together?.manager ?: return XmbFeedback.ERROR
                withName { name -> manager.createRoom(name) }
            }

            XmbItemAction.JoinRoom -> {
                val manager = together?.manager ?: return XmbFeedback.ERROR
                withName { name ->
                    openKeyboard(
                        KeyboardSession(XmbKeyboard(title = text.roomCode), forSearch = false) { code -> manager.joinRoom(code, name) },
                    )
                }
            }

            XmbItemAction.EditUsername -> {
                val manager = together?.manager ?: return XmbFeedback.ERROR
                openKeyboard(
                    KeyboardSession(
                        XmbKeyboard(title = title, text = manager.username.value),
                        forSearch = false,
                    ) { manager.setUsername(it) },
                )
            }

            XmbItemAction.CopyRoomLink -> {
                val link = together?.manager?.roomLink() ?: return XmbFeedback.ERROR
                together.copyText(link)
            }

            XmbItemAction.LeaveRoom -> {
                val manager = together?.manager ?: return XmbFeedback.ERROR
                picker = XmbPicker.Choice(text.leaveQuestion, listOf(labels.yes, labels.no), focus = 1) { if (it == 0) manager.leaveRoom() }
            }

            XmbItemAction.CancelJoin -> {
                together?.manager?.cancelJoinRequest() ?: return XmbFeedback.ERROR
            }

            is XmbItemAction.ReviewJoin -> {
                val manager = together?.manager ?: return XmbFeedback.ERROR
                picker =
                    XmbPicker.Choice(action.username, listOf(text.approve, text.reject), focus = 0) {
                        if (it == 0) manager.approveJoin(action.userId) else manager.rejectJoin(action.userId)
                    }
            }

            is XmbItemAction.StockSettings -> {
                services?.settings?.openStockPage?.invoke(action.page) ?: return XmbFeedback.ERROR
            }
        }
        return XmbFeedback.SELECT
    }

    /** Connects to a cast device, or disconnects from the one in use; moves playback to or back from another device. */
    fun selectCastDevice(entry: XmbEntry): XmbFeedback {
        entry.peerId()?.let { peer ->
            val manager = services?.deviceConnect ?: return XmbFeedback.ERROR
            // The device playing now hands playback back here; another takes it.
            if (manager.remote.state.value.targetId == peer) {
                manager.remote.transferTo(manager.state.value.localDeviceId)
                return XmbFeedback.BACK
            }
            manager.remote.transferTo(peer)
            return XmbFeedback.SELECT
        }
        val playback = services?.playback ?: return XmbFeedback.ERROR
        val state = playback.state.value
        val current = state.castState.device
        if (current != null && current.matches(entry)) {
            playback.disconnectCast()
            return XmbFeedback.BACK
        }
        val device = state.castDevices.firstOrNull { it.matches(entry) } ?: return XmbFeedback.ERROR
        playback.castTo(device)
        return XmbFeedback.SELECT
    }

    fun selectSetting(index: Int): XmbFeedback {
        val entry = listState.entries.getOrNull(index) ?: return XmbFeedback.ERROR
        if (xmbPage == null && (settingsPage == null || registry == null)) return XmbFeedback.ERROR
        when (val step = xmbSettingStep(entry, settingsPage, registry, labels, drawnPages)) {
            XmbSettingStep.Unavailable -> {
                return XmbFeedback.ERROR
            }

            is XmbSettingStep.Pick -> {
                picker = step.picker
            }

            is XmbSettingStep.Type -> {
                openKeyboard(
                    KeyboardSession(XmbKeyboard(title = step.title, text = step.text, secret = step.secret), forSearch = false) { text ->
                        commitSetting { step.apply(text) }
                    },
                )
            }

            is XmbSettingStep.Open -> {
                val target = registry?.get(step.page) ?: return XmbFeedback.ERROR
                navigation =
                    navigation.push(
                        XmbLevel(XmbList.SettingsPage(step.page), target.title, XmbIcon.Drawable(Res.drawable.xmb_item_subsetting)),
                    )
            }

            is XmbSettingStep.Stock -> {
                val settingsHost = services?.settings ?: return XmbFeedback.ERROR
                val host = settingsHost.host()
                // External pages (the system's link settings) open outside the app.
                if (step.page in host.externalPages) host.onOpenPage(step.page) else settingsHost.openStockPage(step.page)
            }

            is XmbSettingStep.Run -> {
                commitSetting(step.action)
            }
        }
        return XmbFeedback.SELECT
    }

    fun dispatch(action: XmbAction) {
        val playback = services?.playback
        when (action) {
            XmbAction.PLAY_PAUSE -> return run { playback?.togglePlayPause() }
            XmbAction.NEXT_TRACK -> return run { playback?.skipToNext() }
            XmbAction.PREVIOUS_TRACK -> return run { playback?.skipToPrevious() }
            else -> Unit
        }
        keyboard?.let { session ->
            val step = session.keyboard.reduce(action)
            keyboard = step.keyboard?.let { KeyboardSession(it, session.forSearch, session.onSubmit) }
            step.submitted?.let(session.onSubmit)
            step.feedback.sound()?.let(sounds::play)
            return
        }
        picker?.let { current ->
            val step = current.reduce(action)
            picker = step.picker
            step.commit?.let(::commitSetting)
            feedback(action, step.feedback)
            return
        }
        val entries = listState.entries
        options?.let { panel ->
            val step = panel.reduce(action)
            options = step.panel
            step.chosen?.let { chosen ->
                if (chosen == XmbOption.ADD_TO_PLAYLIST) {
                    entries.getOrNull(navigation.level?.focus ?: -1)?.song?.let(::addToPlaylist)
                } else {
                    actions?.run(chosen, entries, navigation.level?.focus ?: -1)?.let { navigation = navigation.push(it) }
                }
            }
            feedback(action, step.feedback)
            return
        }
        val step = navigation.reduce(action, itemCounts, levelSize = entries.size)
        navigation = step.navigation
        var result = step.feedback
        if (step.optionsRequested) {
            val entry = entries.getOrNull(navigation.level?.focus ?: -1)
            val choices = entry?.let { actions?.options(it) }.orEmpty()
            if (entry != null && choices.isNotEmpty()) {
                options = XmbOptionsPanel(entry, choices)
                result = XmbFeedback.SELECT
            } else {
                result = XmbFeedback.ERROR
            }
        }
        step.selected?.let { selection ->
            result =
                when {
                    selection.depth == 0 -> {
                        selectTopLevel(selection.category, selection.item)
                    }

                    entries.getOrNull(selection.item)?.kind == XmbEntryKind.SETTING -> {
                        selectSetting(selection.item)
                    }

                    entries.getOrNull(selection.item)?.kind == XmbEntryKind.DEVICE -> {
                        selectCastDevice(entries[selection.item])
                    }

                    entries.getOrNull(selection.item)?.kind == XmbEntryKind.MEMBER -> {
                        XmbFeedback.ERROR
                    }

                    navigation.level?.list == XmbList.Queue -> {
                        // A queued song plays from its place in the queue, which stays as it is.
                        services?.playback?.playQueueIndex(selection.item)
                        if (services?.playback == null) XmbFeedback.ERROR else XmbFeedback.SELECT
                    }

                    navigation.level?.list == XmbList.LikedVideos || navigation.level?.list == XmbList.FeedVideos -> {
                        // Videos play full screen, like the console's Video column.
                        if (actions?.playFrom(entries, selection.item) == true && watchVideo()) XmbFeedback.SELECT else XmbFeedback.ERROR
                    }

                    else -> {
                        val opened = entries.getOrNull(selection.item)?.toLevel()
                        if (opened != null) {
                            navigation = navigation.push(opened)
                            XmbFeedback.SELECT
                        } else if (actions?.playFrom(entries, selection.item) == true) {
                            XmbFeedback.SELECT
                        } else {
                            XmbFeedback.ERROR
                        }
                    }
                }
        }
        feedback(action, result)
    }

    /** The keyboard takes raw keys: typing, Backspace and Escape; arrows and Enter drive the grid. */
    fun keyboardKey(event: KeyEvent): Boolean {
        val session = keyboard ?: return false

        fun update(next: XmbKeyboard) {
            keyboard = KeyboardSession(next, session.forSearch, session.onSubmit)
            if (session.forSearch) services?.search?.type(next.text)
        }
        when (event.key) {
            Key.Backspace -> {
                update(session.keyboard.deleteLast())
            }

            Key.Escape -> {
                dispatch(XmbAction.BACK)
            }

            else -> {
                val action = xmbActionFor(event.key)
                val code = event.utf16CodePoint
                when {
                    action != null && action != XmbAction.PLAY_PAUSE -> {
                        val before = session.keyboard.text
                        dispatch(action)
                        keyboard
                            ?.keyboard
                            ?.text
                            ?.takeIf { it != before }
                            ?.let { if (session.forSearch) services?.search?.type(it) }
                    }

                    code >= 32 && code != 127 && !event.isCtrlPressed && !event.isMetaPressed && !event.isAltPressed -> {
                        update(session.keyboard.type(code.toChar()))
                    }

                    else -> {
                        return false
                    }
                }
            }
        }
        return true
    }

    fun setVolume(level: Float) {
        val volume = services?.volume ?: return
        volume.set(level.coerceIn(0f, 1f))
        showToast(if (level <= 0f) shortcutText.muted else shortcutText.volume((level * 100).roundToInt()))
    }

    /** Runs a keyboard shortcut (Settings > Keyboard Shortcuts). Playback ones work everywhere, like media keys. */
    fun runShortcut(shortcut: XmbShortcut) {
        val playback = services?.playback
        val state = playback?.state?.value
        if (fullScreen != null) overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
        when (shortcut) {
            XmbShortcut.PLAY_PAUSE -> {
                dispatch(XmbAction.PLAY_PAUSE)
            }

            XmbShortcut.NEXT_TRACK -> {
                dispatch(XmbAction.NEXT_TRACK)
            }

            XmbShortcut.PREVIOUS_TRACK -> {
                dispatch(XmbAction.PREVIOUS_TRACK)
            }

            XmbShortcut.SEEK_FORWARD, XmbShortcut.SEEK_BACK -> {
                if (playback == null || state?.currentSong == null) return
                val delta = if (shortcut == XmbShortcut.SEEK_FORWARD) SEEK_STEP_MS else -SEEK_STEP_MS
                val target = (playback.currentPositionMs() + delta).coerceAtLeast(0L)
                playback.seekTo(if (state.durationMs > 0) target.coerceAtMost(state.durationMs) else target)
            }

            XmbShortcut.SHUFFLE -> {
                if (playback == null || state == null) return
                playback.toggleShuffle()
                showToast(if (state.isShuffled) shortcutText.shuffleOff else shortcutText.shuffleOn)
            }

            XmbShortcut.REPEAT -> {
                if (playback == null || state == null) return
                playback.toggleRepeat()
                showToast(
                    when (state.repeatMode) {
                        RepeatMode.OFF -> shortcutText.repeatAll
                        RepeatMode.ALL -> shortcutText.repeatOne
                        RepeatMode.ONE -> shortcutText.repeatOff
                    },
                )
            }

            XmbShortcut.VOLUME_UP, XmbShortcut.VOLUME_DOWN -> {
                val volume = services?.volume ?: return
                setVolume(volume.level + if (shortcut == XmbShortcut.VOLUME_UP) VOLUME_STEP else -VOLUME_STEP)
            }

            XmbShortcut.MUTE -> {
                val volume = services?.volume ?: return
                if (volume.level > 0f) {
                    unmuteLevel = volume.level
                    setVolume(0f)
                } else {
                    setVolume(unmuteLevel)
                }
            }

            XmbShortcut.LIKE -> {
                val song = state?.currentSong ?: return
                val liked = services.isLiked(song.id)
                services.toggleLike(song)
                showToast(if (liked) shortcutText.unliked else shortcutText.liked)
            }

            XmbShortcut.VISUALIZER -> {
                if (fullScreen == XmbFullScreen.Video) playback?.setVideoMode(false)
                fullScreen = if (fullScreen is XmbFullScreen.Visualizer) null else XmbFullScreen.Visualizer(XmbVisualizerKind.WAVES)
                sounds.play(if (fullScreen == null) XmbSound.BACK else XmbSound.SELECT)
            }

            XmbShortcut.LYRICS -> {
                if (services?.lyrics == null) return
                when (val mode = fullScreen) {
                    is XmbFullScreen.Visualizer -> {
                        fullScreen = mode.copy(lyrics = !mode.lyrics)
                    }

                    XmbFullScreen.Lyrics -> {
                        fullScreen = null
                    }

                    else -> {
                        if (mode == XmbFullScreen.Video) playback?.setVideoMode(false)
                        fullScreen = XmbFullScreen.Lyrics
                    }
                }
                sounds.play(if (fullScreen == null) XmbSound.BACK else XmbSound.SELECT)
            }

            XmbShortcut.SEARCH -> {
                if (services?.search == null || fullScreen != null || keyboard != null) return
                options = null
                picker = null
                openKeyboard(KeyboardSession(XmbKeyboard(title = shortcutText.search), forSearch = true, onSubmit = ::search))
                sounds.play(XmbSound.SELECT)
            }
        }
    }

    /** A key while a shortcut waits for its new key: Esc cancels, Backspace clears, menu keys need a modifier. */
    fun recordKey(
        shortcut: XmbShortcut,
        event: KeyEvent,
    ) {
        when (val result = xmbShortcutCapture(XmbKeyChord.of(event))) {
            XmbShortcutCapture.Wait -> {
                Unit
            }

            XmbShortcutCapture.Cancel -> {
                recording = null
                sounds.play(XmbSound.BACK)
            }

            XmbShortcutCapture.Clear -> {
                Settings.shared.xmbShortcuts = prefs.shortcuts.bind(shortcut, null).encode()
                recording = null
                sounds.play(XmbSound.SELECT)
            }

            XmbShortcutCapture.Reserved -> {
                recordingRefused = true
                sounds.play(XmbSound.ERROR)
            }

            is XmbShortcutCapture.Bind -> {
                Settings.shared.xmbShortcuts = prefs.shortcuts.bind(shortcut, result.chord).encode()
                recording = null
                sounds.play(XmbSound.SELECT)
            }
        }
    }

    /** One action from a key, a touch or the platform ([input]): notice, full screen, then the menu. */
    fun act(action: XmbAction) {
        lastInput = seconds.floatValue
        if (recording != null) return
        if (notice != null) {
            val step = notice.notice.reduce(action, noticeFocus)
            noticeFocus = step.focus
            val chosen = step.chosen
            if (chosen != null) {
                sounds.play(XmbSound.NOTICE_CLOSE)
                notice.onChoice(chosen)
            } else {
                step.feedback.sound()?.let(sounds::play)
            }
            return
        }
        val mode = fullScreen
        // A visualizer the idle timer opened goes away on any menu key, like a screen saver.
        if (mode != null && idleVisualizer && action !in MEDIA_ACTIONS) {
            fullScreen = null
            idleVisualizer = false
            overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            feedback(action, XmbFeedback.BACK)
            return
        }
        if (mode != null) {
            // Full screen: ○ steps back (lyrics off, then the XMB), △ toggles lyrics over a
            // visualizer, media keys still work, and anything else shows the track.
            when (action) {
                XmbAction.BACK -> {
                    fullScreen =
                        if (mode is XmbFullScreen.Visualizer && mode.lyrics) {
                            mode.copy(lyrics = false)
                        } else {
                            if (mode == XmbFullScreen.Video) services?.playback?.setVideoMode(false)
                            null
                        }
                    // The track info slides back to its corner in view.
                    overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                    feedback(action, XmbFeedback.BACK)
                }

                XmbAction.OPTIONS -> {
                    if (mode is XmbFullScreen.Visualizer && services?.lyrics != null) {
                        fullScreen = mode.copy(lyrics = !mode.lyrics)
                        feedback(action, XmbFeedback.SELECT)
                    }
                    overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                }

                XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
                    dispatch(action)
                    overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                }

                else -> {
                    overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                }
            }
            return
        }
        dispatch(action)
    }

    /** A tap that hit nothing of its own: it closes a picker or the options, or shows the track full screen. */
    fun tapOutside() {
        when {
            recording != null || notice != null || keyboard != null -> Unit
            picker != null || options != null -> act(XmbAction.BACK)
            fullScreen != null -> act(XmbAction.SELECT)
        }
    }

    /** Whether something over the menu takes taps (or closes on them) instead of the menu. */
    fun menuCovered(): Boolean =
        options != null || picker != null || notice != null || keyboard != null || fullScreen != null || recording != null

    /** A tapped row: the focused one is selected (or its options open); another is focused first. */
    fun tapRow(
        index: Int,
        long: Boolean,
    ) {
        if (menuCovered()) return tapOutside()
        lastInput = seconds.floatValue
        val current = navigation.level?.focus ?: navigation.item
        if (index != current) {
            navigation = navigation.focusRow(index)
            val down = index > current
            feedback(if (down) XmbAction.DOWN else XmbAction.UP, if (down) XmbFeedback.MOVE_DOWN else XmbFeedback.MOVE_UP)
        }
        if (long) {
            act(XmbAction.OPTIONS)
        } else if (index == current) {
            act(XmbAction.SELECT)
        }
    }

    val taps =
        XmbTaps(
            category = { index ->
                when {
                    menuCovered() -> {
                        tapOutside()
                    }

                    navigation.stack.isEmpty() && index != navigation.category -> {
                        lastInput = seconds.floatValue
                        val right = index > navigation.category
                        navigation = navigation.focusCategory(index)
                        feedback(if (right) XmbAction.RIGHT else XmbAction.LEFT, XmbFeedback.MOVE_SIDE)
                    }
                }
            },
            // Rows of the top-level column answer only there; an opened level's rows take over.
            row = { index, long -> if (navigation.stack.isEmpty()) tapRow(index, long) },
            levelRow = { index, long -> if (navigation.stack.isNotEmpty()) tapRow(index, long) },
            parent = { if (menuCovered()) tapOutside() else act(XmbAction.BACK) },
            key = { row, column ->
                keyboard?.let { session ->
                    keyboard =
                        KeyboardSession(
                            session.keyboard.copy(row = row, column = column, suggestion = -1),
                            session.forSearch,
                            session.onSubmit,
                        )
                    act(XmbAction.SELECT)
                }
            },
            suggestion = { index ->
                keyboard?.let { session ->
                    keyboard = KeyboardSession(session.keyboard.copy(suggestion = index), session.forSearch, session.onSubmit)
                    act(XmbAction.SELECT)
                }
            },
            pickerValue = { index ->
                (picker as? XmbPicker.Choice)?.let { choice ->
                    picker = choice.copy(focus = index)
                    act(XmbAction.SELECT)
                }
            },
            sliderStep = { direction -> act(if (direction > 0) XmbAction.RIGHT else XmbAction.LEFT) },
            confirm = { act(XmbAction.SELECT) },
            option = { index ->
                options?.let { panel ->
                    options = panel.copy(focus = index)
                    act(XmbAction.SELECT)
                }
            },
            noticeButton = { index ->
                noticeFocus = index
                act(XmbAction.SELECT)
            },
        )
    val latestTapOutside by rememberUpdatedState<() -> Unit> { tapOutside() }

    // The pointer handler and the platform's input outlive compositions; they call the latest act().
    val latestAct by rememberUpdatedState<(XmbAction) -> Unit> { act(it) }
    val latestSuspended by rememberUpdatedState(suspended)
    LaunchedEffect(input) { input?.actions?.collect { latestAct(it) } }
    LaunchedEffect(input) { input?.toasts?.collect { showToast(it) } }
    // A link, app shortcut or notification: its category, with the level opened, over anything open.
    val pendingOpen = input?.pendingOpen
    // A link carries only an id: its level is titled with a generic word until it loads.
    val genericTitles =
        mapOf(
            "album" to stringResource(Res.string.album),
            "artist" to stringResource(Res.string.artist),
            "playlist" to stringResource(Res.string.playlist),
            "podcast" to stringResource(Res.string.podcast),
            "downloads" to stringResource(Res.string.downloads),
            "history" to stringResource(Res.string.history),
            "liked" to stringResource(Res.string.liked_songs),
        )
    LaunchedEffect(pendingOpen) {
        val target = input?.takeOpen() ?: return@LaunchedEffect
        if (target.nowPlaying) {
            // The player, the XMB way: the last visualizer full screen with the track, over whatever is open.
            if (services
                    ?.playback
                    ?.state
                    ?.value
                    ?.currentSong == null
            ) {
                return@LaunchedEffect
            }
            options = null
            picker = null
            keyboard = null
            if (fullScreen == null) fullScreen = XmbFullScreen.Visualizer(lastVisualizer)
            overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            return@LaunchedEffect
        }
        options = null
        picker = null
        keyboard = null
        // Leaving full-screen video turns the player's video mode off, as ○ does there.
        if (fullScreen == XmbFullScreen.Video) services?.playback?.setVideoMode(false)
        fullScreen = null
        val list = target.list
        val title =
            (list as? XmbList.SettingsPage)?.let { registry?.get(it.page)?.title }
                ?: target.title.ifBlank {
                    when (list) {
                        is XmbList.Album -> genericTitles["album"]
                        is XmbList.Artist -> genericTitles["artist"]
                        is XmbList.Playlist -> genericTitles["playlist"]
                        is XmbList.Podcast -> genericTitles["podcast"]
                        XmbList.Downloads -> genericTitles["downloads"]
                        XmbList.History -> genericTitles["history"]
                        XmbList.Liked -> genericTitles["liked"]
                        else -> null
                    }.orEmpty()
                }
        val icon =
            XmbIcon.Drawable(
                when (list) {
                    is XmbList.Album -> Res.drawable.xmb_item_album
                    is XmbList.Artist -> Res.drawable.xmb_item_artist
                    is XmbList.SearchResults -> Res.drawable.xmb_cat_search
                    is XmbList.SettingsPage -> Res.drawable.xmb_item_subsetting
                    else -> Res.drawable.xmb_item_playlist
                },
            )
        navigation = navigation.opening(target.copy(title = title), categories.map { it.items }, icon)
    }
    val canGoBack =
        navigation.stack.isNotEmpty() || options != null || picker != null || keyboard != null ||
            fullScreen != null || notice != null || recording != null
    SideEffect { input?.canGoBack = canGoBack }
    // Where touches land, in pixels: kept up to date by the layout below.
    val touchPlaces = remember { XmbTouchPlaces() }

    CompositionLocalProvider(LocalXmbTaps provides taps) {
        BoxWithConstraints(
            modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || latestSuspended) return@onKeyEvent false
                    // Android sends its back key through its own back handling (input.systemBack).
                    if (input?.systemBack == true && event.key == Key.Back) return@onKeyEvent false
                    lastInput = seconds.floatValue
                    recording?.let { shortcut ->
                        recordKey(shortcut, event)
                        return@onKeyEvent true
                    }
                    // Shortcuts (not over a notice, nor while typing on the keyboard, where letters are text).
                    if (notice == null && keyboard == null) {
                        prefs.shortcuts.match(XmbKeyChord.of(event))?.let { shortcut ->
                            runShortcut(shortcut)
                            return@onKeyEvent true
                        }
                    }
                    if (notice == null && fullScreen == null && keyboard != null) return@onKeyEvent keyboardKey(event)
                    val action = xmbActionFor(event.key)
                    if (action == null) {
                        // Full screen, any key shows the track.
                        if (fullScreen != null) overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                        return@onKeyEvent notice != null || fullScreen != null
                    }
                    act(action)
                    true
                }.pointerInput(
                    Unit,
                    XmbBackdropInput {
                        val velocity = VelocityTracker()
                        var fling: Job? = null
                        awaitPointerEventScope {
                            var touch: XmbTouchTracker? = null
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: continue
                                // A click anywhere on the XMB takes keyboard focus back (from a title bar, say).
                                if (event.type == PointerEventType.Press && !latestSuspended) {
                                    focusRequester.requestFocus()
                                    lastInput = seconds.floatValue
                                }
                                // Touch (not the mouse): drags step through the menu and a flick coasts on. Taps on the
                                // XMB's parts are theirs (xmbTap consumes them); a tap that hit none of them lands here.
                                if ((change.type == PointerType.Touch || change.type == PointerType.Stylus) && !latestSuspended) {
                                    when (event.type) {
                                        PointerEventType.Press -> {
                                            fling?.cancel()
                                            velocity.resetTracking()
                                            velocity.addPosition(change.uptimeMillis, change.position)
                                            touch =
                                                XmbTouchTracker(
                                                    touchPlaces.stepX,
                                                    touchPlaces.stepY,
                                                    viewConfiguration.touchSlop * TOUCH_SLOP,
                                                ).also {
                                                    it.down(change.position.x, change.position.y, change.uptimeMillis)
                                                }
                                        }

                                        PointerEventType.Move -> {
                                            velocity.addPosition(change.uptimeMillis, change.position)
                                            touch?.move(change.position.x, change.position.y)?.forEach { latestAct(it) }
                                        }

                                        PointerEventType.Release -> {
                                            val tracker = touch
                                            touch = null
                                            when (tracker?.up(change.uptimeMillis)) {
                                                XmbTouchGesture.TAP -> {
                                                    if (!change.isConsumed) latestTapOutside()
                                                }

                                                XmbTouchGesture.LONG_PRESS -> {
                                                    if (!change.isConsumed) latestTapOutside()
                                                }

                                                null -> {
                                                    val speed = velocity.calculateVelocity()
                                                    val steps = tracker?.flingSteps(if (tracker.vertical) speed.y else speed.x).orEmpty()
                                                    if (steps.isNotEmpty()) {
                                                        fling =
                                                            scope.launch {
                                                                steps.forEachIndexed { index, step ->
                                                                    delay(FLING_FIRST_MS + index * FLING_SLOWDOWN_MS)
                                                                    latestAct(step)
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        }

                                        else -> {
                                            Unit
                                        }
                                    }
                                    continue
                                }
                                if (event.type != PointerEventType.Scroll) continue
                                val delta = event.changes.fold(Offset.Zero) { sum, scroll -> sum + scroll.scrollDelta }
                                xmbActionForScroll(delta.x, delta.y)?.let {
                                    dispatch(it)
                                    event.changes.forEach { scroll -> scroll.consume() }
                                }
                            }
                        }
                    },
                ),
        ) {
            val metrics = remember(maxWidth, maxHeight) { XmbMetrics.forSize(maxWidth, maxHeight) }
            val density = LocalDensity.current
            val geometry =
                with(density) {
                    XmbBarGeometry(
                        anchorX = maxWidth.toPx() * metrics.anchorX,
                        barY = maxHeight.toPx() * XmbMetrics.BAR_Y,
                        categorySpacing = metrics.categorySpacing.toPx(),
                        categoryIcon = metrics.categoryIcon.toPx(),
                        categoryLabelOffset = metrics.categoryLabelOffset.toPx(),
                        itemIcon = metrics.itemIcon.toPx(),
                        itemSpacing = metrics.itemSpacing.toPx(),
                        focusedItemOffset = metrics.focusedItemOffset.toPx(),
                        itemsAboveGap = metrics.itemsAboveGap.toPx(),
                        width = maxWidth.toPx(),
                        itemsBelowGap = metrics.itemsBelowGap.toPx(),
                        parentOffset = metrics.parentOffset.toPx(),
                    )
                }
            touchPlaces.update(geometry)
            val styles = rememberXmbRowStyles(metrics, fontFamily)
            val backgroundColors = prefs.backgroundColors(now.month, now.hourOfDay)
            val wallpaperPath = prefs.wallpaperPath
            val wallpaper by produceState<ImageBitmap?>(null, wallpaperPath, wallpapers) {
                value = wallpaperPath?.let { path -> wallpapers?.load(path) }
            }
            if (videoShown < 1f) {
                // One picture for both: opening Waves turns the home sky and wave into it.
                XmbBackdrop(
                    colors = backgroundColors,
                    motion = motion,
                    audio = audio,
                    waves = { visualizerShown },
                    seconds = { seconds.floatValue },
                    modifier = Modifier.fillMaxSize(),
                    wave = prefs.wave,
                    sparkles = prefs.sparkles,
                    wallpaper = wallpaper,
                    wallpaperDim = prefs.wallpaperDim,
                )
            }
            // Valley and Gaia cover the backdrop (which turns to Waves beneath them); Auto-cycle crossfades.
            if ((visualizerShown > 0f && lastVisualizer != XmbVisualizerKind.WAVES) || visualizerMode?.cycle == true) {
                Crossfade(
                    targetState = lastVisualizer,
                    animationSpec = tween(CYCLE_FADE_MS),
                    modifier = Modifier.fillMaxSize().graphicsLayer { alpha = visualizerShown },
                    label = "visualizer",
                ) { kind ->
                    when (kind) {
                        XmbVisualizerKind.WAVES -> {
                            Unit
                        }

                        XmbVisualizerKind.VALLEY -> {
                            ValleyVisualizer(
                                audio,
                                { seconds.floatValue },
                                prefs.visualizerQuality,
                                Modifier.fillMaxSize(),
                            )
                        }

                        XmbVisualizerKind.GAIA -> {
                            GaiaVisualizer(audio, { seconds.floatValue }, prefs.visualizerQuality, Modifier.fillMaxSize())
                        }
                    }
                }
            }
            val video = services?.video
            if (video != null && (fullScreen == XmbFullScreen.Video || videoShown > 0f)) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = videoShown }.background(Color.Black)) {
                    video.Surface(Modifier.fillMaxSize())
                }
            }
            // The menu is gone by the time the track info has slid most of the way.
            val menuAlpha = { (1f - fullShown / MENU_FADE_SHARE).coerceIn(0f, 1f) }
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - drill) * (1f - KEYBOARD_FADE * keyboardShown) * menuAlpha() }) {
                XmbBar(
                    categories = categories,
                    navigation = navigation,
                    metrics = metrics,
                    geometry = geometry,
                    fontFamily = fontFamily,
                    listsEnabled = services != null,
                )
            }
            if (level != null) {
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = (1f - KEYBOARD_FADE * keyboardShown) * menuAlpha() }) {
                    XmbLevelView(level, listState, metrics, geometry, styles, visible = { drill }, dim = { pickerShown })
                }
            }
            services?.playback?.let { playback ->
                if (lyricsShown > 0f) {
                    val position by produceState(playback.currentPositionMs(), playingSong?.id, playbackState?.isPlaying) {
                        while (true) {
                            value = playback.currentPositionMs()
                            delay(LYRICS_POLL_MS)
                        }
                    }
                    val lines = playingSong?.let { services.lyrics?.lines(it.id) }
                    val message =
                        when {
                            playingSong == null -> text.nothingPlaying
                            lines == null -> text.loading
                            lines.isEmpty() -> text.noLyrics
                            else -> null
                        }
                    XmbLyricsView(
                        lines = lines.orEmpty(),
                        current = currentLyricIndex(lines.orEmpty(), position),
                        message = message,
                        metrics = metrics,
                        fontFamily = fontFamily,
                        modifier = Modifier.graphicsLayer { alpha = lyricsShown },
                    )
                }
                XmbTrackInfo(
                    playback = playback,
                    metrics = metrics,
                    fontFamily = fontFamily,
                    expand = { fullShown },
                    alpha = {
                        // Full screen, the track shows after opening and each key press, then fades out
                        // (or always, or never: Music Player Settings). The menu's card can be turned off.
                        val overlay =
                            when (prefs.trackOverlay) {
                                XmbTrackOverlay.BRIEF -> ((overlayUntil - seconds.floatValue) / VISUALIZER_OVERLAY_FADE).coerceIn(0f, 1f)
                                XmbTrackOverlay.ALWAYS -> 1f
                                XmbTrackOverlay.NEVER -> 0f
                            }
                        val card = if (prefs.nowPlayingCard) 1f else 0f
                        card + (overlay - card) * fullShown
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (prefs.clock) {
                XmbClock(
                    xmbClockText(now.month, now.day, now.hour, now.minute, prefs.hour24, prefs.dateFormat),
                    metrics,
                    fontFamily,
                    Modifier.align(Alignment.TopEnd).padding(top = metrics.clockBoxTop).graphicsLayer {
                        alpha =
                            menuAlpha()
                    },
                )
            }
            XmbOptionsPanelView(options, metrics, fontFamily)
            picker?.let { current ->
                val rowTitle =
                    level?.let { listState.entries.getOrNull(it.focus)?.title }
                        ?: itemTitles[navigation.category].getOrNull(navigation.item)
                Box(Modifier.fillMaxSize().graphicsLayer { alpha = pickerShown }) {
                    XmbPickerView(current, rowTitle, metrics, geometry, styles)
                }
            }
            keyboard?.let { XmbKeyboardView(it.keyboard, metrics, fontFamily) }
            notice?.let { XmbNoticeView(it.notice, noticeFocus, metrics, fontFamily) }
            recording?.let { shortcut ->
                XmbNoticeView(
                    XmbNotice(
                        id = "record_shortcut",
                        title = stringResource(shortcut.title()),
                        message = shortcutText.press,
                        details = if (recordingRefused) shortcutText.reserved else shortcutText.hint,
                        buttons = emptyList(),
                    ),
                    focus = 0,
                    metrics = metrics,
                    fontFamily = fontFamily,
                )
            }
            toast?.let { message ->
                XmbToast(
                    message,
                    metrics,
                    fontFamily,
                    Modifier.align(Alignment.TopCenter).padding(top = metrics.clockBoxTop).graphicsLayer {
                        alpha = ((toastUntil - seconds.floatValue) / TOAST_FADE_SECONDS).coerceIn(0f, 1f)
                    },
                )
            }
        }
    }

    // Keys belong to the XMB whenever the window is active and no stock dialog is over it.
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    LaunchedEffect(suspended, windowFocused) { if (!suspended && windowFocused) focusRequester.requestFocus() }
}

/** The account as `Settings.shared` has it; reads are snapshot state, so sign-in changes recompose. */
private fun xmbAccountInfo(): XmbAccountInfo {
    val settings = Settings.shared
    return XmbAccountInfo(
        signedIn = AuthManager().isLoggedIn,
        name = settings.accountName,
        detail = settings.accountEmail,
        thumbnail = settings.accountThumbnail,
    )
}

/**
 * The clock in its box at the top right, as on the console: a thin light border on the top,
 * left and bottom, running off the right edge, with the date and time and the clock icon after it.
 */
@Composable
private fun XmbClock(
    text: String,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier =
            modifier
                .size(metrics.clockBoxWidth, metrics.clockBoxHeight)
                .drawBehind {
                    val border = metrics.clockBorder.toPx()
                    drawRect(Color.Black.copy(alpha = 0.06f))
                    val line = Color.White.copy(alpha = 0.33f)
                    drawRect(line, size = size.copy(height = border))
                    drawRect(line, topLeft = Offset(0f, size.height - border), size = size.copy(height = border))
                    drawRect(line, size = size.copy(width = border))
                },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = metrics.clockEndPadding - metrics.clockIcon),
        ) {
            BasicText(
                text = text,
                style = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { metrics.clockSize.toSp() }),
            )
            Spacer(Modifier.width(metrics.unit * 10))
            Image(
                painterResource(Res.drawable.xmb_ui_clock),
                contentDescription = null,
                modifier =
                    Modifier.size(metrics.clockIcon).graphicsLayer {
                        scaleX = CLOCK_ICON_ZOOM
                        scaleY = CLOCK_ICON_ZOOM
                    },
            )
        }
    }
}

/** Wall-clock time, refreshed every 15 seconds; a fixed [source] makes it deterministic. */
@Composable
private fun rememberXmbTime(source: (() -> XmbTime)?): XmbTime {
    var now by remember { mutableStateOf(source?.invoke() ?: XmbTime.now()) }
    if (source == null) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(15_000)
                now = XmbTime.now()
            }
        }
    }
    return now
}

/**
 * Seconds since the XMB appeared, advanced once per frame while the window is visible, along
 * with the wave's [motion] and the visualizer's [audio]. Readers use them in draw lambdas, so frames don't recompose.
 */
@Composable
private fun rememberAnimationSeconds(
    animate: Boolean,
    motion: XmbWaveMotion,
    audio: VisualizerAudio,
    frameRate: () -> Int,
    waveSpeed: () -> Float,
): MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    val visibility = LocalVisualWorkActive.current
    LaunchedEffect(animate, visibility) {
        if (!animate) return@LaunchedEffect
        visibility.collectLatest { shown ->
            if (!shown) return@collectLatest
            val origin = withFrameMillis { it } - (seconds.floatValue * 1000f).toLong()
            while (true) {
                withFrameMillis {
                    // At 30 frames a second, every other display frame is skipped (nothing redraws).
                    if (frameRate() < 60 && it - origin - (seconds.floatValue * 1000f).toLong() < HALF_RATE_MS) return@withFrameMillis
                    val next = (it - origin) / 1000f
                    // Wave speed (Theme Settings) scales the home wave's clock.
                    motion.advance((next - seconds.floatValue) * waveSpeed())
                    audio.step(next - seconds.floatValue)
                    seconds.floatValue = next
                }
            }
        }
    }
    return seconds
}

/**
 * Full screen: how long the move in or out takes (the backdrop's blend, the track info's slide),
 * the share of it the menu takes to fade, and how long the track overlay stays after a key.
 */
private const val VISUALIZER_FADE_MS = 900
private const val MENU_FADE_SHARE = 0.6f
private const val VISUALIZER_OVERLAY_SECONDS = 4f
private const val VISUALIZER_OVERLAY_FADE = 0.6f

/** Sleep timer lengths offered, in minutes. */
private val SLEEP_MINUTES = listOf(15, 30, 45, 60, 90, 120)

/** Keyboard shortcuts: seek and volume steps, the volume after unmuting from zero, and the toast's timing. */
private const val SEEK_STEP_MS = 10_000L
private const val VOLUME_STEP = 0.05f
private const val DEFAULT_UNMUTE_VOLUME = 0.7f
private const val TOAST_SECONDS = 1.6f
private const val TOAST_FADE_SECONDS = 0.4f

/** Touch: the drag slop (times the platform's, so a wobbly tap isn't a drag), and a flick's pacing. */
private const val TOUCH_SLOP = 2f
private const val FLING_FIRST_MS = 40L
private const val FLING_SLOWDOWN_MS = 12L

/** Auto-cycle: how long each visualizer stays, and the crossfade between them. */
private const val CYCLE_MS = 45_000L
private const val CYCLE_FADE_MS = 1500

/** At 30 frames a second, the least time between two animation frames. */
private const val HALF_RATE_MS = 30L

/** How often the idle timer checks for the visualizer. */
private const val IDLE_CHECK_MS = 1000L

private val MEDIA_ACTIONS = setOf(XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK)

/** How often the lyrics view reads the playback position. */
private const val LYRICS_POLL_MS = 100L

/** How far the menu fades behind the on-screen keyboard. */
private const val KEYBOARD_FADE = 0.8f

/** The clock glyph fills 56 of its PNG's 256 pixels; zoom it to fill its slot. */
private const val CLOCK_ICON_ZOOM = 4.5f

/** The menu's step sizes in pixels for touch, written by the layout and read by the pointer handler. */
private class XmbTouchPlaces {
    var stepX = 1f
    var stepY = 1f

    fun update(geometry: XmbBarGeometry) {
        stepX = geometry.categorySpacing
        stepY = geometry.itemSpacing
    }
}
