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

/** The console's codec pill: silver, dark lettering, with a small speaker mark after it. */
@Composable
internal fun CodecBadge(
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
