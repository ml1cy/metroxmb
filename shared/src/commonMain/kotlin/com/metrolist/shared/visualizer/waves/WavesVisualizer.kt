package com.metrolist.shared.visualizer.waves

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.xmb.theme.XmbBackgroundColors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * The Waves visualizer (M4): twisting silk ribbons over the month's color, with bokeh sparkles
 * around them, all driven by the music through [audio] (compare reference/waves). The ribbons are
 * a runtime shader where the platform has one ([PlatformWavesCanvas]); the sparkles are drawn on
 * top here. [seconds] drives twinkle; the ribbons follow [VisualizerAudio.flow], which runs faster
 * when the music is louder.
 */
@Composable
fun WavesVisualizer(
    colors: XmbBackgroundColors,
    seconds: () -> Float,
    audio: VisualizerAudio,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        PlatformWavesCanvas(WavesColors.from(colors), {
            WavesFrame(audio.flow, audio.bass, audio.mid, audio.treble, audio.beat)
        }, Modifier.fillMaxSize())
        Canvas(Modifier.fillMaxSize()) { drawWaveSparkles(seconds(), audio) }
    }
}

/** One frame's inputs for the ribbons. */
class WavesFrame(
    val time: Float,
    val bass: Float,
    val mid: Float,
    val treble: Float,
    val beat: Float,
)

/** The shader's colors, from the month's background colors. */
class WavesColors(
    val base: Color,
    val glow: Color,
) {
    companion object {
        fun from(colors: XmbBackgroundColors) =
            WavesColors(
                base = colors.top,
                glow =
                    androidx.compose.ui.graphics
                        .lerp(colors.top, Color.White, 0.45f),
            )
    }
}

/** Draws the ribbons; platforms without runtime shaders draw [drawWavesFallback]. */
@Composable
internal expect fun PlatformWavesCanvas(
    colors: WavesColors,
    frame: () -> WavesFrame,
    modifier: Modifier,
)

/**
 * The ribbons' center line in the same terms as the shader: [x] across the screen (0..1), in
 * fractions of the height. Sparkles and the fallback follow it.
 */
internal fun wavesCenter(
    x: Float,
    aspect: Float,
    frame: WavesFrame,
): Float {
    val u = x * aspect * 1.6f
    val amp = 0.035f + 0.05f * frame.bass
    return 0.5f - 0.12f * (x - 0.5f) + wavesCurve(u, 2f, frame.time, amp)
}

internal fun wavesCurve(
    u: Float,
    seed: Float,
    time: Float,
    amp: Float,
): Float =
    amp * (
        sin(u * (2.1f + seed * 0.37f) + time * (0.31f + seed * 0.05f) + seed * 1.7f) +
            0.45f * sin(u * (4.3f + seed * 0.21f) - time * (0.47f + seed * 0.03f) + seed * 3.1f)
    )

/** Platforms without a runtime shader: a dark vignette and soft strands along the center line. */
internal fun DrawScope.drawWavesFallback(
    colors: WavesColors,
    frame: WavesFrame,
) {
    drawRect(
        Brush.radialGradient(
            listOf(colors.base.copy(alpha = 1f).darken(0.6f), colors.base.darken(0.12f)),
            center =
                Offset(size.width * 0.38f, size.height * 0.52f),
            radius = size.maxDimension * 0.8f,
        ),
    )
    val aspect = size.width / size.height
    for (strand in 0 until 5) {
        val path =
            androidx.compose.ui.graphics
                .Path()
        for (step in 0..96) {
            val x = step / 96f
            val y = (wavesCenter(x, aspect, frame) + (strand - 2) * 0.018f * (1f + frame.mid)) * size.height
            if (step == 0) path.moveTo(0f, y) else path.lineTo(x * size.width, y)
        }
        drawPath(
            path,
            colors.glow.copy(alpha = 0.35f + 0.3f * frame.beat),
            style =
                androidx.compose.ui.graphics.drawscope.Stroke(
                    width =
                        size.height / 720f * 1.5f,
                ),
        )
    }
}

private fun Color.darken(factor: Float) = Color(red * factor, green * factor, blue * factor, alpha)

private class Bokeh(
    val u: Float,
    val offset: Float,
    val size: Float,
    val depth: Float,
    val alpha: Float,
    val rate: Float,
    val phase: Float,
)

private val bokeh: List<Bokeh> =
    Random(0xB0CE).let { random ->
        List(BOKEH_COUNT) {
            val bell = (random.nextFloat() + random.nextFloat() + random.nextFloat() - 1.5f) / 1.5f
            Bokeh(
                u = random.nextFloat(),
                offset = bell,
                size = 1f + random.nextFloat() * random.nextFloat() * 5f,
                depth = random.nextFloat(),
                alpha = 0.25f + random.nextFloat() * 0.6f,
                rate = 0.5f + random.nextFloat() * 2f,
                phase = random.nextFloat() * TAU,
            )
        }
    }

private const val BOKEH_COUNT = 160
private const val TAU = (2 * PI).toFloat()

/**
 * Bokeh sparkles around the ribbons: they drift with the flow, twinkle, brighten with the treble
 * and swell on beats, as in the reference frame.
 */
internal fun DrawScope.drawWaveSparkles(
    seconds: Float,
    audio: VisualizerAudio,
) {
    val frame = WavesFrame(audio.flow, audio.bass, audio.mid, audio.treble, audio.beat)
    val unit = size.height / 720f
    val aspect = size.width / size.height
    bokeh.forEach { dot ->
        val x = (dot.u - audio.flow * 0.012f * (0.4f + dot.depth)).mod(1f)
        val spread = size.height * 0.14f * (1f + 0.8f * audio.mid)
        val y = wavesCenter(x, aspect, frame) * size.height + dot.offset * spread + sin(seconds * 0.4f + dot.phase) * unit * 6f
        val twinkle = 0.5f + 0.5f * sin(seconds * dot.rate + dot.phase)
        val near = 1f - 0.5f * abs(dot.offset)
        val alpha = (dot.alpha * near * (0.3f + 0.7f * twinkle) * (0.7f + 0.8f * audio.treble + 0.6f * audio.beat)).coerceIn(0f, 1f)
        val radius = dot.size * unit * (1f + 0.6f * audio.beat * dot.depth)
        val center = Offset(x * size.width, y)
        if (dot.size > 2.2f) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = alpha * 0.5f), Color.Transparent),
                    center = center,
                    radius =
                        radius * 2.5f,
                ),
                radius = radius * 2.5f,
                center = center,
            )
        }
        drawCircle(Color.White.copy(alpha = alpha), radius = radius, center = center)
    }
}

/** SkSL for the ribbons. Uniforms: resolution, time, audio (bass, mid, treble, beat), base, glow. */
internal const val WAVES_SHADER = """
uniform float2 resolution;
uniform float time;
uniform float4 audio;
uniform float3 base;
uniform float3 glow;

float curve(float u, float seed, float t, float amp) {
    return amp * (sin(u * (2.1 + seed * 0.37) + t * (0.31 + seed * 0.05) + seed * 1.7)
        + 0.45 * sin(u * (4.3 + seed * 0.21) - t * (0.47 + seed * 0.03) + seed * 3.1));
}

half4 main(float2 p) {
    float2 uv = p / resolution;
    float aspect = resolution.x / resolution.y;
    float bass = audio.x;
    float mid = audio.y;
    float treble = audio.z;
    float beat = audio.w;
    // Near black, with the month's color glowing up from the lower left, as in the reference.
    float2 c = (uv - float2(0.0, 1.0)) * float2(aspect, 1.0);
    float glowBg = smoothstep(1.6, 0.0, length(c));
    float3 col = base * (0.03 + 0.42 * glowBg * glowBg);
    float u = uv.x * aspect * 1.6;
    float amp = 0.035 + 0.05 * bass;
    // The band rises to the right; every ribbon follows its center line with a little drift of its own.
    float center = 0.5 - 0.12 * (uv.x - 0.5) + curve(u, 2.0, time, amp);
    float width = 0.045 + 0.035 * mid;
    float light = 0.0;
    for (int i = 0; i < 5; i++) {
        float s = float(i);
        float m = center + (s - 2.0) * 0.026 * (1.0 + 0.6 * mid) + 0.45 * curve(u * 0.8, s, time * 0.8, amp);
        float twist = cos(u * (0.9 + 0.13 * s) + time * (0.21 + 0.035 * s) + s * 2.3);
        float hw = width * (0.25 + 0.75 * abs(twist));
        float d = uv.y - m;
        float inside = smoothstep(hw + 0.003, hw - 0.003, abs(d));
        float e = abs(abs(d) - hw);
        float edge = exp(-e * e * (60000.0 - 25000.0 * treble));
        float fold = 1.0 + 1.2 * (1.0 - abs(twist));
        // Faces are faint and darker in the middle, edges bright: lit silk seen from the side.
        float face = inside * (0.07 + 0.11 * (abs(d) / max(hw, 0.0001)));
        light += face * fold + edge * (0.16 + 0.14 * beat) * fold;
    }
    float b = uv.y - center;
    light += 0.10 * exp(-b * b * 45.0) * (0.5 + 0.6 * bass + 0.5 * beat);
    col += glow * light;
    return half4(half3(col), 1.0);
}
"""
