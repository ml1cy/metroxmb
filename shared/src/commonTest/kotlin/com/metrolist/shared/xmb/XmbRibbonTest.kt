package com.metrolist.shared.xmb

import com.metrolist.shared.visualizer.background.RIBBON_RESOLUTION
import com.metrolist.shared.visualizer.background.RIBBON_SAMPLE_OFFSETS
import com.metrolist.shared.visualizer.background.ribbonAlpha
import com.metrolist.shared.visualizer.background.ribbonIndices
import com.metrolist.shared.visualizer.background.ribbonPassAlpha
import com.metrolist.shared.visualizer.background.ribbonVertex
import kotlin.math.cos
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbRibbonTest {
    @Test
    fun meshCoversTheGridWithTwoTrianglesPerCell() {
        val n = RIBBON_RESOLUTION
        assertEquals(6 * (n - 1) * (n - 1), ribbonIndices.size)
        assertEquals(n * n - 1, ribbonIndices.max())
        assertEquals(0, ribbonIndices.min())
    }

    @Test
    fun aSheetFacingTheViewerIsDarkAndAnEdgeOnOneIsBright() {
        val here = floatArrayOf(0f, 0f, 0f)
        // Flat in the screen plane: normal along z.
        assertEquals(0f, ribbonAlpha(here, floatArrayOf(0.1f, 0f, 0f), floatArrayOf(0f, 0.1f, 0f), opacity = 1f))
        // Edge-on: the sheet runs into the screen, normal in the screen plane.
        val edgeOn = ribbonAlpha(here, floatArrayOf(0.1f, 0f, 0f), floatArrayOf(0f, 0f, 0.1f), opacity = 0.75f)
        assertEquals((1f - cos(1f)) * 0.75f, edgeOn, 0.0001f)
        assertEquals(0f, ribbonAlpha(here, here, here, opacity = 1f), "Degenerate cells draw nothing")
    }

    @Test
    fun theRibbonStaysInItsBandAndIsDeterministic() {
        val ratio = 16f / 9f * 0.375f
        for (i in 0..20) {
            for (j in 0..20) {
                val x = i / 10f - 1f
                val y = j / 10f - 1f
                val vertex = ribbonVertex(x, y, time = 3.7f, ratio = ratio)
                assertEquals(x, vertex[0])
                // cos·cos/8, the noise (< 1/15), and the sway (ratio/2), around -0.3.
                assertTrue(vertex[1] in (-0.3f - 0.125f - ratio / 2f - 0.001f)..(-0.3f + 0.125f + ratio / 2f + 1f / 15f + 0.001f))
                assertContentEquals(vertex, ribbonVertex(x, y, time = 3.7f, ratio = ratio))
            }
        }
    }

    @Test
    fun moreFoldsChangeTheSheet() {
        val four = ribbonVertex(0.3f, 0.35f, time = 1f, ratio = 0.5f, folds = 4f)[1]
        val nine = ribbonVertex(0.3f, 0.35f, time = 1f, ratio = 0.5f, folds = 9f)[1]
        assertTrue(four != nine)
    }

    @Test
    fun amplitudeScalesTheFolds() {
        val flat = ribbonVertex(0.3f, 0.2f, time = 1f, ratio = 0.5f, amplitude = 0f)[1]
        val folded = ribbonVertex(0.3f, 0.2f, time = 1f, ratio = 0.5f, amplitude = 2f)[1]
        assertTrue(flat != folded)
    }

    @Test
    fun ribbonPassesKeepFullEightBitPrecision() {
        val passes = RIBBON_SAMPLE_OFFSETS.size
        for (level in 0..255) {
            val alpha = level / 255f
            // Each pass's color is stored in 8 bits, rounded as Compose's Color does.
            val sum = (0 until passes).sumOf { pass -> (ribbonPassAlpha(alpha, pass, passes) * 255f + 0.5f).toInt() }
            assertTrue(kotlin.math.abs(sum - level) <= 1, "level $level came back as $sum")
        }
        // Nothing where the sheet is clear: no haze from the nudges.
        assertEquals(0, (0 until passes).sumOf { pass -> (ribbonPassAlpha(0f, pass, passes) * 255f + 0.5f).toInt() })
    }
}
