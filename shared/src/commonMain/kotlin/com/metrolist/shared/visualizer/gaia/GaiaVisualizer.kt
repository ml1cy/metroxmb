package com.metrolist.shared.visualizer.gaia

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.visualizer.VisualizerAudio
import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.visualizer.shader.XmbShader
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.sqrt

/**
 * Gaia, from the owner's console recording (`reference/other/`): the Earth in space. A slow
 * camera moves through the recording's views: close over the clouds, the whole globe, a sunrise
 * over the dark limb, the planet far away. The surface, clouds, water and city lights are NASA's
 * Blue Marble and Black Marble maps (public domain, `files/xmb/gaia/`); until they've loaded, a
 * noise-made Earth stands in. The music's energy brightens the atmosphere and clouds; beats make
 * the city lights pulse.
 */
@Composable
fun GaiaVisualizer(
    audio: VisualizerAudio,
    seconds: () -> Float,
    quality: VisualizerQuality,
    modifier: Modifier = Modifier,
) {
    val procedural = remember { XmbShader(GAIA_SHADER) }
    val earth = remember { XmbShader(GAIA_EARTH_SHADER) }
    val maps by produceState<GaiaMaps?>(null, earth) {
        if (!earth.available) return@produceState
        holdGaiaMaps()
        try {
            value = runCatching { loadGaiaMaps() }.getOrNull()?.also { it.bind(earth) }
            awaitCancellation()
        } finally {
            releaseGaiaMaps()
        }
    }
    Canvas(modifier) {
        val textured = maps
        val shader = if (textured != null) earth else procedural
        seconds()
        val view = gaiaView(audio.calmFlow)
        val center = Offset(view.x * size.width, view.y * size.height)
        val radius = view.radius * size.height
        if (!shader.available) return@Canvas drawGaiaFallback(center, radius, audio)
        val length = sqrt(view.sunX * view.sunX + view.sunY * view.sunY + view.sunZ * view.sunZ)
        shader.set("iResolution", size.width, size.height)
        shader.set("iTime", audio.calmFlow)
        shader.set("center", center.x, center.y)
        shader.set("radius", radius)
        shader.set("sun", view.sunX / length, view.sunY / length, view.sunZ / length)
        shader.set("energy", audio.calmEnergy)
        shader.set("beat", audio.beat)
        if (textured != null) {
            textured.sizes(shader)
            // Far away, the planet takes the quarter-size maps: the full ones would shimmer.
            val texelsPerPixel = textured.width / (2f * PI.toFloat() * radius)
            shader.set("lod", (log2(texelsPerPixel.coerceAtLeast(1f)) / 2f).coerceIn(0f, 1f))
        } else {
            shader.set("octaves", quality.gaiaOctaves.toFloat())
        }
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

/**
 * The Earth maps: the surface and clouds at full size (4096 × 2048) and a quarter of it, the water
 * mask and city lights at 2048 × 1024. Decoded once and kept for the app's life.
 */
internal class GaiaMaps(
    private val day: ImageBitmap,
    private val dayLow: ImageBitmap,
    private val clouds: ImageBitmap,
    private val cloudsLow: ImageBitmap,
    private val water: ImageBitmap,
    private val lights: ImageBitmap,
) {
    val width: Float get() = day.width.toFloat()

    fun bind(shader: XmbShader) {
        shader.setImage("dayHi", day)
        shader.setImage("dayLo", dayLow)
        shader.setImage("cloudsHi", clouds)
        shader.setImage("cloudsLo", cloudsLow)
        shader.setImage("water", water)
        shader.setImage("lights", lights)
    }

    fun sizes(shader: XmbShader) {
        shader.set("hiSize", day.width.toFloat(), day.height.toFloat())
        shader.set("loSize", dayLow.width.toFloat(), dayLow.height.toFloat())
        shader.set("smallSize", water.width.toFloat(), water.height.toFloat())
    }
}

private var gaiaMaps: GaiaMaps? = null
private val gaiaMapsLock = Mutex()

// The maps are about 85 MB of bitmaps: they're kept while a Gaia view is shown and for a minute
// after the last one closes (auto-cycle comes back to Gaia), then dropped.
private var gaiaMapUsers = 0
private var gaiaMapRelease: Job? = null
private val gaiaMapScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun holdGaiaMaps() {
    synchronizedGaia {
        gaiaMapUsers++
        gaiaMapRelease?.cancel()
        gaiaMapRelease = null
    }
}

private fun releaseGaiaMaps() {
    synchronizedGaia {
        if (--gaiaMapUsers > 0) return@synchronizedGaia
        gaiaMapRelease =
            gaiaMapScope.launch {
                delay(GAIA_MAPS_KEEP_MS)
                gaiaMapsLock.withLock { if (gaiaMapUsers == 0) gaiaMaps = null }
            }
    }
}

private val gaiaUseLock = SynchronizedObject()

private inline fun synchronizedGaia(block: () -> Unit) = synchronized(gaiaUseLock, block)

private const val GAIA_MAPS_KEEP_MS = 60_000L

@OptIn(ExperimentalResourceApi::class)
internal suspend fun loadGaiaMaps(): GaiaMaps =
    gaiaMapsLock.withLock {
        gaiaMaps ?: withContext(Dispatchers.Default) {
            fun read(name: String) = suspend { Res.readBytes("files/xmb/gaia/$name").decodeToImageBitmap() }
            val day = read("day.jpg")()
            val clouds = read("clouds.jpg")()
            GaiaMaps(day, quarter(day), clouds, quarter(clouds), read("water.jpg")(), read("lights.jpg")())
        }.also { gaiaMaps = it }
    }

/** [image] at a quarter of its size, filtered smoothly: the far views' maps. */
private fun quarter(image: ImageBitmap): ImageBitmap =
    ImageBitmap(image.width / 4, image.height / 4).also { small ->
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(small), Size(small.width.toFloat(), small.height.toFloat())) {
            drawImage(image, dstSize = IntSize(small.width, small.height), filterQuality = FilterQuality.High)
        }
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

/**
 * The Earth from its maps: the surface point under the pixel (turned slowly, tilted) gives
 * latitude and longitude, which sample the surface, clouds (drifting a little), water mask and
 * city lights, blended toward the quarter-size maps by [lod]. Lit by the sun with a soft
 * terminator, sun glint on water only, lit clouds, lights on the night side, atmospheric haze
 * toward the rim, and an anti-aliased limb with its halo, on soft stars.
 */
internal const val GAIA_EARTH_SHADER = """
uniform shader dayHi;
uniform shader dayLo;
uniform shader cloudsHi;
uniform shader cloudsLo;
uniform shader water;
uniform shader lights;
uniform float2 hiSize;
uniform float2 loSize;
uniform float2 smallSize;
uniform float lod;
uniform float2 iResolution;
uniform float iTime;
uniform float2 center;
uniform float radius;
uniform float3 sun;
uniform float energy;
uniform float beat;

float hash(float2 p) {
    float3 p3 = fract(float3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float3 stars(float2 fragCoord) {
    float2 cell = floor(fragCoord / 3.0);
    float seed = hash(cell);
    if (seed < 0.996) return float3(0.0);
    float2 at = (cell + float2(hash(cell + 7.1), hash(cell + 3.7))) * 3.0;
    float d = length(fragCoord - at);
    float twinkle = 0.75 + 0.25 * sin(iTime * 2.0 + seed * 90.0);
    return float3(exp(-d * d * 1.2) * (seed - 0.996) * 250.0 * twinkle);
}

half4 main(float2 fragCoord) {
    float2 p = (fragCoord - center) / radius;
    p.y = -p.y;
    float r = length(p);
    float3 light = normalize(sun);
    float2 rimDirection = r > 0.0 ? p / r : float2(0.0, 1.0);
    float facing = dot(rimDirection, normalize(light.xy + float2(0.0001, 0.0)));
    float3 space = stars(fragCoord);
    float halo = exp(-max(r - 1.0, 0.0) / 0.018);
    space += float3(0.35, 0.6, 1.0) * halo * clamp(facing + 0.35, 0.0, 1.0) * (0.55 + 0.5 * energy) * step(1.0, r);

    float3 planet = float3(0.0);
    if (r < 1.0 + 2.0 / radius) {
        float3 n = float3(p, sqrt(max(1.0 - r * r, 0.0)));
        n = normalize(n);
        float turn = iTime * 0.01;
        float tilt = 0.35;
        float3 q = float3(n.x * cos(turn) + n.z * sin(turn), n.y, -n.x * sin(turn) + n.z * cos(turn));
        q = float3(q.x, q.y * cos(tilt) - q.z * sin(tilt), q.y * sin(tilt) + q.z * cos(tilt));
        float2 uv = float2(atan(q.x, q.z) / 6.2831853 + 0.5, 0.5 - asin(clamp(q.y, -1.0, 1.0)) / 3.1415927);
        float3 day = mix(dayHi.eval(uv * hiSize).rgb, dayLo.eval(uv * loSize).rgb, lod);
        float2 cloudUv = float2(fract(uv.x + iTime * 0.0004), uv.y);
        float cloud = mix(cloudsHi.eval(cloudUv * hiSize).r, cloudsLo.eval(cloudUv * loSize).r, lod);
        float sea = water.eval(uv * smallSize).r;
        float city = lights.eval(uv * smallSize).r;

        float sunAngle = dot(n, light);
        float lit = smoothstep(-0.08, 0.35, sunAngle) * clamp(sunAngle * 0.9 + 0.25, 0.0, 1.0);
        float3 surface = pow(day, float3(1.15)) * 1.15 * lit;
        float3 reflected = reflect(-light, n);
        surface += float3(1.0, 0.92, 0.8) * sea * pow(max(reflected.z, 0.0), 120.0) * 0.35 * lit;
        float3 cloudColor = float3(1.0) * (0.04 + 1.05 * lit) * (0.9 + 0.2 * energy);
        surface = mix(surface, cloudColor, cloud * 0.92);
        float night = smoothstep(0.05, -0.18, sunAngle);
        surface += float3(1.0, 0.72, 0.42) * pow(city, 1.4) * 1.6 * night * (1.0 - cloud * 0.85) * (0.8 + 0.5 * beat);
        float fresnel = pow(1.0 - n.z, 2.2);
        float3 air = float3(0.32, 0.55, 1.0) * clamp(sunAngle + 0.35, 0.0, 1.0);
        planet = mix(surface, air, fresnel * 0.55) + air * fresnel * 0.35 * (0.8 + 0.4 * energy);
    }
    // One pixel of smooth edge, so the limb doesn't step.
    float cover = clamp((1.0 - r) * radius + 0.5, 0.0, 1.0);
    float3 color = mix(space, planet, cover);

    float behind = smoothstep(0.0, -0.5, light.z);
    float limb = exp(-abs(r - 1.0) * 70.0);
    color += float3(1.0, 0.48, 0.18) * limb * behind * clamp(facing, 0.0, 1.0) * 1.4;
    float2 sunAt = normalize(light.xy + float2(0.0001, 0.0)) * 1.01;
    color += float3(1.0, 0.95, 0.85) * exp(-length(p - sunAt) * 18.0) * behind * 1.6;
    return half4(color, 1.0);
}
"""
