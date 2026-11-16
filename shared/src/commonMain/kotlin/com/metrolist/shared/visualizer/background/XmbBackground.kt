package com.metrolist.shared.visualizer.background

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * The home-screen background: the monthly gradient, a silk wave of crossing strands with a soft
 * glow band, and sparkles drifting around it (compare the frames in reference/xmb). [motion] makes it react
 * to navigation; [seconds] drives the resting flow and twinkle. Both are read while drawing, so the
 * caller's frame clock (which stops while the window is hidden) redraws without recomposing.
 * M4's Waves visualizer replaces the wave with the full shader; the sparkles carry over.
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

/** One silk strand: how fast and how often it crosses the wave's center line. */
private class Strand(
    val frequency: Float,
    val speed: Float,
    val phase: Float,
    val spread: Float,
    val alpha: Float,
)

private val strands =
    listOf(
        Strand(frequency = 0.9f, speed = 0.05f, phase = 0.0f, spread = 1.0f, alpha = 1.0f),
        Strand(frequency = 1.15f, speed = -0.035f, phase = 2.4f, spread = 0.85f, alpha = 0.8f),
        Strand(frequency = 0.7f, speed = 0.03f, phase = 4.1f, spread = 0.7f, alpha = 0.9f),
        Strand(frequency = 1.45f, speed = 0.06f, phase = 1.2f, spread = 0.55f, alpha = 0.65f),
        Strand(frequency = 0.55f, speed = -0.025f, phase = 5.3f, spread = 0.95f, alpha = 0.7f),
        Strand(frequency = 1.8f, speed = -0.05f, phase = 3.3f, spread = 0.4f, alpha = 0.55f),
        Strand(frequency = 1.0f, speed = 0.045f, phase = 0.7f, spread = 0.3f, alpha = 0.85f),
    )

/** Pairs of strands whose gap is filled, which reads as translucent twisting ribbons. */
private val sheets = listOf(0 to 1, 2 to 4, 3 to 6)

private class Sparkle(
    val u: Float,
    val offset: Float,
    val size: Float,
    val depth: Float,
    val alpha: Float,
    val twinkleRate: Float,
    val twinklePhase: Float,
    val bobRate: Float,
    val far: Boolean,
)

private val sparkles: List<Sparkle> =
    Random(0x5EED).let { random ->
        List(SPARKLE_COUNT) { index ->
            val far = index % 5 == 0
            val big = random.nextFloat() < 0.08f
            val bell = (random.nextFloat() + random.nextFloat() + random.nextFloat() - 1.5f) / 1.5f
            Sparkle(
                u = random.nextFloat(),
                offset = if (far) random.nextFloat() * 2f - 1f else bell,
                size = if (big) 1.8f + random.nextFloat() * 0.8f else 0.5f + random.nextFloat() * 1.0f,
                depth = random.nextFloat(),
                alpha = if (far) 0.25f + random.nextFloat() * 0.3f else 0.45f + random.nextFloat() * 0.55f,
                twinkleRate = 0.6f + random.nextFloat() * 2.4f,
                twinklePhase = random.nextFloat() * TAU,
                bobRate = 0.2f + random.nextFloat() * 0.5f,
                far = far,
            )
        }
    }

private const val SPARKLE_COUNT = 260
private const val SEGMENTS = 72
private const val TAU = (2 * PI).toFloat()

/** Where the focused category sits; ✕ pulses start there. Matches XmbMetrics.ANCHOR_X. */
private const val PULSE_ORIGIN = 0.295f
private const val PULSE_SPEED = 0.9f

internal fun DrawScope.drawXmbBackground(
    colors: XmbBackgroundColors,
    seconds: Float,
    motion: XmbWaveMotion,
) {
    drawRect(Brush.verticalGradient(listOf(colors.top, colors.bottom)))
    drawRect(
        Brush.radialGradient(
            colors = listOf(colors.highlight, Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.08f),
            radius = max(size.width, size.height) * 0.75f,
        ),
    )
    val wave = WaveShape(size.width, size.height, motion, seconds)
    drawGlowBand(wave)
    drawSheets(wave)
    drawStrands(wave)
    drawSparkles(wave, seconds)
}

/** The wave's geometry for one frame. */
private class WaveShape(
    val width: Float,
    val height: Float,
    val motion: XmbWaveMotion,
    seconds: Float,
) {
    val unit = height / 720f
    private val flow = motion.flow + motion.shiver * 0.08f * sin(seconds * 46f)
    private val lift = motion.lift * height * 0.06f

    /** Center line, in pixels: low on the left, rising to the right, like the console's. */
    fun centerY(u: Float): Float {
        val shape =
            0.6f - 0.13f * u +
                0.045f * sin(TAU * (0.55f * u + 0.035f * flow) + 0.6f) +
                0.02f * sin(TAU * (1.4f * u - 0.05f * flow) + 2.1f)
        return shape * height + lift
    }

    /** How far strands stray from the center line: widest on the left, as on the console. */
    fun spread(u: Float): Float =
        height * (0.045f + 0.035f * (1f - u) + 0.02f * sin(TAU * (0.7f * u + 0.02f * flow))) * (1f + 0.9f * motion.swell)

    fun strandY(
        strand: Strand,
        u: Float,
    ): Float = centerY(u) + spread(u) * strand.spread * sin(TAU * (strand.frequency * u - strand.speed * flow) + strand.phase)

    /** Brightness along a strand: slow glints, plus the ✕ pulse travelling out from the focus. */
    fun glint(
        strand: Strand,
        u: Float,
    ): Float {
        val glint = 0.45f + 0.55f * (0.5f + 0.5f * sin(TAU * (1.2f * u - 0.06f * flow) + strand.phase * 1.7f))
        return glint + pulse(u)
    }

    fun pulse(u: Float): Float {
        if (motion.flash <= 0f) return 0f
        val reach = motion.pulseAge * PULSE_SPEED
        val distance = abs(abs(u - PULSE_ORIGIN) - reach)
        return 1.6f * motion.flash * exp(-distance * distance / 0.004f)
    }

    fun path(y: (Float) -> Float): Path =
        Path().apply {
            for (step in 0..SEGMENTS) {
                val u = step.toFloat() / SEGMENTS
                if (step == 0) moveTo(0f, y(u)) else lineTo(u * width, y(u))
            }
        }
}

private fun DrawScope.drawGlowBand(wave: WaveShape) {
    val center = wave.path(wave::centerY)
    val boost = 1f + 0.8f * wave.motion.swell + 0.5f * wave.motion.flash
    listOf(0.32f to 0.05f, 0.2f to 0.07f, 0.12f to 0.08f, 0.06f to 0.09f).forEach { (width, alpha) ->
        drawPath(
            center,
            Color.White.copy(alpha = (alpha * boost).coerceAtMost(1f)),
            style = Stroke(width = wave.height * width, cap = StrokeCap.Round),
        )
    }
}

private fun DrawScope.drawSheets(wave: WaveShape) {
    sheets.forEach { (first, second) ->
        val a = strands[first]
        val b = strands[second]
        val sheet = wave.path { wave.strandY(a, it) }
        for (step in SEGMENTS downTo 0) {
            val u = step.toFloat() / SEGMENTS
            sheet.lineTo(u * wave.width, wave.strandY(b, u))
        }
        sheet.close()
        drawPath(sheet, Color.White.copy(alpha = 0.08f * (1f + 0.6f * wave.motion.swell)))
    }
}

private fun DrawScope.drawStrands(wave: WaveShape) {
    val flash = wave.motion.flash
    strands.forEach { strand ->
        val path = wave.path { wave.strandY(strand, it) }
        listOf(7f to 0.05f, 2.5f to 0.1f, 1f to 0.3f + 0.3f * flash).forEach { (width, alpha) ->
            val stops =
                Array(GLINT_STOPS) { index ->
                    val u = index.toFloat() / (GLINT_STOPS - 1)
                    u to Color.White.copy(alpha = (alpha * strand.alpha * wave.glint(strand, u)).coerceIn(0f, 1f))
                }
            drawPath(
                path,
                Brush.horizontalGradient(*stops, startX = 0f, endX = wave.width),
                style = Stroke(width = width * wave.unit, cap = StrokeCap.Round),
            )
        }
    }
}

private const val GLINT_STOPS = 17

private fun DrawScope.drawSparkles(
    wave: WaveShape,
    seconds: Float,
) {
    val motion = wave.motion
    sparkles.forEach { sparkle ->
        val parallax = 0.4f + 0.6f * sparkle.depth
        val u = (sparkle.u - motion.drift * parallax - seconds * 0.003f * sparkle.depth).mod(1f)
        val x = u * wave.width
        val baseY =
            if (sparkle.far) {
                wave.height * (0.5f + 0.5f * sparkle.offset)
            } else {
                wave.centerY(u) + sparkle.offset * wave.height * 0.16f * (1f + 0.5f * motion.swell)
            }
        val y =
            baseY + sin(seconds * sparkle.bobRate + sparkle.twinklePhase) * wave.unit * 4f + motion.lift * wave.height * 0.03f * parallax
        val twinkle = 0.5f + 0.5f * sin(seconds * sparkle.twinkleRate + sparkle.twinklePhase)
        val nearWave = if (sparkle.far) 1f else 1f - 0.5f * abs(sparkle.offset)
        val alpha =
            (
                sparkle.alpha * nearWave * (0.25f + 0.75f * twinkle * twinkle) * (
                    1f + 1.6f * motion.flash +
                        wave.pulse(
                            u,
                        )
                )
            ).coerceIn(0f, 1f)
        val radius = sparkle.size * wave.unit * (1f + 0.5f * motion.flash * sparkle.depth)
        if (sparkle.size > 1.8f) {
            // Bigger sparkles get a soft falloff, not a disc.
            val halo = radius * 3f
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = alpha * 0.35f), Color.Transparent),
                    center = Offset(x, y),
                    radius = halo,
                ),
                radius = halo,
                center = Offset(x, y),
            )
        }
        drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = Offset(x, y))
    }
}
