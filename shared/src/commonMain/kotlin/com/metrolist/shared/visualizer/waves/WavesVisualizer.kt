package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.RIBBON_OPACITY
import com.metrolist.shared.visualizer.background.RIBBON_PARTICLE_OPACITY
import com.metrolist.shared.visualizer.background.RIBBON_START
import com.metrolist.shared.visualizer.background.RibbonFrame
import com.metrolist.shared.visualizer.background.drawRibbon
import com.metrolist.shared.visualizer.background.drawRibbonParticles
import com.metrolist.shared.xmb.theme.XmbBackgroundColors

/**
 * The Waves visualizer (M4): the XMB ribbon full screen over the month's color fading to deep navy,
 * as in the photos in reference/waves, driven by the music. Bass deepens the folds, overall
 * energy and beats brighten the sheet, treble and beats light up the particles, and the ribbon
 * flows faster when the music is louder ([VisualizerAudio.flow]).
 */
@Composable
fun WavesVisualizer(
    colors: XmbBackgroundColors,
    audio: VisualizerAudio,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(0f to colors.base, 0.75f to NAVY, 1f to DEEP_NAVY))
        val frame =
            RibbonFrame(
                time = RIBBON_START + audio.flow * WAVES_SPEED,
                amplitude = 1f + 1f * audio.bass,
                opacity = RIBBON_OPACITY * (1.1f + 0.5f * audio.energy + 0.7f * audio.beat),
                particleOpacity = RIBBON_PARTICLE_OPACITY * (0.7f + 0.8f * audio.treble + 0.8f * audio.beat),
                // The photos show the band higher than the home screen's, across the upper middle.
                lift = WAVES_LIFT,
            )
        drawRibbon(frame)
        drawRibbonParticles(frame)
    }
}

/** Raises the band by this much of half the screen height. */
private const val WAVES_LIFT = 0.3f

/** The ribbon's clock against [VisualizerAudio.flow]: a little livelier than the home screen's. */
private const val WAVES_SPEED = 0.35f

/** The photos' bottom colors: navy (about 40, 50, 119) shading to (14, 19, 59). */
private val NAVY = Color(0.06f, 0.08f, 0.24f)
private val DEEP_NAVY = Color(0.04f, 0.05f, 0.15f)
