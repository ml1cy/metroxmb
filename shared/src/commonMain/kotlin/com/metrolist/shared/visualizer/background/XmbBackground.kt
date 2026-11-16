package com.metrolist.shared.visualizer.background

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import kotlin.math.sin

/**
 * The home-screen background: the month's color, dark at the top and bright at the bottom, with
 * the XMB ribbon and its particles (`XmbRibbon.kt`, ported from RetroArch's ribbon). [motion]
 * makes it react to navigation; [seconds] drives the error shiver. Both are read while drawing, so
 * the caller's frame clock (which stops while the window is hidden) redraws without recomposing.
 */
@Composable
fun XmbBackground(
    colors: XmbBackgroundColors,
    seconds: () -> Float,
    modifier: Modifier = Modifier,
    motion: XmbWaveMotion? = null,
) {
    Canvas(modifier) {
        drawXmbBackground(colors, seconds(), motion ?: restingMotion)
    }
}

private val restingMotion = XmbWaveMotion()

internal fun DrawScope.drawXmbBackground(
    colors: XmbBackgroundColors,
    seconds: Float,
    motion: XmbWaveMotion,
) {
    drawRect(Brush.verticalGradient(listOf(colors.top, colors.bottom)))
    val frame =
        RibbonFrame(
            // Navigation pushes the flow (XmbWaveMotion); the error sound makes it shiver.
            time = RIBBON_START + (motion.flow + motion.shiver * 0.15f * sin(seconds * 46f)) * RIBBON_SPEED,
            amplitude = 1f + 0.9f * motion.swell,
            opacity = RIBBON_OPACITY * (1f + 0.7f * motion.flash),
            particleOpacity = RIBBON_PARTICLE_OPACITY * (1f + 1.5f * motion.flash),
            lift = -motion.lift * 0.12f,
            drift = motion.drift,
        )
    drawRibbon(frame)
    drawRibbonParticles(frame)
}
