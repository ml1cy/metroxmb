/*
 * The ribbon and particle math in this file is ported from the WebGL XMB at
 * https://github.com/tsbehlman/xmb, itself adapted from RetroArch's XMB ribbon shader:
 *
 *   RetroArch - A frontend for libretro.
 *   Copyright (C) 2010-2014 - Hans-Kristian Arntzen
 *   Copyright (C) 2011-2017 - Daniel De Matteis
 *   Copyright (C) 2012-2015 - Michael Lelli
 *   Modifications Copyright (C) 2018 - Trevor Behlman
 *
 * Both are licensed under the GNU General Public License version 3 or later, like this project.
 */
package com.metrolist.shared.visualizer.background

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * One frame of the XMB ribbon. [time] is the ribbon's own clock (about a quarter of real time at
 * rest); [amplitude] scales the folds, [opacity] the sheet and [particleOpacity] the particles;
 * [lift] moves everything up (positive) in normalized device units; [drift] slides the particles
 * left, in screen widths.
 */
internal class RibbonFrame(
    val time: Float,
    val amplitude: Float = 1f,
    val opacity: Float = RIBBON_OPACITY,
    val particleOpacity: Float = RIBBON_PARTICLE_OPACITY,
    val lift: Float = 0f,
    val drift: Float = 0f,
    /** How many times the sheet folds across its depth: more folds draw more silk lines. */
    val folds: Float = RIBBON_FOLDS,
    /** The sheet's color, added onto the background. */
    val tint: Color = Color.White,
    /** The particles' own clock, so they can rush while the ribbon keeps its pace. */
    val particleTime: Float = time,
    /** How far back along its path (in particle-clock time) each particle's trail reaches; 0 draws dots. */
    val streak: Float = 0f,
    /** Particle size, 1 at rest; loud passages make them bigger, as on the console. */
    val particleSize: Float = 1f,
)

internal const val RIBBON_FOLDS = 4f

internal const val RIBBON_SPEED = 0.25f

/**
 * Where the ribbon's clock starts: a quarter turn of its main sway, so it first rises from the
 * lower left to the right as in the console frames, instead of starting as an arch.
 */
internal const val RIBBON_START = (kotlin.math.PI / 2).toFloat()
internal const val RIBBON_OPACITY = 0.75f
internal const val RIBBON_PARTICLE_OPACITY = 0.75f
internal const val RIBBON_RESOLUTION = 50
private const val PARTICLE_COUNT = 400
private const val PARTICLE_SIZE = 8f

/**
 * A point of the ribbon sheet for grid position ([x], [y]) in -1..1: x across the screen, y along
 * the sheet's depth. Returns (screen x, screen y, depth) in normalized device units (y up). [ratio]
 * is the window's aspect ratio clamped to 1..2, times 0.375.
 */
internal fun ribbonVertex(
    x: Float,
    y: Float,
    time: Float,
    ratio: Float,
    amplitude: Float = 1f,
    folds: Float = RIBBON_FOLDS,
): FloatArray = FloatArray(3).also { ribbonVertexInto(it, x, y, time, ratio, amplitude, folds) }

/** [ribbonVertex] written to [out], for the per-frame mesh. */
internal fun ribbonVertexInto(
    out: FloatArray,
    x: Float,
    y: Float,
    time: Float,
    ratio: Float,
    amplitude: Float,
    folds: Float,
) {
    var vy = cos(y * folds) * cos(y + time / 5f + x) / 8f * amplitude
    val c = noise(x * 7f / 4f, vy * 7f, y * 7f) / 15f
    vy += c + cos(x * 2f - time) * ratio / 2f - 0.3f
    out[0] = x
    out[1] = vy
    out[2] = y + c
}

/**
 * How bright the sheet is where it turns edge-on to the viewer: 1 minus the z of the surface
 * normal, eased, times [opacity]. Overlapping layers add up, which draws the folds and silk lines.
 */
internal fun ribbonAlpha(
    here: FloatArray,
    alongX: FloatArray,
    alongY: FloatArray,
    opacity: Float,
): Float =
    edgeOnAlpha(
        alongX[0] - here[0],
        alongX[1] - here[1],
        alongX[2] - here[2],
        alongY[0] - here[0],
        alongY[1] - here[1],
        alongY[2] - here[2],
        opacity,
    )

/** [ribbonAlpha] from the two edge vectors (a along x, b along y). */
private fun edgeOnAlpha(
    ax: Float,
    ay: Float,
    az: Float,
    bx: Float,
    by: Float,
    bz: Float,
    opacity: Float,
): Float {
    val nx = ay * bz - az * by
    val ny = az * bx - ax * bz
    val nz = ax * by - ay * bx
    val length = sqrt(nx * nx + ny * ny + nz * nz)
    if (length == 0f) return 0f
    val edgeOn = 1f - abs(nz / length)
    return (1f - cos(edgeOn * edgeOn)) * opacity
}

/** Triangle indices for the ribbon grid, two per cell (2,500 vertices fit a short). */
internal val ribbonIndices: ShortArray by lazy {
    val n = RIBBON_RESOLUTION
    val indices = ShortArray(6 * (n - 1) * (n - 1))
    var i = 0
    for (column in 0 until n - 1) {
        for (row in 0 until n - 1) {
            val a = column * n + row
            val b = a + n
            for (index in intArrayOf(a, b, a + 1, a + 1, b, b + 1)) indices[i++] = index.toShort()
        }
    }
    indices
}

/**
 * The ribbon's per-frame buffers, kept between frames: the sheet is 2,500 vertices drawn eight
 * times a frame, and building lists of boxed points and colors for that cost about 1.7 MB of
 * garbage a frame. Drawing happens on the UI thread, one ribbon at a time.
 */
private object RibbonMesh {
    const val N = RIBBON_RESOLUTION
    val gx = FloatArray(N * N)
    val gy = FloatArray(N * N)
    val gz = FloatArray(N * N)
    val positions = FloatArray(2 * N * N)
    val levels = IntArray(N * N)
    val vertex = FloatArray(3)
}

/**
 * Draws the ribbon sheet, added onto what's below. drawVertices has no anti-aliasing: where the
 * sheet turns edge-on (its crests and troughs) the triangles get thinner than a pixel and the silk
 * lines would step, so the mesh is drawn once per sub-pixel offset at a share of its strength,
 * which adds up to 8x multisampling.
 */
internal fun DrawScope.drawRibbon(frame: RibbonFrame) {
    val n = RibbonMesh.N
    val ratio = (size.width / size.height).coerceIn(1f, 2f) * 0.375f
    val (gx, gy, gz) = Triple(RibbonMesh.gx, RibbonMesh.gy, RibbonMesh.gz)
    val vertex = RibbonMesh.vertex
    for (index in 0 until n * n) {
        ribbonVertexInto(
            vertex,
            (index / n) * 2f / (n - 1) - 1f,
            (index % n) * 2f / (n - 1) - 1f,
            frame.time,
            ratio,
            frame.amplitude,
            frame.folds,
        )
        gx[index] = vertex[0]
        gy[index] = vertex[1]
        gz[index] = vertex[2]
    }
    val positions = RibbonMesh.positions
    val levels = RibbonMesh.levels
    for (column in 0 until n) {
        for (row in 0 until n) {
            val index = column * n + row
            // Neighbors along each grid direction; the last row and column look backwards.
            val x = if (column < n - 1) index + n else index - n
            val y = if (row < n - 1) index + 1 else index - 1
            val xSign = if (column < n - 1) 1f else -1f
            val ySign = if (row < n - 1) 1f else -1f
            val alpha =
                edgeOnAlpha(
                    (gx[x] - gx[index]) * xSign,
                    (gy[x] - gy[index]) * xSign,
                    (gz[x] - gz[index]) * xSign,
                    (gx[y] - gx[index]) * ySign,
                    (gy[y] - gy[index]) * ySign,
                    (gz[y] - gz[index]) * ySign,
                    frame.opacity,
                ).coerceIn(0f, 1f)
            positions[2 * index] = (gx[index] + 1f) / 2f * size.width
            positions[2 * index + 1] = (1f - gy[index] - frame.lift) / 2f * size.height
            levels[index] = (alpha * 255f + 0.5f).toInt()
        }
    }
    val tint = frame.tint
    val rgb = ((tint.red * 255f + 0.5f).toInt() shl 16) or ((tint.green * 255f + 0.5f).toInt() shl 8) or (tint.blue * 255f + 0.5f).toInt()
    drawRibbonMesh(positions, levels, rgb, ribbonIndices)
}

/** Pass [pass]'s vertex colors (ARGB): [rgb] with that pass's share of each vertex's [levels]. */
internal fun ribbonPassColors(
    levels: IntArray,
    rgb: Int,
    pass: Int,
    out: IntArray,
) {
    val passes = ribbonSampleOffsets.size
    for (index in levels.indices) out[index] = (ribbonPassLevel(levels[index], pass, passes) shl 24) or rgb
}

/**
 * One pass's share of an 8-bit [level]. Colors are 8-bit, so plain shares rounded alike in every
 * pass would add up to only 32 of the 256 levels (blocky bands along the mesh cells); spreading
 * the remainder over the passes adds up to [level] exactly.
 */
internal fun ribbonPassLevel(
    level: Int,
    pass: Int,
    passes: Int,
): Int = (level + pass) / passes

/** Sub-pixel sample positions for the ribbon's passes: the standard 8x multisampling pattern. */
internal val RIBBON_SAMPLE_OFFSETS_8 =
    listOf(1 to -3, -1 to 3, 5 to 1, -3 to -5, -5 to 5, -7 to -1, 3 to 7, 7 to -7)
        .map { (x, y) -> Offset(x / 16f, y / 16f) }

/** 4x multisampling's rotated grid, and a single pass. */
private val RIBBON_SAMPLE_OFFSETS_4 = listOf(-1 to -3, 3 to -1, 1 to 3, -3 to 1).map { (x, y) -> Offset(x / 8f, y / 8f) }
private val RIBBON_SAMPLE_OFFSETS_1 = listOf(Offset.Zero)

/** The passes the ribbon draws now (Visualizer quality: 8, 4 or 1); set on the UI thread. */
internal var ribbonSampleOffsets: List<Offset> = RIBBON_SAMPLE_OFFSETS_8
    private set

internal fun setRibbonPasses(passes: Int) {
    ribbonSampleOffsets =
        when {
            passes >= 8 -> RIBBON_SAMPLE_OFFSETS_8
            passes >= 4 -> RIBBON_SAMPLE_OFFSETS_4
            else -> RIBBON_SAMPLE_OFFSETS_1
        }
}

/**
 * Draws the ribbon mesh added onto what's below, once per [ribbonSampleOffsets] offset with
 * [ribbonPassColors], straight from the arrays ([positions] as x, y pairs; [levels] per vertex,
 * 0..255; [rgb] the tint). Platform code: Skia on desktop, the framework canvas on Android.
 */
internal expect fun DrawScope.drawRibbonMesh(
    positions: FloatArray,
    levels: IntArray,
    rgb: Int,
    indices: ShortArray,
)

private class ParticleSeed(
    val x: Float,
    val y: Float,
    val size: Float,
)

private val particleSeeds: List<ParticleSeed> =
    Random(0x51EED).let { random ->
        List(PARTICLE_COUNT) { ParticleSeed(random.nextFloat(), random.nextFloat(), random.nextFloat().pow(10) * PARTICLE_SIZE + 3f) }
    }

/** A particle's place this frame, reused so drawing 400 particles allocates nothing. */
private class ParticlePoint {
    var x = 0f
    var y = 0f
    var alpha = 0f
}

private val particleNow = ParticlePoint()
private val particleTrail = ParticlePoint()

/** One soft glow (white center, 60% halfway, clear edge), drawn scaled for every particle. */
private val particleSprite: ImageBitmap by lazy {
    ImageBitmap(SPRITE_SIZE, SPRITE_SIZE).also { image ->
        val half = SPRITE_SIZE / 2f
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(SPRITE_SIZE.toFloat(), SPRITE_SIZE.toFloat())) {
            drawCircle(
                Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0.6f), Color.Transparent), Offset(half, half), half),
                radius = half,
                center = Offset(half, half),
            )
        }
    }
}

private const val SPRITE_SIZE = 64

/** Particles drifting along the ribbon and twinkling, added onto what's below. */
internal fun DrawScope.drawRibbonParticles(frame: RibbonFrame) {
    val ratio = (size.width / size.height).coerceIn(1f, 2f) * 0.375f
    val pixel = size.height / 1080f
    val sprite = particleSprite
    for (seed in particleSeeds) {
        val now = particleNow
        if (!particlePosition(seed, frame.particleTime, frame, ratio, now)) continue
        val centerX = (now.x + 1f) / 2f * size.width
        val centerY = (1f - now.y - frame.lift) / 2f * size.height
        val radius = seed.size * pixel / 2f * frame.particleSize
        val trail = particleTrail
        if (frame.streak > 0f && particlePosition(seed, frame.particleTime - frame.streak, frame, ratio, trail) &&
            abs(trail.x - now.x) < 0.5f
        ) {
            // Fast particles leave a short trail, like the console's streaks in loud passages:
            // three segments brightening toward the particle.
            val startX = (trail.x + 1f) / 2f * size.width
            val startY = (1f - trail.y - frame.lift) / 2f * size.height
            for (segment in 0 until TRAIL_SEGMENTS) {
                val from = segment.toFloat() / TRAIL_SEGMENTS
                val to = (segment + 1f) / TRAIL_SEGMENTS
                drawLine(
                    Color.White,
                    start = Offset(startX + (centerX - startX) * from, startY + (centerY - startY) * from),
                    end = Offset(startX + (centerX - startX) * to, startY + (centerY - startY) * to),
                    strokeWidth = radius,
                    alpha = now.alpha * to * 0.8f,
                    blendMode = BlendMode.Plus,
                )
            }
        }
        val scale = 2f * radius / SPRITE_SIZE
        withTransform({
            translate(centerX - radius, centerY - radius)
            scale(scale, scale, Offset.Zero)
        }) {
            drawImage(sprite, alpha = now.alpha, blendMode = BlendMode.Plus)
        }
    }
}

private const val TRAIL_SEGMENTS = 3

/**
 * A particle's place (x, y, alpha in normalized device units) at particle-clock [time], written
 * to [out]: it drifts and bobs on its own clock and rides the ribbon's wave on the ribbon's
 * clock. False when it's dark.
 */
private fun particlePosition(
    seed: ParticleSeed,
    time: Float,
    frame: RibbonFrame,
    ratio: Float,
    out: ParticlePoint,
): Boolean {
    val x = fract(time * (seed.x - 0.5f) / 15f + seed.y * 50f - frame.drift) * 2f - 1f
    var y = sin(time * (seed.y + 1.5f) / 4f + seed.x * 100f)
    y /= (6f - seed.x * 4f * seed.y) / ratio
    val mixAmount = y * 0.5f + 0.5f
    val variance =
        (
            sin(time * (seed.x + 0.5f) * 12f + seed.y * 10f) * (1f - mixAmount) +
                sin(time * (seed.y + 1.5f) * 6f + seed.x * 4f) * mixAmount
        ) *
            seed.x + seed.y
    val alpha = (frame.particleOpacity * variance * variance).coerceIn(0f, 1f)
    if (alpha <= 0.01f) return false
    val wave = frame.time
    y += cos(seed.y * 4f) * cos(x + seed.y + wave / 5f) / 8f * frame.amplitude + cos(x * 2f - wave) * ratio / 2f - 0.28f
    out.x = x
    out.y = y
    out.alpha = alpha
    return true
}

private fun fract(value: Float) = value - floor(value)

private fun hash(n: Float): Float = fract(sin(n) * 43758.5453f)

/** Value noise, as in the original shader. */
private fun noise(
    px: Float,
    py: Float,
    pz: Float,
): Float {
    val fx = fract(px).let { it * it * (3f - 2f * it) }
    val fy = fract(py).let { it * it * (3f - 2f * it) }
    val fz = fract(pz).let { it * it * (3f - 2f * it) }
    val n = floor(px) + floor(py) * 57f + floor(pz) * 113f

    fun lerp(
        a: Float,
        b: Float,
        t: Float,
    ) = a + (b - a) * t
    return lerp(
        lerp(lerp(hash(n), hash(n + 1f), fx), lerp(hash(n + 57f), hash(n + 58f), fx), fy),
        lerp(lerp(hash(n + 113f), hash(n + 114f), fx), lerp(hash(n + 170f), hash(n + 171f), fx), fy),
        fz,
    )
}
