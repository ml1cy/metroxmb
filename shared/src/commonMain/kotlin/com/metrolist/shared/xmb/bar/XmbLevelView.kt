package com.metrolist.shared.xmb.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.loading
import com.metrolist.shared.generated.resources.nothing_to_show
import com.metrolist.shared.generated.resources.xmb_list_error
import com.metrolist.shared.xmb.adapters.XmbListState
import com.metrolist.shared.xmb.adapters.icon
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.nav.XmbLevel
import com.metrolist.shared.xmb.theme.XmbMetrics
import com.metrolist.shared.xmb.theme.XmbMotion
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

private const val ROWS_ABOVE = 7
private const val ROWS_BELOW = 10

/**
 * An opened level: the parent row's icon to the left, and the list's rows centered on the focused
 * one. Rows slide in from the right when the level opens. [visible] (0..1) fades the whole level
 * while it opens or closes.
 */
@Composable
internal fun XmbLevelView(
    level: XmbLevel,
    state: XmbListState,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    fontFamily: FontFamily,
    glow: () -> Float,
    visible: () -> Float,
) {
    val focus = remember(level.list.key) { Animatable(level.focus.toFloat()) }
    LaunchedEffect(level.list.key, level.focus) {
        focus.animateTo(level.focus.toFloat(), tween(XmbMotion.ITEM_MS, easing = XmbMotion.scrollEasing))
    }
    val enter = remember(level.list.key) { Animatable(0f) }
    LaunchedEffect(level.list.key) { enter.animateTo(1f, tween(XmbMotion.FADE_MS, easing = XmbMotion.scrollEasing)) }
    val density = LocalDensity.current
    val slide = with(density) { metrics.unit.toPx() * 40f }
    val titleStyle = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { metrics.itemTitleSize.toSp() })
    val glowRadius = with(density) { metrics.unit.toPx() * 8f }

    Box(
        Modifier
            .placeAt(alignX = 0.5f, alignY = 0.5f) { Offset(geometry.anchorX - geometry.parentOffset, geometry.levelFocusY) }
            .size(metrics.itemIcon)
            .graphicsLayer { alpha = visible() * 0.8f },
    ) {
        XmbArtwork(level.artwork, level.icon, metrics.itemIcon)
    }

    val rowX = { geometry.anchorX - geometry.itemIcon / 2f + (1f - enter.value) * slide }
    if (state.entries.isEmpty()) {
        val message =
            when {
                state.isLoading -> Res.string.loading
                state.isError -> Res.string.xmb_list_error
                else -> Res.string.nothing_to_show
            }
        BasicText(
            text = stringResource(message),
            style = titleStyle.copy(color = Color.White.copy(alpha = 0.8f)),
            modifier =
                Modifier
                    .placeAt(alignY = 0.5f) { Offset(rowX(), geometry.levelFocusY) }
                    .graphicsLayer { alpha = visible() * enter.value },
        )
        return
    }

    state.entries.forEachIndexed { index, entry ->
        if (index < level.focus - ROWS_ABOVE || index > level.focus + ROWS_BELOW) return@forEachIndexed
        key(entry.key, index) {
            val focused = index == level.focus
            val relative = { index - focus.value }
            val emphasis = { (1f - abs(relative())).coerceIn(0f, 1f) }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .placeAt(alignY = 0.5f) { Offset(rowX(), geometry.levelFocusY + relative() * geometry.itemSpacing) }
                        .graphicsLayer {
                            alpha = visible() * enter.value * lerp(XmbMotion.UNFOCUSED_ITEM_ALPHA, 1f, emphasis())
                        },
            ) {
                Box(
                    Modifier
                        .size(metrics.itemIcon)
                        .graphicsLayer {
                            val scale = lerp(XmbMotion.UNFOCUSED_ITEM_SCALE, 1f, emphasis())
                            scaleX = scale
                            scaleY = scale
                        }.focusGlow { emphasis() * glow() },
                ) {
                    XmbArtwork(entry.artwork, entry.icon, metrics.itemIcon)
                }
                Spacer(Modifier.width(metrics.itemTextOffset))
                Column {
                    BasicText(
                        text = entry.title,
                        style =
                            if (focused) {
                                titleStyle.copy(
                                    shadow = Shadow(Color.White.copy(alpha = 0.6f), Offset.Zero, glowRadius),
                                )
                            } else {
                                titleStyle
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = entry.subtitle
                    if (focused && subtitle != null) {
                        BasicText(
                            text = subtitle,
                            style =
                                TextStyle(
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontFamily = fontFamily,
                                    fontSize = with(density) { metrics.itemSubtitleSize.toSp() },
                                ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/** Album art or a photo when there is one, the XMB icon until it loads or if it fails. */
@Composable
internal fun XmbArtwork(
    url: String?,
    fallback: XmbIcon,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size)) {
        if (url == null) {
            XmbIconImage(fallback, Modifier.fillMaxSize())
            return@Box
        }
        val painter = rememberAsyncImagePainter(url)
        val state by painter.state.collectAsState()
        if (state !is AsyncImagePainter.State.Success) XmbIconImage(fallback, Modifier.fillMaxSize())
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(size * 0.06f)),
        )
    }
}
