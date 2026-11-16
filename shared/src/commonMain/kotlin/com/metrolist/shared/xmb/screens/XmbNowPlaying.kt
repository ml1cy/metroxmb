package com.metrolist.shared.xmb.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.xmb_item_song
import com.metrolist.shared.ui.playback.PlaybackController
import com.metrolist.shared.xmb.bar.XmbArtwork
import com.metrolist.shared.xmb.nav.XmbIcon
import com.metrolist.shared.xmb.theme.XmbMetrics
import kotlinx.coroutines.delay

/**
 * Bottom-right track info, like the console's music player overlay: artwork, title, artist and a
 * thin progress bar with times. Hidden when nothing is loaded; dimmed while paused. The position
 * is polled twice a second only while playing.
 */
@Composable
internal fun XmbNowPlaying(
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
    val text = TextStyle(color = Color.White, fontFamily = fontFamily, fontSize = with(density) { (metrics.unit * 16).toSp() })
    val small = text.copy(color = Color.White.copy(alpha = 0.75f), fontSize = with(density) { (metrics.unit * 13).toSp() })
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.graphicsLayer { alpha = if (state.isPlaying) 1f else 0.7f },
    ) {
        XmbArtwork(song.thumbnail, XmbIcon.Drawable(Res.drawable.xmb_item_song), metrics.unit * 56)
        Spacer(Modifier.width(metrics.unit * 14))
        Column(Modifier.width(metrics.unit * 280)) {
            BasicText(song.customTitle ?: song.title, style = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            BasicText(
                song.customArtist ?: song.artists.joinToString(", ") { it.name },
                style = small,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(metrics.unit * 6))
            val fraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
            Canvas(Modifier.fillMaxWidth().height(metrics.unit * 3)) {
                val radius = CornerRadius(size.height / 2f)
                drawRoundRect(Color.White.copy(alpha = 0.25f), cornerRadius = radius)
                drawRoundRect(Color.White, size = size.copy(width = size.width * fraction), cornerRadius = radius)
            }
            Spacer(Modifier.height(metrics.unit * 4))
            BasicText(
                if (duration > 0) "${formatXmbTime(position)} / ${formatXmbTime(duration)}" else formatXmbTime(position),
                style = small,
            )
        }
    }
}

/** m:ss, or h:mm:ss from an hour up. */
fun formatXmbTime(milliseconds: Long): String {
    val total = (milliseconds / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = (total % 60).toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:$seconds" else "$minutes:$seconds"
}
