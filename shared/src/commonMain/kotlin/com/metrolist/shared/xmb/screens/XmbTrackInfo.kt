package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_song
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.xmb.bar.XmbArtwork
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.theme.XmbMetrics
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The playing track, between its two places: the XMB's card at the bottom right ([expand] 0) and
 * the full-screen player's overlay ([expand] 1), laid out like the console's music player
 * (`reference/waves/video-track-overlay-1080p.jpg`, measured at 1.5 px per unit: artwork, title and
 * "artist / album" at the bottom left; the queue position, "00:01:53 / 00:02:14" with a codec
 * badge and a green progress bar at the bottom right). As a full screen opens, the artwork and
 * titles slide left and grow, the progress bar grows into the console's, and the times, queue
 * position and badge cross over, all on the one [expand] value, so nothing pops in. [alpha] fades
 * it all. Hidden when nothing is loaded.
 */
@Composable
internal fun XmbTrackInfo(
    playback: PlaybackController,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
    expand: () -> Float,
    alpha: () -> Float,
    modifier: Modifier = Modifier,
) {
    val state by playback.state.collectAsState()
    val song = state.currentSong ?: return
    val position by produceState(playback.currentPositionMs(), song.id, state.isPlaying) {
        while (true) {
            value = playback.currentPositionMs()
            if (!state.isPlaying) break
            delay(500)
        }
    }
    val duration = state.durationMs.takeIf { it > 0 } ?: song.duration?.times(1000) ?: 0L
    val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    val density = LocalDensity.current
    val unit = metrics.unit
    val shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, with(density) { unit.toPx() }), with(density) { (unit * 3).toPx() })

    fun style(
        size: Float,
        color: Color = Color.White,
    ) = TextStyle(color = color, fontFamily = fontFamily, fontSize = with(density) { (unit * size).toSp() }, shadow = shadow)
    val dim = Color.White.copy(alpha = 0.75f)
    val paused = !state.isPlaying

    // Scales from the top left by [scale] of the expansion and fades by [fade] of it, in the layer,
    // so the move redraws without recomposing.
    fun Modifier.scaled(
        fade: (Float) -> Float = { 1f },
        scale: (Float) -> Float,
    ): Modifier =
        graphicsLayer {
            val e = expand().coerceIn(0f, 1f)
            val factor = scale(e)
            scaleX = factor
            scaleY = factor
            transformOrigin = TransformOrigin(0f, 0f)
            this.alpha = fade(e)
        }
    Layout(
        content = {
            XmbArtwork(
                song.thumbnail,
                XmbIcon.Drawable(Res.drawable.xmb_item_song),
                unit * OVERLAY_ART,
                Modifier.scaled { lerp(CARD_ART / OVERLAY_ART, 1f, it) },
            )
            BasicText(
                song.customTitle ?: song.title,
                style = style(OVERLAY_TITLE),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.scaled { lerp(CARD_TITLE / OVERLAY_TITLE, 1f, it) },
            )
            BasicText(
                song.customArtist ?: song.artists.joinToString(", ") { it.name },
                style = style(CARD_DETAIL, dim),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.scaled({ 1f - crossfade(it) }) { lerp(1f, OVERLAY_ARTIST / CARD_DETAIL, it) },
            )
            BasicText(
                artistAlbumLine(song) ?: "- / -",
                style = style(OVERLAY_ARTIST),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.scaled(::crossfade) { lerp(CARD_DETAIL / OVERLAY_ARTIST, 1f, it) },
            )
            BasicText(
                if (duration > 0) "${formatXmbTime(position)} / ${formatXmbTime(duration)}" else formatXmbTime(position),
                style = style(CARD_DETAIL, dim),
                modifier = Modifier.scaled({ 1f - handOff(it, 0.1f, 0.4f) }) { lerp(1f, OVERLAY_TIME / CARD_DETAIL, it) },
            )
            BasicText(
                if (duration > 0) "${formatPlaybackClock(position)} / ${formatPlaybackClock(duration)}" else formatPlaybackClock(position),
                style = style(OVERLAY_TIME),
                modifier = Modifier.scaled({ handOff(it, 0.5f, 0.85f) }) { lerp(CARD_DETAIL / OVERLAY_TIME, 1f, it) },
            )
            BasicText(
                queueCounter(state.currentQueueIndex, state.queue.size) ?: "",
                style = style(OVERLAY_ARTIST),
                modifier = Modifier.scaled(::lateFade) { 1f },
            )
            Box(Modifier.scaled(::lateFade) { 1f }) {
                codecBadge(state.playbackResource?.mimeType, state.playbackResource?.codecs)?.let { codec ->
                    CodecBadge(codec, style(11f).copy(color = Color(0xFF2A2A2A), shadow = null), unit)
                }
            }
            Canvas(Modifier) { drawTrackBar(fraction, expand().coerceIn(0f, 1f)) }
        },
        modifier =
            modifier.graphicsLayer {
                this.alpha = alpha() * lerp(if (paused) PAUSED_ALPHA else 1f, 1f, expand().coerceIn(0f, 1f))
            },
    ) { measurables, constraints ->
        val e = expand().coerceIn(0f, 1f)
        val u = unit.toPx()
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val free = Constraints()

        fun wide(units: Float) = Constraints(maxWidth = (units * u).roundToInt())
        val art = measurables[0].measure(free)
        val title = measurables[1].measure(wide(lerp(CARD_COLUMN * OVERLAY_TITLE / CARD_TITLE, OVERLAY_COLUMN, e)))
        val cardArtist = measurables[2].measure(wide(CARD_COLUMN))
        val overlayArtist = measurables[3].measure(wide(lerp(CARD_COLUMN * OVERLAY_ARTIST / CARD_DETAIL, OVERLAY_COLUMN, e)))
        val cardTime = measurables[4].measure(free)
        val overlayTime = measurables[5].measure(free)
        val queue = measurables[6].measure(free)
        val badge = measurables[7].measure(free)
        val barWidth = lerp(CARD_COLUMN, OVERLAY_BAR_WIDTH, e) * u
        val barHeight = lerp(CARD_BAR, OVERLAY_BAR, e) * u
        val bar = measurables[8].measure(Constraints.fixed(barWidth.roundToInt(), barHeight.roundToInt().coerceAtLeast(1)))

        // The card: artwork, then a column of title, artists, bar and time, at the bottom right.
        val titleSmall = title.height * CARD_TITLE / OVERLAY_TITLE
        val cardColumnHeight = titleSmall + cardArtist.height + (CARD_GAP + CARD_BAR + CARD_TIME_GAP) * u + cardTime.height
        val cardRow = max(CARD_ART * u, cardColumnHeight)
        val cardTop = height - CARD_MARGIN * u - cardRow
        val cardX = width - (CARD_MARGIN + CARD_COLUMN) * u
        val cardY = cardTop + (cardRow - cardColumnHeight) / 2f
        val cardArt = Offset(cardX - (CARD_ART_GAP + CARD_ART) * u, cardTop + (cardRow - CARD_ART * u) / 2f)
        val cardTitle = Offset(cardX, cardY)
        val cardDetail = Offset(cardX, cardY + titleSmall)
        val cardBar = Offset(cardX, cardDetail.y + cardArtist.height + CARD_GAP * u)
        val cardClock = Offset(cardX, cardBar.y + (CARD_BAR + CARD_TIME_GAP) * u)

        // The overlay: artwork and titles at the bottom left, the rest at the bottom right.
        val overlayColumnHeight = title.height + overlayArtist.height.toFloat()
        val overlayRow = max(OVERLAY_ART * u, overlayColumnHeight)
        val overlayTop = height - OVERLAY_BOTTOM * u - overlayRow
        val overlayArt = Offset(OVERLAY_START * u, overlayTop + (overlayRow - OVERLAY_ART * u) / 2f)
        val overlayTitle = Offset((OVERLAY_START + OVERLAY_ART + 1f) * u, overlayTop + (overlayRow - overlayColumnHeight) / 2f)
        val overlayDetail = Offset(overlayTitle.x, overlayTitle.y + title.height)
        val right = width - OVERLAY_END * u
        val left = right - OVERLAY_BAR_WIDTH * u
        val overlayBar = Offset(left, height - (OVERLAY_BOTTOM + 4f + OVERLAY_BAR) * u)
        val timeRow = max(overlayTime.height, badge.height).toFloat()
        val timeTop = overlayBar.y - 18f * u - timeRow
        val overlayClock = Offset(left, timeTop + (timeRow - overlayTime.height) / 2f)

        layout(constraints.maxWidth, constraints.maxHeight) {
            fun move(
                placeable: Placeable,
                from: Offset,
                to: Offset,
            ) = placeable.place(lerp(from.x, to.x, e).roundToInt(), lerp(from.y, to.y, e).roundToInt())
            move(art, cardArt, overlayArt)
            move(title, cardTitle, overlayTitle)
            move(cardArtist, cardDetail, overlayDetail)
            move(overlayArtist, cardDetail, overlayDetail)
            move(cardTime, cardClock, overlayClock)
            move(overlayTime, cardClock, overlayClock)
            queue.place((right - queue.width).roundToInt(), (timeTop - 26f * u - queue.height).roundToInt())
            badge.place((right - badge.width).roundToInt(), (timeTop + (timeRow - badge.height) / 2f).roundToInt())
            move(bar, cardBar, overlayBar)
        }
    }
}

/**
 * The progress bar [expand] of the way from the card's (white on a faint track) to the console's
 * (green on a dark track with a light rim).
 */
private fun DrawScope.drawTrackBar(
    fraction: Float,
    expand: Float,
) {
    val radius = CornerRadius(size.height / 2f)
    val filled = size.copy(width = size.width * fraction)
    val card = 1f - expand
    if (card > 0f) {
        drawRoundRect(Color.White.copy(alpha = 0.25f), cornerRadius = radius, alpha = card)
        drawRoundRect(Color.White, size = filled, cornerRadius = radius, alpha = card)
    }
    if (expand > 0f) {
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF5C5C5C))), cornerRadius = radius, alpha = expand)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFC6F06E), Color(0xFF7FC41E))),
            size = filled,
            cornerRadius = radius,
            alpha = expand,
        )
        drawRoundRect(Color.White.copy(alpha = 0.35f), cornerRadius = radius, style = Stroke(width = size.height / 6f), alpha = expand)
    }
}

/**
 * The card's artist line gives way to the overlay's around the middle of the move; they start
 * with the same artists, so they cross over in place.
 */
internal fun crossfade(expand: Float): Float = handOff(expand, 0.25f, 0.75f)

/**
 * A smooth 0 to 1 between [from] and [to] of the move. The two times differ ("1:23" and
 * "00:01:23"), so one leaves before the other arrives rather than showing both.
 */
internal fun handOff(
    expand: Float,
    from: Float,
    to: Float,
): Float = ((expand - from) / (to - from)).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

/** What only the overlay has (queue position, codec badge) arrives in the second half. */
private fun lateFade(expand: Float): Float = ((expand - 0.5f) / 0.5f).coerceIn(0f, 1f)

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
) = start + (stop - start) * fraction

/** Where the card is paused, it dims to this. */
private const val PAUSED_ALPHA = 0.7f

// The card's measurements, in units.
private const val CARD_MARGIN = 32f
private const val CARD_ART = 56f
private const val CARD_ART_GAP = 14f
private const val CARD_COLUMN = 280f
private const val CARD_TITLE = 16f
private const val CARD_DETAIL = 13f
private const val CARD_GAP = 6f
private const val CARD_BAR = 3f
private const val CARD_TIME_GAP = 4f

// The overlay's, from the console capture.
private const val OVERLAY_START = 79f
private const val OVERLAY_END = 77f
private const val OVERLAY_BOTTOM = 58f
private const val OVERLAY_ART = 96f
private const val OVERLAY_COLUMN = 560f
private const val OVERLAY_TITLE = 24f
private const val OVERLAY_ARTIST = 19f
private const val OVERLAY_TIME = 20f
private const val OVERLAY_BAR_WIDTH = 257f
private const val OVERLAY_BAR = 6f
