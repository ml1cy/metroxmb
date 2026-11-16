package com.metrolist.shared.xmb.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.model.LyricsEntry
import com.metrolist.shared.xmb.bar.placeAt
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import kotlin.math.abs

/** Lines shown above and below the current one. */
private const val LYRIC_NEIGHBORS = 3

/**
 * Synced lyrics, XMB style: the current line ([current]) large and glowing at the middle of the
 * screen, the lines around it smaller and fading with distance, sliding up as the song moves on.
 * [message] replaces the lines while there are none (loading, none found, nothing playing).
 */
@Composable
internal fun XmbLyricsView(
    lines: List<LyricsEntry>,
    current: Int,
    message: String?,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val unit = with(density) { metrics.unit.toPx() }
    val big =
        TextStyle(
            color = Color.White,
            fontFamily = fontFamily,
            fontSize = with(density) { (metrics.unit * 34).toSp() },
            textAlign = TextAlign.Center,
            shadow = Shadow(Color.White.copy(alpha = 0.55f), Offset.Zero, unit * 10f),
        )
    val small =
        big.copy(
            fontSize = with(density) { (metrics.unit * 24).toSp() },
            shadow =
                Shadow(
                    Color.Black.copy(alpha = 0.4f),
                    Offset(0f, unit),
                    unit * 3f,
                ),
        )
    BoxWithConstraints(modifier.fillMaxSize()) {
        // A soft dark band behind the text keeps it readable over the ribbon.
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.15f to Color.Black.copy(alpha = 0.35f),
                    0.7f to Color.Black.copy(alpha = 0.35f),
                    0.85f to Color.Transparent,
                ),
            ),
        )
        if (lines.isEmpty() || message != null) {
            BasicText(message.orEmpty(), style = small, modifier = Modifier.align(Alignment.Center))
            return@BoxWithConstraints
        }
        val centerX = with(density) { maxWidth.toPx() } / 2f
        val centerY = with(density) { maxHeight.toPx() } * 0.4f
        val scroll = remember { Animatable(current.coerceAtLeast(0).toFloat()) }
        LaunchedEffect(current) {
            scroll.animateTo(current.coerceAtLeast(0).toFloat(), tween(XmbMotion.FADE_MS * 2, easing = XmbMotion.scrollEasing))
        }
        val pitch = unit * 64f
        val first = (current - LYRIC_NEIGHBORS).coerceAtLeast(0)
        val last = (current + LYRIC_NEIGHBORS).coerceAtMost(lines.lastIndex)
        for (index in first..last) {
            key(index) {
                val isCurrent = index == current
                BasicText(
                    text = lines[index].text.ifBlank { "♪" },
                    style = if (isCurrent) big else small,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier =
                        Modifier
                            .widthIn(max = metrics.unit * 900)
                            .placeAt(alignX = 0.5f, alignY = 0.5f) { Offset(centerX, centerY + (index - scroll.value) * pitch) }
                            .graphicsLayer {
                                val distance = abs(index - scroll.value)
                                alpha = (1f - distance / (LYRIC_NEIGHBORS + 1f)).coerceIn(0f, 1f) * (if (isCurrent) 1f else 0.7f)
                            },
                )
            }
        }
    }
}
