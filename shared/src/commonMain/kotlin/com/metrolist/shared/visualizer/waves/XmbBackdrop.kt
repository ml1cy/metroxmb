package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.background.RibbonFrame
import com.metrolist.shared.visualizer.background.XmbWaveMotion
import com.metrolist.shared.visualizer.background.drawRibbon
import com.metrolist.shared.visualizer.background.drawRibbonParticles
import com.metrolist.shared.visualizer.background.homeRibbonFrame
import com.metrolist.shared.xmb.theme.XmbBackgroundColors

/**
 * The XMB's background and the Waves visualizer as one picture, [waves] (0..1) of the way from
 * the home screen to Waves. The sky's colors and every ribbon setting blend, and the ribbon's
 * clocks hand over without a jump, so opening Waves turns the home wave into it instead of
 * fading one picture over the other.
 */
@Composable
fun XmbBackdrop(
    colors: XmbBackgroundColors,
    motion: XmbWaveMotion,
    audio: VisualizerAudio,
    waves: () -> Float,
    seconds: () -> Float,
    modifier: Modifier = Modifier,
) {
    val ribbonClock = remember { RibbonClockBlend() }
    val particleClock = remember { RibbonClockBlend() }
    Canvas(modifier) {
        val t = waves().coerceIn(0f, 1f)
        val home = homeRibbonFrame(motion, seconds())
        val music = wavesRibbonFrame(audio)
        drawRect(Brush.verticalGradient(*blendedSky(colors, t)))
        val frame =
            RibbonFrame(
                time = ribbonClock.blend(home.time, music.time, t),
                amplitude = lerp(home.amplitude, music.amplitude, t),
                opacity = lerp(home.opacity, music.opacity, t),
                particleOpacity = lerp(home.particleOpacity, music.particleOpacity, t),
                lift = lerp(home.lift, music.lift, t),
                // The particles keep their place across the screen.
                drift = home.drift,
                folds = lerp(home.folds, music.folds, t),
                tint = lerp(home.tint, music.tint, t),
                particleTime = particleClock.blend(home.particleTime, music.particleTime, t),
                streak = lerp(home.streak, music.streak, t),
            )
        drawRibbon(frame)
        drawRibbonParticles(frame)
    }
}

/** The sky [t] of the way from the home gradient to the Waves violet, at the Waves stops. */
internal fun blendedSky(
    colors: XmbBackgroundColors,
    t: Float,
): Array<Pair<Float, Color>> =
    if (t >= 1f) {
        WAVES_SKY
    } else {
        Array(WAVES_SKY.size) { index ->
            val (stop, sky) = WAVES_SKY[index]
            val home =
                when (stop) {
                    0f -> colors.top
                    1f -> colors.bottom
                    else -> lerp(colors.top, colors.bottom, stop)
                }
            stop to if (t <= 0f) home else lerp(home, sky, t)
        }
    }

/**
 * Hands a ribbon clock over between the home screen's and the visualizer's without a jump. The
 * clocks run at their own pace from their own start, so each gets an offset, re-anchored while
 * the other one shows alone: a fade starts from the time on screen, and the two only drift apart
 * by their difference in speed while it runs.
 */
internal class RibbonClockBlend {
    private var homeOffset = 0f
    private var wavesOffset = 0f

    fun blend(
        home: Float,
        waves: Float,
        t: Float,
    ): Float {
        if (t <= 0f) {
            wavesOffset = home + homeOffset - waves
        } else if (t >= 1f) {
            homeOffset = waves + wavesOffset - home
        }
        return lerp(home + homeOffset, waves + wavesOffset, t)
    }
}

private fun lerp(
    start: Float,
    stop: Float,
    fraction: Float,
) = start + (stop - start) * fraction
