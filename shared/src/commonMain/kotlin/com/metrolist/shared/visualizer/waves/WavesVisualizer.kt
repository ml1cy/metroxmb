package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.RIBBON_FOLDS
import com.metrolist.shared.visualizer.background.RIBBON_OPACITY
import com.metrolist.shared.visualizer.background.RIBBON_PARTICLE_OPACITY
import com.metrolist.shared.visualizer.background.RIBBON_START
import com.metrolist.shared.visualizer.background.RibbonFrame
import com.metrolist.shared.visualizer.background.drawRibbon
import com.metrolist.shared.visualizer.background.drawRibbonParticles

/**
 * The Waves visualizer (M4): the XMB ribbon low on a violet sky that fades to near black, as in
 * every console video and photo the owner sent (reference/waves); unlike the home screen it
 * doesn't follow the month's color. Driven by the music: bass deepens the folds, mids add more of
 * them (loud passages draw a bundle of thin silk lines), energy and beats brighten the sheet, and
 * the particles rush and streak ([VisualizerAudio.particleFlow]).
 */
@Composable
fun WavesVisualizer(
    audio: VisualizerAudio,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(*SKY))
        val rush = 1f + VisualizerAudio.PARTICLE_RUSH * audio.energy + VisualizerAudio.PARTICLE_BEAT_RUSH * audio.beat
        val frame =
            RibbonFrame(
                time = RIBBON_START + audio.flow * WAVES_SPEED,
                amplitude = 1f + 0.8f * audio.bass,
                opacity = RIBBON_OPACITY * (1f + 0.5f * audio.energy + 0.6f * audio.beat),
                particleOpacity = RIBBON_PARTICLE_OPACITY * (0.8f + 0.8f * audio.treble + 0.8f * audio.beat),
                folds = RIBBON_FOLDS + 5f * audio.mid,
                tint = TINT,
                particleTime = RIBBON_START + audio.particleFlow * WAVES_SPEED,
                // A trail as long as the particle moves in about a twentieth of a second.
                streak = WAVES_SPEED * rush / 20f,
            )
        drawRibbon(frame)
        drawRibbonParticles(frame)
    }
}

/** The ribbon's clock against [VisualizerAudio.flow]: a little livelier than the home screen's. */
private const val WAVES_SPEED = 0.35f

/** Sampled from the console video: violet at the top through deep violet to near black. */
private val SKY =
    arrayOf(
        0f to Color(110, 40, 240),
        0.35f to Color(70, 14, 175),
        0.6f to Color(35, 8, 125),
        0.85f to Color(10, 4, 45),
        1f to Color(5, 2, 20),
    )

/** The strands' lavender-pink highlights in the video. */
private val TINT = Color(1f, 0.82f, 1f)
