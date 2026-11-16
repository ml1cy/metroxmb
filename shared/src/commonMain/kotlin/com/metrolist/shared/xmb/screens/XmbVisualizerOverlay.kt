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
import androidx.compose.foundation.layout.size
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
 * The track info over a full-screen visualizer, laid out like the console's music player (compare
 * the photos in reference/waves): artwork, title and "artist / album" at the bottom left; the
 * queue position, "00:01:50 / 00:03:51", a green-on-blue progress bar and a codec badge at the
 * bottom right. Hidden when nothing is loaded.
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
    val shadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, with(density) { unit.toPx() }), with(density) { (unit * 3).toPx() })
    val title = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { (unit * 20).toSp() }, shadow = shadow)
    val detail = title.copy(fontSize = with(density) { (unit * 15).toSp() })
    Box(modifier.fillMaxSize().padding(horizontal = unit * 64, vertical = unit * 56)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.BottomStart)) {
            // XmbArtwork insets art to 70% of its box; this box makes the art 48 units.
            XmbArtwork(song.thumbnail, XmbIcon.Drawable(Res.drawable.xmb_item_song), unit * 68)
            Spacer(Modifier.width(unit * 2))
            Column(Modifier.widthIn(max = unit * 520)) {
                BasicText(song.customTitle ?: song.title, style = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                artistAlbumLine(song)?.let { BasicText(it, style = detail, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(unit * 6),
            modifier = Modifier.align(Alignment.BottomEnd),
        ) {
            queueCounter(state.currentQueueIndex, state.queue.size)?.let { BasicText(it, style = title) }
            BasicText(
                if (duration > 0) "${formatPlaybackClock(position)} / ${formatPlaybackClock(duration)}" else formatPlaybackClock(position),
                style = title,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(unit * 10)) {
                val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                Canvas(Modifier.width(unit * 260).height(unit * 8)) {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9CC3F0), Color(0xFF5C8FD6))), cornerRadius = radius)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0xFFB4F06A), Color(0xFF5CB82E))),
                        size =
                            size.copy(
                                width =
                                    size.width * fraction,
                            ),
                        cornerRadius = radius,
                    )
                }
                codecBadge(state.playbackResource?.mimeType, state.playbackResource?.codecs)?.let { codec ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier =
                            Modifier
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF8FB0EC), Color(0xFF3C63B8))),
                                    RoundedCornerShape(unit * 4),
                                ).border(unit, Color.White.copy(alpha = 0.7f), RoundedCornerShape(unit * 4))
                                .padding(horizontal = unit * 8, vertical = unit * 1),
                    ) {
                        BasicText(codec, style = detail.copy(fontSize = with(density) { (unit * 13).toSp() }))
                    }
                }
            }
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
