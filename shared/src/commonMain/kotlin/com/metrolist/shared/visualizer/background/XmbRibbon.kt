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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.VertexMode
import androidx.compose.ui.graphics.Vertices
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
): FloatArray {
    var vy = cos(y * folds) * cos(y + time / 5f + x) / 8f * amplitude
    val c = noise(x * 7f / 4f, vy * 7f, y * 7f) / 15f
    vy += c + cos(x * 2f - time) * ratio / 2f - 0.3f
    return floatArrayOf(x, vy, y + c)
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
): Float {
    val ax = alongX[0] - here[0]
    val ay = alongX[1] - here[1]
    val az = alongX[2] - here[2]
    val bx = alongY[0] - here[0]
    val by = alongY[1] - here[1]
    val bz = alongY[2] - here[2]
    val nx = ay * bz - az * by
    val ny = az * bx - ax * bz
    val nz = ax * by - ay * bx
    val length = sqrt(nx * nx + ny * ny + nz * nz)
    if (length == 0f) return 0f
    val edgeOn = 1f - abs(nz / length)
    return (1f - cos(edgeOn * edgeOn)) * opacity
}

/** Triangle indices for the ribbon grid, two per cell. */
internal val ribbonIndices: List<Int> by lazy {
    val n = RIBBON_RESOLUTION
    buildList(6 * (n - 1) * (n - 1)) {
        for (column in 0 until n - 1) {
            for (row in 0 until n - 1) {
                val a = column * n + row
                val b = a + n
                addAll(listOf(a, b, a + 1, a + 1, b, b + 1))
            }
        }
    }
}

/** Draws the ribbon sheet, added onto what's below. */
internal fun DrawScope.drawRibbon(frame: RibbonFrame) {
    val n = RIBBON_RESOLUTION
    val ratio = (size.width / size.height).coerceIn(1f, 2f) * 0.375f
    val grid =
        Array(n * n) { index ->
            val column = index / n
            val row = index % n
            ribbonVertex(column * 2f / (n - 1) - 1f, row * 2f / (n - 1) - 1f, frame.time, ratio, frame.amplitude, frame.folds)
        }
    val positions = ArrayList<Offset>(n * n)
    val alphas = FloatArray(n * n)
    for (column in 0 until n) {
        for (row in 0 until n) {
            val index = column * n + row
            val here = grid[index]
            // Neighbors along each grid direction; the last row and column look backwards.
            val alongX = if (column < n - 1) grid[index + n] else mirror(here, grid[index - n])
            val alongY = if (row < n - 1) grid[index + 1] else mirror(here, grid[index - 1])
            val alpha = ribbonAlpha(here, alongX, alongY, frame.opacity).coerceIn(0f, 1f)
            positions += Offset((here[0] + 1f) / 2f * size.width, (1f - here[1] - frame.lift) / 2f * size.height)
            alphas[index] = alpha
        }
    }
    // Vertex colors are multiplied by the paint color, so it must be white.
    val paint =
        Paint().apply {
            color = Color.White
            blendMode = BlendMode.Plus
        }
    // drawVertices has no anti-aliasing. Where the sheet turns edge-on (its crests and troughs) the
    // triangles get thinner than a pixel and the silk lines would step, so the mesh is drawn once per
    // sub-pixel offset at a fraction of its strength: added up, that's 8x multisampling.
    val passes = RIBBON_SAMPLE_OFFSETS.size
    drawIntoCanvas { canvas ->
        RIBBON_SAMPLE_OFFSETS.forEachIndexed { pass, offset ->
            val colors = List(n * n) { Color(frame.tint.red, frame.tint.green, frame.tint.blue, ribbonPassAlpha(alphas[it], pass, passes)) }
            val vertices = Vertices(VertexMode.Triangles, positions, positions, colors, ribbonIndices)
            canvas.save()
            canvas.translate(offset.x, offset.y)
            canvas.drawVertices(vertices, BlendMode.Modulate, paint)
            canvas.restore()
        }
    }
}

/**
 * One pass's share of [alpha]. Colors are 8-bit, so a plain eighth would round to whole steps
 * in every pass and the sum would keep only 32 of the 256 levels: the dim body of the ribbon
 * showed blocky bands along the mesh cells. Each pass is nudged by a different fraction of one
 * step instead, so the passes round at different points and add up to [alpha] at full precision.
 */
internal fun ribbonPassAlpha(
    alpha: Float,
    pass: Int,
    passes: Int,
): Float = (alpha / passes + (pass - (passes - 1) / 2f) / passes / 255f).coerceIn(0f, 1f)

/** Sub-pixel sample positions for the ribbon's passes: the standard 8x multisampling pattern. */
internal val RIBBON_SAMPLE_OFFSETS =
    listOf(1 to -3, -1 to 3, 5 to 1, -3 to -5, -5 to 5, -7 to -1, 3 to 7, 7 to -7)
        .map { (x, y) -> Offset(x / 16f, y / 16f) }

private fun mirror(
    here: FloatArray,
    previous: FloatArray,
) = floatArrayOf(2 * here[0] - previous[0], 2 * here[1] - previous[1], 2 * here[2] - previous[2])

private class ParticleSeed(
    val x: Float,
    val y: Float,
    val size: Float,
)

private val particleSeeds: List<ParticleSeed> =
    Random(0x51EED).let { random ->
        List(PARTICLE_COUNT) { ParticleSeed(random.nextFloat(), random.nextFloat(), random.nextFloat().pow(10) * PARTICLE_SIZE + 3f) }
    }

/** Particles drifting along the ribbon and twinkling, added onto what's below. */
internal fun DrawScope.drawRibbonParticles(frame: RibbonFrame) {
    val ratio = (size.width / size.height).coerceIn(1f, 2f) * 0.375f
    val pixel = size.height / 1080f
    particleSeeds.forEach { seed ->
        val now = particlePosition(seed, frame.particleTime, frame, ratio) ?: return@forEach
        val center = Offset((now[0] + 1f) / 2f * size.width, (1f - now[1] - frame.lift) / 2f * size.height)
        val alpha = now[2]
        val radius = seed.size * pixel / 2f
        val trail = if (frame.streak > 0f) particlePosition(seed, frame.particleTime - frame.streak, frame, ratio) else null
        if (trail != null && abs(trail[0] - now[0]) < 0.5f) {
            // Fast particles leave a short trail, like the console's streaks in loud passages.
            val start = Offset((trail[0] + 1f) / 2f * size.width, (1f - trail[1] - frame.lift) / 2f * size.height)
            drawLine(
                Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = alpha)), start = start, end = center),
                start = start,
                end = center,
                strokeWidth = radius,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus,
            )
        }
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = alpha * 0.6f), Color.Transparent),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
            blendMode = BlendMode.Plus,
        )
    }
}

/**
 * A particle's (x, y, alpha) in normalized device units at particle-clock [time]: it drifts and
 * bobs on its own clock and rides the ribbon's wave on the ribbon's clock. Null when it's dark.
 */
private fun particlePosition(
    seed: ParticleSeed,
    time: Float,
    frame: RibbonFrame,
    ratio: Float,
): FloatArray? {
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
    if (alpha <= 0.01f) return null
    val wave = frame.time
    y += cos(seed.y * 4f) * cos(x + seed.y + wave / 5f) / 8f * frame.amplitude + cos(x * 2f - wave) * ratio / 2f - 0.28f
    return floatArrayOf(x, y, alpha)
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
