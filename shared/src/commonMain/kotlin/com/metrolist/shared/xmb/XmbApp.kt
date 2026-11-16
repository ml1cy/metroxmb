package com.metrolist.shared.xmb

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.metrolist.shared.auth.AuthManager
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.no
import com.metrolist.shared.generated.resources.xmb_cat_search
import com.metrolist.shared.generated.resources.xmb_item_subsetting
import com.metrolist.shared.generated.resources.xmb_ui_clock
import com.metrolist.shared.generated.resources.xmb_users_sign_out_confirm
import com.metrolist.shared.generated.resources.xmb_value_off
import com.metrolist.shared.generated.resources.xmb_value_on
import com.metrolist.shared.generated.resources.yes
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.components.LocalVisualWorkActive
import com.metrolist.shared.ui.settings.SettingsControl
import com.metrolist.shared.ui.settings.rememberMetrolistSettingsRegistry
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.XmbBackground
import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.background.XmbWaveNudge
import com.metrolist.shared.visualizer.rememberVisualizerAudio
import com.metrolist.shared.visualizer.waves.WavesVisualizer
import com.metrolist.shared.xmb.adapters.XmbEntryKind
import com.metrolist.shared.xmb.adapters.XmbList
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.XmbSettingLabels
import com.metrolist.shared.xmb.adapters.XmbSettingStep
import com.metrolist.shared.xmb.adapters.plainNumber
import com.metrolist.shared.xmb.adapters.toXmbEntries
import com.metrolist.shared.xmb.adapters.xmbSettingStep
import com.metrolist.shared.xmb.bar.XmbBar
import com.metrolist.shared.xmb.bar.XmbBarGeometry
import com.metrolist.shared.xmb.bar.XmbLevelView
import com.metrolist.shared.xmb.bar.rememberXmbRowStyles
import com.metrolist.shared.xmb.nav.XmbAccountInfo
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbKeyboard
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.XmbOptionsPanel
import com.metrolist.shared.xmb.nav.XmbPicker
import com.metrolist.shared.xmb.nav.XmbVisualizerKind
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.resolve
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.nav.xmbActionForScroll
import com.metrolist.shared.xmb.nav.xmbCategories
import com.metrolist.shared.xmb.nav.xmbSearchItems
import com.metrolist.shared.xmb.nav.xmbSettingsItems
import com.metrolist.shared.xmb.nav.xmbUserItems
import com.metrolist.shared.xmb.screens.XmbKeyboardView
import com.metrolist.shared.xmb.screens.XmbNowPlaying
import com.metrolist.shared.xmb.screens.XmbOptionsPanelView
import com.metrolist.shared.xmb.screens.XmbPickerView
import com.metrolist.shared.xmb.screens.XmbVisualizerOverlay
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import com.metrolist.shared.xmb.sound.sound
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import com.metrolist.shared.xmb.theme.XmbPalette
import com.metrolist.shared.xmb.theme.rememberXmbFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
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
    time: (() -> XmbTime)? = null,
    animate: Boolean = true,
    initialCategory: XmbCategoryId = XmbCategoryId.MUSIC,
    initialItem: Int = 0,
    initialLevels: List<XmbLevel> = emptyList(),
    initialOptions: Boolean = false,
    initialPicker: XmbPicker? = null,
    initialKeyboard: XmbKeyboard? = null,
    initialVisualizer: XmbVisualizerKind? = null,
) {
    val sounds = LocalXmbSoundPlayer.current
    val now = rememberXmbTime(time)
    val fontFamily = rememberXmbFontFamily()
    val motion = remember { XmbWaveMotion() }
    var visualizer by remember { mutableStateOf(initialVisualizer) }
    val audio = rememberVisualizerAudio(active = visualizer != null && animate)
    val seconds = rememberAnimationSeconds(animate, motion, audio)
    // The full-screen visualizer's track overlay shows until this time, then fades.
    var overlayUntil by remember { mutableFloatStateOf(VISUALIZER_OVERLAY_SECONDS) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val actions = remember(services, scope) { services?.let { XmbActions(it, scope) } }

    // Live columns: settings pages from the registry, recent searches, the signed-in account.
    var settingsRevision by remember { mutableIntStateOf(0) }
    val registry = services?.settings?.let { rememberMetrolistSettingsRegistry(it.host(), settingsRevision) }
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
    val categories =
        xmbCategories(
            users = xmbUserItems(account),
            settings = registry?.let(::xmbSettingsItems).orEmpty(),
            search = xmbSearchItems(history),
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
    val musicState by remember(level?.list?.key, services) {
        val list = level?.list
        if (list == null || services == null ||
            settingsPage != null
        ) {
            flowOf(XmbListState.loaded(emptyList()))
        } else {
            services.music.open(list)
        }
    }.collectAsState(XmbListState())
    val listState =
        if (settingsPage != null) {
            val page = registry?.get(settingsPage)
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
        } else {
            musicState
        }
    val drill by animateFloatAsState(if (navigation.stack.isEmpty()) 0f else 1f, tween(XmbMotion.FADE_MS))
    val pickerShown by animateFloatAsState(if (picker != null) 1f else 0f, tween(XmbMotion.FADE_MS))
    val keyboardShown by animateFloatAsState(if (keyboard != null) 1f else 0f, tween(XmbMotion.FADE_MS))
    val visualizerShown by animateFloatAsState(if (visualizer != null) 1f else 0f, tween(VISUALIZER_FADE_MS))

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
        nudge?.let(motion::nudge)
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

    fun commitSetting(change: () -> Unit) {
        change()
        settingsRevision++
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
                visualizer = action.kind
                overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
            }

            is XmbItemAction.StockSettings -> {
                services?.settings?.openStockPage?.invoke(action.page) ?: return XmbFeedback.ERROR
            }
        }
        return XmbFeedback.SELECT
    }

    fun selectSetting(index: Int): XmbFeedback {
        val entry = listState.entries.getOrNull(index) ?: return XmbFeedback.ERROR
        val page = settingsPage ?: return XmbFeedback.ERROR
        val currentRegistry = registry ?: return XmbFeedback.ERROR
        when (val step = xmbSettingStep(entry, page, currentRegistry, labels)) {
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
                val target = currentRegistry[step.page] ?: return XmbFeedback.ERROR
                navigation =
                    navigation.push(
                        XmbLevel(XmbList.SettingsPage(step.page), target.title, XmbIcon.Drawable(Res.drawable.xmb_item_subsetting)),
                    )
            }

            is XmbSettingStep.Stock -> {
                services?.settings?.openStockPage?.invoke(step.page) ?: return XmbFeedback.ERROR
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
                actions?.run(chosen, entries, navigation.level?.focus ?: -1)?.let { navigation = navigation.push(it) }
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

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || suspended) return@onKeyEvent false
                if (visualizer != null) {
                    // Full screen: ○ goes back to the XMB, media keys still work, anything else shows the track.
                    when (val action = xmbActionFor(event.key)) {
                        XmbAction.BACK -> {
                            visualizer = null
                            feedback(action, XmbFeedback.BACK)
                        }

                        XmbAction.PLAY_PAUSE, XmbAction.NEXT_TRACK, XmbAction.PREVIOUS_TRACK -> {
                            dispatch(action)
                            overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                        }

                        else -> {
                            overlayUntil = seconds.floatValue + VISUALIZER_OVERLAY_SECONDS
                        }
                    }
                    return@onKeyEvent true
                }
                if (keyboard != null) return@onKeyEvent keyboardKey(event)
                val action = xmbActionFor(event.key) ?: return@onKeyEvent false
                dispatch(action)
                true
            }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val delta = event.changes.fold(Offset.Zero) { sum, change -> sum + change.scrollDelta }
                        xmbActionForScroll(delta.x, delta.y)?.let {
                            dispatch(it)
                            event.changes.forEach { change -> change.consume() }
                        }
                    }
                }
            },
    ) {
        val metrics = remember(maxWidth, maxHeight) { XmbMetrics.forSize(maxWidth, maxHeight) }
        val density = LocalDensity.current
        val geometry =
            with(density) {
                XmbBarGeometry(
                    anchorX = maxWidth.toPx() * XmbMetrics.ANCHOR_X,
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
        val styles = rememberXmbRowStyles(metrics, fontFamily)
        val backgroundColors = XmbPalette.backgroundColors(now.month, now.hourOfDay)
        if (visualizerShown < 1f) {
            XmbBackground(
                colors = backgroundColors,
                modifier = Modifier.fillMaxSize(),
                seconds = { seconds.floatValue },
                motion = motion,
            )
        }
        if (visualizer != null || visualizerShown > 0f) {
            WavesVisualizer(
                audio = audio,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = visualizerShown },
            )
        }
        val menuAlpha = { 1f - visualizerShown }
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
            XmbNowPlaying(
                playback,
                metrics,
                fontFamily,
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = metrics.edgeMargin, bottom = metrics.edgeMargin)
                    .graphicsLayer { alpha = menuAlpha() },
            )
            if (visualizerShown > 0f) {
                XmbVisualizerOverlay(
                    playback,
                    metrics,
                    fontFamily,
                    Modifier.graphicsLayer {
                        // Full screen, the track shows after opening and each key press, then fades out.
                        val overlay = ((overlayUntil - seconds.floatValue) / VISUALIZER_OVERLAY_FADE).coerceIn(0f, 1f)
                        alpha = visualizerShown * overlay
                    },
                )
            }
        }
        XmbClock(
            now,
            metrics,
            fontFamily,
            Modifier.align(Alignment.TopEnd).padding(top = metrics.clockBoxTop).graphicsLayer {
                alpha =
                    menuAlpha()
            },
        )
        XmbOptionsPanelView(options, metrics, fontFamily)
        picker?.let { current ->
            val rowTitle =
                level?.let { listState.entries.getOrNull(it.focus)?.title } ?: itemTitles[navigation.category].getOrNull(navigation.item)
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = pickerShown }) {
                XmbPickerView(current, rowTitle, metrics, geometry, styles)
            }
        }
        keyboard?.let { XmbKeyboardView(it.keyboard, metrics, fontFamily) }
    }

    LaunchedEffect(suspended) { if (!suspended) focusRequester.requestFocus() }
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
    now: XmbTime,
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
                text = now.clockText,
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
                    val next = (it - origin) / 1000f
                    motion.advance(next - seconds.floatValue)
                    audio.step(next - seconds.floatValue)
                    seconds.floatValue = next
                }
            }
        }
    }
    return seconds
}

/** Full-screen visualizer: fade in or out, and how long the track overlay stays after a key. */
private const val VISUALIZER_FADE_MS = 600
private const val VISUALIZER_OVERLAY_SECONDS = 4f
private const val VISUALIZER_OVERLAY_FADE = 0.6f

/** How far the menu fades behind the on-screen keyboard. */
private const val KEYBOARD_FADE = 0.8f

/** The clock glyph fills 56 of its PNG's 256 pixels; zoom it to fill its slot. */
private const val CLOCK_ICON_ZOOM = 4.5f
