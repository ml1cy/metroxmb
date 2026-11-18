package com.metrolist.shared.visualizer.valley

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.lerp
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.visualizer.shader.XmbShader
import kotlin.math.floor
import kotlin.math.sin

/**
 * Valley, from the owner's console recordings (`reference/valley/`): a soft flight low over molten
 * terrain. Ridges stand in layers from the far haze to the near, blurrier ground, each with a
 * glowing crest; the colors drift through molten orange, gold, deep blue and crimson. The bass
 * lifts the ridges, the music's energy speeds the flight and beats brighten the crests.
 */
@Composable
fun ValleyVisualizer(
    audio: VisualizerAudio,
    seconds: () -> Float,
    quality: VisualizerQuality,
    modifier: Modifier = Modifier,
) {
    val shader = remember { XmbShader(VALLEY_SHADER) }
    Canvas(modifier) {
        seconds()
        val palette = valleyPalette(audio.flow)
        if (!shader.available) return@Canvas drawValleyFallback(audio, palette)
        shader.set("iResolution", size.width, size.height)
        shader.set("iTime", audio.flow * VALLEY_SPEED)
        shader.set("bass", audio.bass)
        shader.set("energy", audio.energy)
        shader.set("beat", audio.beat)
        shader.set("layers", quality.valleyLayers.toFloat())
        shader.set("octaves", quality.valleyOctaves.toFloat())
        shader.set("haze", palette.haze.red, palette.haze.green, palette.haze.blue)
        shader.set("ground", palette.ground.red, palette.ground.green, palette.ground.blue)
        shader.set("glow", palette.glow.red, palette.glow.green, palette.glow.blue)
        shader.draw(this)
    }
}

/** The colors at one moment: [haze] near the horizon, the [ground], and the crests' [glow]. */
internal class ValleyPalette(
    val haze: Color,
    val ground: Color,
    val glow: Color,
)

private val VALLEY_PALETTES =
    listOf(
        // Molten, gold, night blue, crimson: the recordings drift through these.
        ValleyPalette(Color(0.55f, 0.12f, 0.02f), Color(0.95f, 0.42f, 0.07f), Color(1f, 0.85f, 0.5f)),
        ValleyPalette(Color(0.38f, 0.24f, 0.02f), Color(0.95f, 0.72f, 0.12f), Color(1f, 0.95f, 0.72f)),
        ValleyPalette(Color(0.05f, 0.1f, 0.3f), Color(0.22f, 0.45f, 0.95f), Color(0.72f, 0.9f, 1f)),
        ValleyPalette(Color(0.28f, 0.03f, 0.14f), Color(0.78f, 0.16f, 0.32f), Color(1f, 0.72f, 0.85f)),
    )

/** The palette at flow clock [time]: each holds for a while, then blends into the next. */
internal fun valleyPalette(time: Float): ValleyPalette {
    val position = time / PALETTE_SECONDS
    val index = floor(position).toInt().mod(VALLEY_PALETTES.size)
    val next = VALLEY_PALETTES[(index + 1) % VALLEY_PALETTES.size]
    val current = VALLEY_PALETTES[index]
    val blend = ((position - floor(position) - PALETTE_HOLD) / (1f - PALETTE_HOLD)).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }
    return ValleyPalette(
        lerp(current.haze, next.haze, blend),
        lerp(current.ground, next.ground, blend),
        lerp(current.glow, next.glow, blend),
    )
}

/** Without shaders (Android before 13): the haze, then soft ridges drawn as filled curves. */
private fun DrawScope.drawValleyFallback(
    audio: VisualizerAudio,
    palette: ValleyPalette,
) {
    val horizon = size.height * 0.55f
    drawRect(Brush.verticalGradient(0f to Color.Black, 0.55f to palette.haze.copy(alpha = 1f), 1f to Color.Black))
    val time = audio.flow * VALLEY_SPEED
    for (layer in 0 until FALLBACK_LAYERS) {
        val near = layer / (FALLBACK_LAYERS - 1f)
        val path = Path()
        val base = horizon + size.height * 0.45f * near * near
        path.moveTo(0f, size.height)
        for (step in 0..FALLBACK_STEPS) {
            val x = size.width * step / FALLBACK_STEPS
            val wave =
                sin(step * 0.31f + layer * 1.7f + time * (0.3f + near)) * 0.6f +
                    sin(step * 0.13f - layer * 0.9f + time * 0.2f) * 0.4f
            path.lineTo(x, base - size.height * (0.05f + 0.08f * (1f - near)) * (1f + 0.6f * audio.bass) * (0.5f + 0.5f * wave))
        }
        path.lineTo(size.width, size.height)
        path.close()
        val color = lerp(palette.haze, palette.ground, 0.35f + 0.65f * near)
        drawPath(
            path,
            Brush.verticalGradient(
                listOf(lerp(color, palette.glow, 0.4f + 0.4f * audio.beat), Color.Black),
                startY =
                    base - size.height * 0.12f,
                endY = size.height,
            ),
        )
    }
    drawRect(
        Brush.radialGradient(
            listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)),
            Offset(size.width / 2f, size.height / 2f),
            size.maxDimension * 0.75f,
        ),
    )
}

/** The flight's clock against the audio flow, and how long each palette lasts (flow seconds). */
private const val VALLEY_SPEED = 0.6f
private const val PALETTE_SECONDS = 28f
private const val PALETTE_HOLD = 0.7f
private const val FALLBACK_LAYERS = 6
private const val FALLBACK_STEPS = 48

/**
 * Ridges as layers from far to near: each layer's height along the screen comes from noise at its
 * distance; perspective puts it on screen, the pixel takes the nearest layer in front of it with a
 * soft edge (softer when near, like a lens), a crest glow, and haze by distance.
 */
internal const val VALLEY_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float bass;
uniform float energy;
uniform float beat;
uniform float layers;
uniform float octaves;
uniform float3 haze;
uniform float3 ground;
uniform float3 glow;

float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + float2(1.0, 0.0)), u.x), mix(hash(i + float2(0.0, 1.0)), hash(i + float2(1.0, 1.0)), u.x), u.y);
}

float fbm(float2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 5; i++) {
        if (float(i) >= octaves) break;
        value += amplitude * noise(p);
        p = p * 2.03 + float2(1.7, 9.2);
        amplitude *= 0.5;
    }
    return value;
}

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / iResolution;
    float aspect = iResolution.x / iResolution.y;
    float sx = (uv.x - 0.5) * aspect;
    float sy = 1.0 - uv.y;
    float horizon = 0.55 + 0.03 * sin(iTime * 0.07);
    float camX = sin(iTime * 0.11) * 1.4;
    float camH = 0.9 + 0.08 * sin(iTime * 0.09);
    float camZ = iTime;

    float above = clamp((sy - horizon) / (1.0 - horizon), 0.0, 1.0);
    float3 color = mix(haze * 0.6, haze * 0.03, pow(above, 0.5));
    color += haze * 0.35 * exp(-abs(sy - horizon) * 9.0);

    for (int i = 0; i < 28; i++) {
        if (float(i) >= layers) break;
        float t = float(i) / max(layers - 1.0, 1.0);
        float depth = mix(7.0, 0.35, pow(t, 0.8));
        float wx = camX + sx * depth * 1.1;
        float wz = camZ + depth;
        float h = fbm(float2(wx * 0.45, wz * 0.45));
        h = pow(h, 1.3) * 1.35 * (0.85 + 0.6 * bass);
        // Far away, broad mountains rise over the horizon, as in the recordings.
        h += (1.0 - t) * 2.4 * pow(noise(float2(wx * 0.12, wz * 0.05)), 2.0);
        float line = horizon + (h - camH) * 0.5 / depth;
        float soft = 0.004 + 0.04 * t * t;
        float cover = smoothstep(line + soft, line - soft, sy);
        float below = max(line - sy, 0.0);
        float crest = exp(-below * mix(55.0, 14.0, t));
        float streak = pow(noise(float2(wx * 4.0, wz * 0.8)), 5.0) * t;
        float3 layerColor = ground * (0.06 + 0.55 * h);
        layerColor += glow * crest * (0.5 + 0.5 * energy + 0.8 * beat);
        layerColor += glow * streak * 1.2;
        float fog = clamp(exp(-depth * 0.25) * 1.4, 0.0, 1.0);
        layerColor = mix(haze * 0.5, layerColor, fog);
        color = mix(color, layerColor, cover);
    }
    float2 centered = uv - 0.5;
    color *= 1.0 - dot(centered, centered) * 0.9;
    return half4(color, 1.0);
}
"""
