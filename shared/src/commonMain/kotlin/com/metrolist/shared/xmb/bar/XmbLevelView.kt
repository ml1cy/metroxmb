package com.metrolist.shared.xmb.bar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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

private const val ROWS_ABOVE = 6
private const val ROWS_BELOW = 10

/** How much a value picker dims the rows it isn't for. */
private const val PICKER_DIM = 0.6f

/** Unavailable rows (disabled settings) dim to this. */
private const val DISABLED_ALPHA = 0.4f

/**
 * An opened level: the parent row's icon to the left, and the list's rows laid out like a
 * category's column around the same focus point. Rows slide in from the right when the level
 * opens. [visible] (0..1) fades the whole level while it opens or closes; [dim] (0..1) fades the
 * rows that aren't focused while a value picker is open.
 */
@Composable
internal fun XmbLevelView(
    level: XmbLevel,
    state: XmbListState,
    metrics: XmbMetrics,
    geometry: XmbBarGeometry,
    styles: XmbRowStyles,
    visible: () -> Float,
    dim: () -> Float = { 0f },
) {
    val focus = remember(level.list.key) { Animatable(level.focus.toFloat()) }
    LaunchedEffect(level.list.key, level.focus) {
        focus.animateTo(level.focus.toFloat(), tween(XmbMotion.ITEM_MS, easing = XmbMotion.scrollEasing))
    }
    val enter = remember(level.list.key) { Animatable(0f) }
    LaunchedEffect(level.list.key) { enter.animateTo(1f, tween(XmbMotion.FADE_MS, easing = XmbMotion.scrollEasing)) }
    val slide = with(LocalDensity.current) { metrics.unit.toPx() * 40f }

    Box(
        Modifier
            .placeAt(alignX = 0.5f, alignY = 0.5f) { Offset(geometry.anchorX - geometry.parentOffset, geometry.focusY) }
            .size(metrics.itemIcon)
            .graphicsLayer { alpha = visible() * 0.8f * (1f - 0.6f * dim()) },
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
            style = styles.title.copy(color = Color.White.copy(alpha = 0.8f)),
            modifier =
                Modifier
                    .placeAt(alignY = 0.5f) { Offset(rowX() + geometry.itemIcon, geometry.focusY) }
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
            XmbRow(
                title = entry.title,
                subtitle = entry.subtitle.takeIf { focused },
                value = entry.value,
                focused = focused,
                styles = styles,
                metrics = metrics,
                modifier =
                    Modifier
                        .placeAt(alignY = 0.5f) { Offset(rowX(), geometry.itemY(relative())) }
                        .graphicsLayer {
                            val rest = lerp(XmbMotion.UNFOCUSED_ITEM_ALPHA, 1f, emphasis()) * (1f - PICKER_DIM * (1f - emphasis()) * dim())
                            alpha = visible() * enter.value * rest * (if (entry.enabled) 1f else DISABLED_ALPHA)
                        },
                iconScale = { lerp(XmbMotion.UNFOCUSED_ITEM_SCALE, 1f, emphasis()) },
            ) {
                XmbArtwork(entry.artwork, entry.icon, metrics.itemIcon)
            }
        }
    }
}

/**
 * Album art or a photo when there is one, the XMB icon until it loads or if it fails. Artwork is
 * drawn smaller than the icon box ([XmbMotion.ARTWORK_FILL]) because the PS3 icons have padding.
 */
@Composable
internal fun XmbArtwork(
    url: String?,
    fallback: XmbIcon,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (url == null) {
            XmbIconImage(fallback, Modifier.fillMaxSize())
            return@Box
        }
        val painter = rememberAsyncImagePainter(url)
        val state by painter.state.collectAsState()
        if (state !is AsyncImagePainter.State.Success) XmbIconImage(fallback, Modifier.fillMaxSize())
        val art = size * XmbMotion.ARTWORK_FILL
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(art).clip(RoundedCornerShape(art * 0.06f)),
        )
    }
}
