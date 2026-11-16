package com.metrolist.shared.xmb

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_ui_clock
import com.metrolist.shared.ui.components.LocalVisualWorkActive
import com.metrolist.shared.visualizer.background.XmbBackground
import com.metrolist.shared.xmb.bar.XmbBar
import com.metrolist.shared.xmb.bar.XmbBarGeometry
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbCategoryId
import com.metrolist.shared.xmb.nav.XmbFeedback
import com.metrolist.shared.xmb.nav.XmbItemAction
import com.metrolist.shared.xmb.nav.XmbNavigation
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.nav.xmbActionForScroll
import com.metrolist.shared.xmb.nav.xmbCategories
import com.metrolist.shared.xmb.sound.LocalXmbSoundPlayer
import com.metrolist.shared.xmb.sound.sound
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import com.metrolist.shared.xmb.theme.XmbPalette
import com.metrolist.shared.xmb.theme.rememberXmbFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI
import kotlin.math.sin
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

/**
 * The XMB front end: background, category row with item columns, and the clock. [onSwitchToStock]
 * switches the interface style back to the stock screens. [time] and [animate] exist so dev
 * snapshots render a fixed scene.
 */
@Composable
fun XmbApp(
    onSwitchToStock: () -> Unit,
    modifier: Modifier = Modifier,
    time: (() -> XmbTime)? = null,
    animate: Boolean = true,
    initialCategory: XmbCategoryId = XmbCategoryId.MUSIC,
    initialItem: Int = 0,
) {
    val categories = remember { xmbCategories() }
    val itemCounts = remember(categories) { categories.map { it.items.size } }
    var navigation by remember {
        val start = XmbNavigation.initial(itemCounts, initialCategory.ordinal)
        val item = initialItem.coerceIn(0, (itemCounts[start.category] - 1).coerceAtLeast(0))
        mutableStateOf(start.copy(items = start.items.toMutableList().also { it[start.category] = item }))
    }
    val sounds = LocalXmbSoundPlayer.current
    val switchToStock by rememberUpdatedState(onSwitchToStock)
    val now = rememberXmbTime(time)
    val fontFamily = rememberXmbFontFamily()
    val seconds = rememberAnimationSeconds(animate)
    val focusRequester = remember { FocusRequester() }

    fun dispatch(action: XmbAction) {
        val step = navigation.reduce(action, itemCounts)
        navigation = step.navigation
        var feedback = step.feedback
        step.selected?.let { selection ->
            when (categories[selection.category].items[selection.item].action) {
                XmbItemAction.NONE -> feedback = XmbFeedback.ERROR
                XmbItemAction.SWITCH_TO_METROLIST -> switchToStock()
            }
        }
        feedback.sound()?.let(sounds::play)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val action = xmbActionFor(event.key) ?: return@onKeyEvent false
                dispatch(action)
                true
            }.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type != PointerEventType.Scroll) continue
                        val delta = event.changes.fold(androidx.compose.ui.geometry.Offset.Zero) { sum, change -> sum + change.scrollDelta }
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
                )
            }
        XmbBackground(
            colors = XmbPalette.backgroundColors(now.month, now.hourOfDay),
            modifier = Modifier.fillMaxSize(),
            seconds = { seconds.floatValue },
        )
        Box(Modifier.fillMaxSize()) {
            XmbBar(
                categories = categories,
                navigation = navigation,
                metrics = metrics,
                geometry = geometry,
                fontFamily = fontFamily,
                glow = { glowStrength(seconds.floatValue) },
            )
        }
        XmbClock(now, metrics, fontFamily, Modifier.align(Alignment.TopEnd).padding(top = metrics.edgeMargin, end = metrics.edgeMargin))
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

@Composable
private fun XmbClock(
    now: XmbTime,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(Res.drawable.xmb_ui_clock),
            contentDescription = null,
            modifier =
                Modifier.size(metrics.clockSize * 1.4f).graphicsLayer {
                    scaleX = CLOCK_ICON_ZOOM
                    scaleY = CLOCK_ICON_ZOOM
                },
        )
        Spacer(Modifier.width(metrics.unit * 8))
        BasicText(
            text = now.clockText,
            style = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { metrics.clockSize.toSp() }),
        )
    }
}

/** Wall-clock time, refreshed every minute; a fixed [source] makes it deterministic. */
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
 * Seconds since the XMB appeared, advanced once per frame while the window is visible. Readers
 * use it in draw lambdas, so frames don't recompose.
 */
@Composable
private fun rememberAnimationSeconds(animate: Boolean): androidx.compose.runtime.MutableFloatState {
    val seconds = remember { mutableFloatStateOf(0f) }
    val visibility = LocalVisualWorkActive.current
    LaunchedEffect(animate, visibility) {
        if (!animate) return@LaunchedEffect
        visibility.collectLatest { shown ->
            if (!shown) return@collectLatest
            val origin = withFrameMillis { it } - (seconds.floatValue * 1000f).toLong()
            while (true) {
                withFrameMillis { seconds.floatValue = (it - origin) / 1000f }
            }
        }
    }
    return seconds
}

/** The clock glyph fills 56 of its PNG's 256 pixels; zoom it to fill its slot. */
private const val CLOCK_ICON_ZOOM = 4f

private fun glowStrength(seconds: Float): Float {
    val phase = 0.5f + 0.5f * sin(2f * PI.toFloat() * seconds * 1000f / XmbMotion.GLOW_PULSE_MS)
    return XmbMotion.GLOW_MIN + (XmbMotion.GLOW_MAX - XmbMotion.GLOW_MIN) * phase
}
