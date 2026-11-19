package com.metrolist.shared.xmb.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.action_download
import com.metrolist.shared.generated.resources.action_like
import com.metrolist.shared.generated.resources.add_to_playlist
import com.metrolist.shared.generated.resources.add_to_queue
import com.metrolist.shared.generated.resources.play
import com.metrolist.shared.generated.resources.play_next
import com.metrolist.shared.generated.resources.remove_download
import com.metrolist.shared.generated.resources.shuffle
import com.metrolist.shared.generated.resources.start_radio
import com.metrolist.shared.generated.resources.unlike
import com.metrolist.shared.generated.resources.view_album
import com.metrolist.shared.generated.resources.view_artist
import com.metrolist.shared.xmb.bar.LocalXmbTaps
import com.metrolist.shared.xmb.bar.xmbTap
import com.metrolist.shared.xmb.nav.XmbOption
import com.metrolist.shared.xmb.nav.XmbOptionsPanel
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val XmbOption.label: StringResource
    get() =
        when (this) {
            XmbOption.PLAY -> Res.string.play
            XmbOption.SHUFFLE -> Res.string.shuffle
            XmbOption.PLAY_NEXT -> Res.string.play_next
            XmbOption.ADD_TO_QUEUE -> Res.string.add_to_queue
            XmbOption.START_RADIO -> Res.string.start_radio
            XmbOption.ADD_TO_PLAYLIST -> Res.string.add_to_playlist
            XmbOption.LIKE -> Res.string.action_like
            XmbOption.UNLIKE -> Res.string.unlike
            XmbOption.DOWNLOAD -> Res.string.action_download
            XmbOption.REMOVE_DOWNLOAD -> Res.string.remove_download
            XmbOption.VIEW_ALBUM -> Res.string.view_album
            XmbOption.VIEW_ARTIST -> Res.string.view_artist
        }

/**
 * The △ options panel: a dark band sliding in from the right with the row's actions, the focused
 * one glowing. Stays composed while it slides out after [panel] becomes null.
 */
@Composable
internal fun XmbOptionsPanelView(
    panel: XmbOptionsPanel?,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
) {
    val shown = remember { Animatable(0f) }
    val lastPanel = remember { mutableStateOf<XmbOptionsPanel?>(null) }
    SideEffect { if (panel != null) lastPanel.value = panel }
    LaunchedEffect(panel != null) {
        shown.animateTo(if (panel != null) 1f else 0f, tween(XmbMotion.FADE_MS, easing = XmbMotion.scrollEasing))
    }
    val current = panel ?: lastPanel.value ?: return
    if (panel == null && shown.value == 0f) return
    val density = LocalDensity.current
    val panelWidth = metrics.unit * 360
    val style = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { metrics.itemTitleSize.toSp() })
    val glow = Shadow(Color.White.copy(alpha = 0.7f), Offset.Zero, with(density) { metrics.unit.toPx() * 8f })
    Box(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(metrics.unit * 22, Alignment.CenterVertically),
            modifier =
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(panelWidth)
                    .graphicsLayer {
                        translationX = (1f - shown.value) * panelWidth.toPx()
                        alpha = shown.value
                    }.background(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.65f))))
                    .padding(start = metrics.unit * 40, end = metrics.unit * 24),
        ) {
            BasicText(
                text = current.entry.title,
                style = style.copy(color = Color.White.copy(alpha = 0.55f), fontSize = with(density) { metrics.itemSubtitleSize.toSp() }),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val taps = LocalXmbTaps.current
            current.options.forEachIndexed { index, option ->
                val focused = index == current.focus
                BasicText(
                    text = stringResource(option.label),
                    style = if (focused) style.copy(shadow = glow) else style.copy(color = Color.White.copy(alpha = 0.6f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().xmbTap { taps?.option(index) },
                )
            }
        }
    }
}
