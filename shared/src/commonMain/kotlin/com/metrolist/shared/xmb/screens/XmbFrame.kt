package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.back
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.waves.XmbBackdrop
import com.metrolist.shared.xmb.XmbTime
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.XmbTaps
import com.metrolist.shared.xmb.nav.XmbAction
import com.metrolist.shared.xmb.nav.XmbNotice
import com.metrolist.shared.xmb.nav.reduce
import com.metrolist.shared.xmb.nav.xmbActionFor
import com.metrolist.shared.xmb.settings.XmbPrefs
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.rememberXmbFontFamily
import org.jetbrains.compose.resources.stringResource

/** A button on an XMB screen outside the menu (a notice, a sign-in frame). */
class XmbFrameButton(
    val label: String,
    val run: () -> Unit,
)

/**
 * The XMB's sky and wave on their own, for screens shown outside the menu (first-run setup,
 * sign-in, the crash screen). [content] gets the metrics and font for the size it's drawn at.
 */
@Composable
fun XmbBackdropScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.(XmbMetrics, FontFamily) -> Unit,
) {
    val fontFamily = rememberXmbFontFamily()
    val prefs = XmbPrefs.current()
    val motion = remember { XmbWaveMotion() }
    val audio = remember { VisualizerAudio() }
    var seconds by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameMillis { it }
        while (true) {
            withFrameMillis { now ->
                val next = (now - start) / 1000f
                motion.advance(next - seconds)
                seconds = next
            }
        }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(Color.Black)) {
        val metrics = remember(maxWidth, maxHeight) { XmbMetrics.forSize(maxWidth, maxHeight) }
        val now = remember { XmbTime.now() }
        XmbBackdrop(
            colors = prefs.backgroundColors(now.month, now.hourOfDay),
            motion = motion,
            audio = audio,
            waves = { 0f },
            seconds = { seconds },
            modifier = Modifier.fillMaxSize(),
            wave = prefs.wave,
            sparkles = prefs.sparkles,
        )
        content(metrics, fontFamily)
    }
}

/**
 * A full-screen notice the console way: the notice band with [title], [message], [details] and
 * [buttons]. ←/→ (or a D-pad) choose, ✕/Enter/Space confirms, ○/Esc runs [onBack]; buttons take
 * taps and clicks. With no buttons it is a wait. [backdrop] false draws it over whatever is shown
 * (dimmed) instead of the XMB's sky. [id] resets the focus when the notice changes.
 */
@Composable
fun XmbNoticeScreen(
    message: String,
    buttons: List<XmbFrameButton>,
    modifier: Modifier = Modifier,
    title: String? = null,
    details: String? = null,
    onBack: (() -> Unit)? = null,
    id: String = message,
    backdrop: Boolean = true,
) {
    var focus by remember(id) { mutableIntStateOf(0) }
    val notice =
        XmbNotice(
            id = id,
            title = title,
            message = message,
            details = details,
            buttons = buttons.map { it.label },
            focus = focus,
        )
    val latestButtons by rememberUpdatedState(buttons)
    val latestBack by rememberUpdatedState(onBack)

    fun act(action: XmbAction) {
        if (action == XmbAction.BACK) {
            latestBack?.invoke()
            return
        }
        if (latestButtons.isEmpty()) return
        val step = notice.reduce(action, focus)
        focus = step.focus
        step.chosen?.let { latestButtons.getOrNull(it)?.run?.invoke() }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(id) { runCatching { focusRequester.requestFocus() } }
    val taps =
        remember {
            XmbTaps(
                category = {},
                row = { _, _ -> },
                levelRow = { _, _ -> },
                parent = {},
                key = { _, _ -> },
                suggestion = {},
                pickerValue = {},
                sliderStep = {},
                confirm = {},
                option = {},
                noticeButton = { index ->
                    focus = index
                    latestButtons.getOrNull(index)?.run?.invoke()
                },
            )
        }
    val keys =
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val action = if (event.key == Key.Spacebar) XmbAction.SELECT else xmbActionFor(event.key)
                action?.let(::act)
                action != null
            }
    if (backdrop) {
        XmbBackdropScreen(keys) { metrics, fontFamily ->
            CompositionLocalProvider(LocalXmbTaps provides taps) { XmbNoticeView(notice, focus, metrics, fontFamily) }
        }
    } else {
        val fontFamily = rememberXmbFontFamily()
        BoxWithConstraints(keys) {
            val metrics = remember(maxWidth, maxHeight) { XmbMetrics.forSize(maxWidth, maxHeight) }
            CompositionLocalProvider(LocalXmbTaps provides taps) { XmbNoticeView(notice, focus, metrics, fontFamily) }
        }
    }
}

/**
 * A web page in an XMB frame (Google's sign-in, say): the XMB's sky behind, [title] at the top,
 * the page in the middle, and at the bottom [hint] with the frame's buttons (Back first, then
 * [buttons]). The page takes the keyboard, so the buttons take taps and clicks; the platform
 * maps its back key to [onBack].
 */
@Composable
fun XmbWebFrame(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    buttons: List<XmbFrameButton> = emptyList(),
    content: @Composable BoxScope.() -> Unit,
) {
    val back = stringResource(Res.string.back)
    XmbBackdropScreen(modifier) { metrics, fontFamily ->
        val density = LocalDensity.current
        val unit = metrics.unit

        fun style(
            size: Float,
            alpha: Float = 1f,
        ) = TextStyle(
            color = Color.White.copy(alpha = alpha),
            fontFamily = fontFamily,
            fontSize = with(density) { (unit * size).toSp() },
        )
        val margin = unit * if (metrics.portrait) 20 else 48
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            BasicText(
                title,
                style = style(26f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = margin, end = margin, top = unit * 28, bottom = unit * 12),
            )
            Box(Modifier.fillMaxWidth().height(unit * 1.33f).background(Color.White.copy(alpha = 0.35f)))
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = margin, vertical = unit * 16)
                    .clip(RoundedCornerShape(unit * 6))
                    .border(unit * 1.33f, Color.White.copy(alpha = 0.35f), RoundedCornerShape(unit * 6))
                    .background(Color.White),
                content = content,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(unit * 24),
                modifier = Modifier.fillMaxWidth().padding(start = margin, end = margin, bottom = unit * 20),
            ) {
                BasicText(
                    hint.orEmpty(),
                    style = style(15f, 0.7f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                (listOf(XmbFrameButton(back, onBack)) + buttons).forEach { button ->
                    BasicText(
                        button.label,
                        style = style(19f),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(unit * 4))
                                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = button.run)
                                .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(unit * 4))
                                .padding(horizontal = unit * 18, vertical = unit * 6),
                    )
                }
            }
        }
    }
}
