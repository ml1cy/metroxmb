package com.metrolist.shared.visualizer.gaia

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.visualizer.shader.XmbShader
import kotlin.math.floor
import kotlin.math.sqrt

/**
 * Gaia, from the owner's console recording (`reference/other/`): the Earth in space. A slow
 * camera moves through the recording's views: close over the clouds, the whole globe, a sunrise
 * over the dark limb, the planet far away. Oceans, continents, ice, clouds and city lights on the
 * night side are made from noise on the sphere; the atmosphere glows at the edge. The music's
 * energy brightens the atmosphere and clouds; beats make the city lights pulse.
 */
@Composable
fun GaiaVisualizer(
    audio: VisualizerAudio,
    seconds: () -> Float,
    quality: VisualizerQuality,
    modifier: Modifier = Modifier,
) {
    val shader = remember { XmbShader(GAIA_SHADER) }
    Canvas(modifier) {
        seconds()
        val view = gaiaView(audio.flow)
        val center = Offset(view.x * size.width, view.y * size.height)
        val radius = view.radius * size.height
        if (!shader.available) return@Canvas drawGaiaFallback(center, radius, audio)
        val length = sqrt(view.sunX * view.sunX + view.sunY * view.sunY + view.sunZ * view.sunZ)
        shader.set("iResolution", size.width, size.height)
        shader.set("iTime", audio.flow)
        shader.set("center", center.x, center.y)
        shader.set("radius", radius)
        shader.set("sun", view.sunX / length, view.sunY / length, view.sunZ / length)
        shader.set("energy", audio.energy)
        shader.set("beat", audio.beat)
        shader.set("octaves", quality.gaiaOctaves.toFloat())
        shader.draw(this)
    }
}

/**
 * Where the camera is: the planet's center ([x], [y] as shares of the width and height), its
 * [radius] as a share of the height, and the direction of the sun (y up, z toward the camera).
 */
internal data class GaiaView(
    val x: Float,
    val y: Float,
    val radius: Float,
    val sunX: Float,
    val sunY: Float,
    val sunZ: Float,
)

/** The recording's views, in order: close over the clouds, the globe, sunrise, far away. */
internal val GAIA_VIEWS =
    listOf(
        GaiaView(0.92f, 1.3f, 1.25f, -0.6f, 0.45f, 0.65f),
        GaiaView(0.6f, 0.5f, 0.42f, -0.55f, 0.3f, 0.8f),
        GaiaView(0.66f, 0.02f, 0.62f, -0.55f, -0.5f, -0.65f),
        GaiaView(0.82f, 0.3f, 0.12f, -0.75f, 0.25f, 0.6f),
    )

/** The camera at flow clock [time]: each view holds, then the camera eases to the next. */
internal fun gaiaView(time: Float): GaiaView {
    val position = time / VIEW_SECONDS
    val index = floor(position).toInt().mod(GAIA_VIEWS.size)
    val from = GAIA_VIEWS[index]
    val to = GAIA_VIEWS[(index + 1) % GAIA_VIEWS.size]
    val t = ((position - floor(position) - VIEW_HOLD) / (1f - VIEW_HOLD)).coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

    fun mix(
        a: Float,
        b: Float,
    ) = a + (b - a) * t
    return GaiaView(
        mix(from.x, to.x),
        mix(from.y, to.y),
        mix(from.radius, to.radius),
        mix(from.sunX, to.sunX),
        mix(from.sunY, to.sunY),
        mix(from.sunZ, to.sunZ),
    )
}

/** Without shaders (Android before 13): a lit globe with an atmosphere ring on black. */
private fun DrawScope.drawGaiaFallback(
    center: Offset,
    radius: Float,
    audio: VisualizerAudio,
) {
    drawRect(Color.Black)
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0.35f, 0.6f, 1f).copy(alpha = 0.35f + 0.25f * audio.energy), Color.Transparent),
            center,
            radius * 1.12f,
        ),
        radius * 1.12f,
        center,
    )
    drawCircle(
        Brush.radialGradient(
            listOf(Color(0.3f, 0.5f, 0.7f), Color(0.05f, 0.15f, 0.3f), Color(0.01f, 0.02f, 0.05f)),
            Offset(center.x - radius * 0.35f, center.y - radius * 0.3f),
            radius * 1.6f,
        ),
        radius,
        center,
    )
}

/** Seconds of flow clock per view, and the share of it the camera holds still. */
private const val VIEW_SECONDS = 40f
private const val VIEW_HOLD = 0.75f

/**
 * The planet: a sphere under the pixel (y up, z toward the camera), turned slowly; land, ocean,
 * ice and clouds from 3D noise of the surface point, lit by the sun, with city lights on the
 * night side, an ocean glint, the atmosphere at the rim and a halo outside it (orange where the
 * sun is just behind the limb), on stars.
 */
internal const val GAIA_SHADER = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 center;
uniform float radius;
uniform float3 sun;
uniform float energy;
uniform float beat;
uniform float octaves;

float hash(float3 p) {
    return fract(sin(dot(p, float3(127.1, 311.7, 74.7))) * 43758.5453);
}

float noise(float3 p) {
    float3 i = floor(p);
    float3 f = fract(p);
    float3 u = f * f * (3.0 - 2.0 * f);
    float a = mix(hash(i), hash(i + float3(1.0, 0.0, 0.0)), u.x);
    float b = mix(hash(i + float3(0.0, 1.0, 0.0)), hash(i + float3(1.0, 1.0, 0.0)), u.x);
    float c = mix(hash(i + float3(0.0, 0.0, 1.0)), hash(i + float3(1.0, 0.0, 1.0)), u.x);
    float d = mix(hash(i + float3(0.0, 1.0, 1.0)), hash(i + float3(1.0, 1.0, 1.0)), u.x);
    return mix(mix(a, b, u.y), mix(c, d, u.y), u.z);
}

float fbm(float3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 6; i++) {
        if (float(i) >= octaves) break;
        value += amplitude * noise(p);
        p = p * 2.07 + float3(3.1, 1.7, 5.3);
        amplitude *= 0.5;
    }
    return value;
}

half4 main(float2 fragCoord) {
    float3 color = float3(0.0);
    float2 cell = floor(fragCoord / 2.0);
    float star = hash(float3(cell, 7.0));
    if (star > 0.9975) {
        color += float3(0.75 + 0.25 * sin(iTime * 2.0 + star * 90.0)) * (star - 0.9975) * 300.0;
    }

    float2 p = (fragCoord - center) / radius;
    p.y = -p.y;
    float r = length(p);
    float3 light = normalize(sun);
    float2 rimDirection = r > 0.0 ? p / r : float2(0.0, 1.0);
    float facing = dot(rimDirection, normalize(light.xy + float2(0.0001, 0.0)));

    if (r < 1.0) {
        float3 n = float3(p, sqrt(1.0 - r * r));
        float turn = iTime * 0.02;
        float tilt = 0.4;
        float3 q = float3(n.x * cos(turn) + n.z * sin(turn), n.y, -n.x * sin(turn) + n.z * cos(turn));
        q = float3(q.x, q.y * cos(tilt) - q.z * sin(tilt), q.y * sin(tilt) + q.z * cos(tilt));

        float land = fbm(q * 2.2);
        float isLand = smoothstep(0.5, 0.53, land);
        float3 ocean = mix(float3(0.01, 0.04, 0.11), float3(0.03, 0.13, 0.27), fbm(q * 5.0));
        float3 soil = mix(float3(0.1, 0.17, 0.06), float3(0.45, 0.38, 0.25), smoothstep(0.53, 0.72, land));
        float3 surface = mix(ocean, soil, isLand);
        surface = mix(surface, float3(0.92), smoothstep(0.78, 0.9, abs(q.y)));

        float clouds = smoothstep(0.48, 0.78, fbm(q * 3.3 + float3(iTime * 0.012, 0.0, 0.0)));
        float lit = max(dot(n, light), 0.0);
        float3 shade = surface * (0.02 + 1.15 * lit);
        float3 halfway = normalize(light + float3(0.0, 0.0, 1.0));
        shade += (1.0 - isLand) * pow(max(dot(n, halfway), 0.0), 70.0) * 0.55 * lit;
        shade = mix(shade, float3(1.0) * (0.03 + 1.05 * lit) * (0.85 + 0.25 * energy), clouds * 0.85);

        float night = smoothstep(0.08, -0.2, dot(n, light));
        float cities = isLand * pow(noise(q * 38.0), 9.0) * 5.0 * (1.0 - clouds);
        shade += float3(1.0, 0.62, 0.28) * cities * night * (0.75 + 0.7 * beat);

        float rim = pow(1.0 - n.z, 3.0);
        shade += float3(0.3, 0.55, 1.0) * rim * clamp(dot(n, light) + 0.35, 0.0, 1.0) * (0.75 + 0.5 * energy);
        color = shade;
    } else {
        float outside = r - 1.0;
        float halo = exp(-outside * 28.0);
        color += float3(0.35, 0.6, 1.0) * halo * clamp(facing + 0.3, 0.0, 1.0) * (0.6 + 0.6 * energy);
    }

    float behind = smoothstep(0.0, -0.5, light.z);
    float limb = exp(-abs(r - 1.0) * 70.0);
    color += float3(1.0, 0.48, 0.18) * limb * behind * clamp(facing, 0.0, 1.0) * 1.4;
    float2 sunAt = normalize(light.xy + float2(0.0001, 0.0)) * 1.01;
    float sunGlow = exp(-length(p - sunAt) * 18.0) * behind;
    color += float3(1.0, 0.95, 0.85) * sunGlow * 1.6;
    return half4(color, 1.0);
}
"""
