package com.metrolist.shared.visualizer.background

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/**
 * The M1 background: the monthly gradient with a soft highlight and a simple animated wave. M4
 * replaces the wave with the full Waves shader. [seconds] is read while drawing, so the caller's
 * frame clock (which stops while the window is hidden) redraws it without recomposing.
 */
@Composable
fun XmbBackground(
    colors: XmbBackgroundColors,
    seconds: () -> Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        drawXmbBackground(colors, seconds())
    }
}

/** Wave ribbons: relative height, vertical position, speed, wavelength and phase. */
private class Ribbon(
    val y: Float,
    val amplitude: Float,
    val thickness: Float,
    val speed: Float,
    val wavelength: Float,
    val phase: Float,
    val alpha: Float,
)

private val ribbons =
    listOf(
        Ribbon(y = 0.62f, amplitude = 0.055f, thickness = 0.075f, speed = 0.11f, wavelength = 1.35f, phase = 0f, alpha = 0.16f),
        Ribbon(y = 0.6f, amplitude = 0.04f, thickness = 0.045f, speed = -0.07f, wavelength = 0.95f, phase = 1.7f, alpha = 0.12f),
        Ribbon(y = 0.66f, amplitude = 0.03f, thickness = 0.03f, speed = 0.05f, wavelength = 0.7f, phase = 3.1f, alpha = 0.1f),
    )

private const val WAVE_SEGMENTS = 64

internal fun DrawScope.drawXmbBackground(
    colors: XmbBackgroundColors,
    seconds: Float,
) {
    drawRect(Brush.verticalGradient(listOf(colors.top, colors.bottom)))
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.highlight, Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.08f),
            radius = max(size.width, size.height) * 0.75f,
        ),
    )
    ribbons.forEach { drawRibbon(it, seconds) }
}

private fun DrawScope.drawRibbon(
    ribbon: Ribbon,
    seconds: Float,
) {
    val width = size.width
    val height = size.height
    val center = height * ribbon.y
    val maxThickness = height * ribbon.thickness
    val upper = Path()
    val lower = ArrayList<Offset>(WAVE_SEGMENTS + 1)
    for (step in 0..WAVE_SEGMENTS) {
        val x = width * step / WAVE_SEGMENTS
        val u = x / width
        val angle = 2f * PI.toFloat() * (u / ribbon.wavelength + seconds * ribbon.speed) + ribbon.phase
        val y = center + height * ribbon.amplitude * (sin(angle) + 0.35f * sin(angle * 2.3f + 0.8f))
        val thickness = maxThickness * (0.55f + 0.45f * sin(angle * 0.6f + 1.3f))
        if (step == 0) upper.moveTo(x, y - thickness) else upper.lineTo(x, y - thickness)
        lower += Offset(x, y + thickness)
    }
    val crest = Path().apply { addPath(upper) }
    for (point in lower.asReversed()) upper.lineTo(point.x, point.y)
    upper.close()
    drawPath(
        upper,
        Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = ribbon.alpha), Color.Transparent),
            startY = center - maxThickness - height * ribbon.amplitude * 1.35f,
            endY = center + maxThickness + height * ribbon.amplitude * 1.35f,
        ),
    )
    drawPath(crest, Color.White.copy(alpha = ribbon.alpha * 0.9f), style = Stroke(width = height / 720f * 1.2f))
}
