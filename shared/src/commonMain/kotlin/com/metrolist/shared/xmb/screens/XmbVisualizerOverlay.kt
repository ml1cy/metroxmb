package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_song
import com.metrolist.shared.model.SongItem
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.xmb.bar.XmbArtwork
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.theme.XmbMetrics
import kotlinx.coroutines.delay

/**
 * The track info over a full-screen visualizer, laid out like the console's music player
 * (`reference/waves/video-track-overlay-1080p.jpg`, measured at 1.5 px per unit): artwork, title
 * and "artist / album" at the bottom left; at the bottom right the queue position, then
 * "00:01:53 / 00:02:14" beside a silver codec badge, then a green progress bar on a dark track.
 * Hidden when nothing is loaded.
 */
@Composable
internal fun XmbVisualizerOverlay(
    playback: PlaybackController,
    metrics: XmbMetrics,
    fontFamily: FontFamily,
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
    val density = LocalDensity.current
    val unit = metrics.unit
    val shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, with(density) { unit.toPx() }), with(density) { (unit * 3).toPx() })

    fun style(size: Float) =
        TextStyle(
            color = Color.White,
            fontFamily = fontFamily,
            fontSize =
                with(density) {
                    (unit * size).toSp()
                },
            shadow = shadow,
        )
    Box(modifier.fillMaxSize().padding(start = unit * 79, end = unit * 77, bottom = unit * 58)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.BottomStart)) {
            // XmbArtwork insets art to 70% of its box; this box makes the art 67 units (100 px at 1080p).
            XmbArtwork(song.thumbnail, XmbIcon.Drawable(Res.drawable.xmb_item_song), unit * 96)
            Spacer(Modifier.width(unit * 1))
            Column(Modifier.widthIn(max = unit * 560)) {
                BasicText(song.customTitle ?: song.title, style = style(24f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                BasicText(artistAlbumLine(song) ?: "- / -", style = style(19f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.align(Alignment.BottomEnd).width(unit * 257).padding(bottom = unit * 4),
        ) {
            BasicText(queueCounter(state.currentQueueIndex, state.queue.size) ?: "", style = style(19f))
            Spacer(Modifier.height(unit * 26))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    if (duration >
                        0
                    ) {
                        "${formatPlaybackClock(position)} / ${formatPlaybackClock(duration)}"
                    } else {
                        formatPlaybackClock(position)
                    },
                    style = style(20f),
                    modifier = Modifier.weight(1f),
                )
                codecBadge(state.playbackResource?.mimeType, state.playbackResource?.codecs)?.let { codec ->
                    CodecBadge(codec, style(11f).copy(color = Color(0xFF2A2A2A), shadow = null), unit)
                }
            }
            Spacer(Modifier.height(unit * 18))
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Canvas(Modifier.width(unit * 257).height(unit * 6)) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF3A3A3A), Color(0xFF5C5C5C))), cornerRadius = radius)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFFC6F06E), Color(0xFF7FC41E))),
                    size =
                        size.copy(
                            width =
                                size.width * fraction,
                        ),
                    cornerRadius = radius,
                )
                drawRoundRect(Color.White.copy(alpha = 0.35f), cornerRadius = radius, style = Stroke(width = size.height / 6f))
            }
        }
    }
}

/** The console's codec pill: silver, dark lettering, with a small speaker mark after it. */
@Composable
private fun CodecBadge(
    codec: String,
    text: TextStyle,
    unit: androidx.compose.ui.unit.Dp,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(unit * 2)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .width(unit * 56)
                    .height(unit * 12)
                    .background(Brush.verticalGradient(listOf(Color(0xFFE6E6E6), Color(0xFF8C8C8C))), RoundedCornerShape(unit * 3))
                    .border(unit * 0.7f, Color.White.copy(alpha = 0.6f), RoundedCornerShape(unit * 3)),
        ) {
            BasicText(codec, style = text)
        }
        Canvas(Modifier.width(unit * 5).height(unit * 10)) {
            drawArc(Color(0xFFBDBDBD), startAngle = -60f, sweepAngle = 120f, useCenter = false, style = Stroke(width = size.width / 3f))
        }
    }
}

/** hh:mm:ss, as the console's music player shows it. */
internal fun formatPlaybackClock(milliseconds: Long): String {
    val total = (milliseconds / 1000).coerceAtLeast(0)
    return listOf(total / 3600, (total % 3600) / 60, total % 60).joinToString(":") { it.toString().padStart(2, '0') }
}

/** "3/10": the track's place in the queue, when there is a queue. */
internal fun queueCounter(
    index: Int,
    size: Int,
): String? = if (size > 0 && index in 0 until size) "${index + 1}/$size" else null

/** The stream's codec for the badge (the console shows MP3, AAC...), or null when unknown. */
internal fun codecBadge(
    mimeType: String?,
    codecs: String?,
): String? {
    val codec = (codecs ?: mimeType?.substringAfter("codecs=", "")).orEmpty().lowercase()
    val mime = mimeType.orEmpty().lowercase()
    return when {
        "opus" in codec -> "OPUS"
        "mp4a" in codec || "aac" in codec -> "AAC"
        "vorbis" in codec -> "OGG"
        "flac" in codec || "flac" in mime -> "FLAC"
        "mpeg" in mime || "mp3" in codec -> "MP3"
        "mp4" in mime || "m4a" in mime -> "AAC"
        else -> null
    }
}

/** "LINKIN PARK / One More Light": artists, then the album when there is one. */
internal fun artistAlbumLine(song: SongItem): String? {
    val artists =
        song.customArtist ?: song.artists
            .map { it.name }
            .filter(String::isNotBlank)
            .joinToString(", ")
    val album = song.album?.name?.takeIf(String::isNotBlank)
    return listOfNotNull(artists.ifBlank { null }, album).joinToString(" / ").ifBlank { null }
}
