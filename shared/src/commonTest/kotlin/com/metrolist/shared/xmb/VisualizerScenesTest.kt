package com.metrolist.shared.xmb

import com.metrolist.shared.visualizer.VisualizerQuality
import com.metrolist.shared.visualizer.background.ribbonPassColors
import com.metrolist.shared.visualizer.background.ribbonSampleOffsets
import com.metrolist.shared.visualizer.background.setRibbonPasses
import com.metrolist.shared.visualizer.gaia.GAIA_VIEWS
import com.metrolist.shared.visualizer.gaia.gaiaView
import com.metrolist.shared.visualizer.valley.valleyPalette
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class VisualizerScenesTest {
    @AfterTest
    fun restore() = setRibbonPasses(VisualizerQuality.HIGH.ribbonPasses)

    @Test
    fun valleyHoldsEachPaletteThenBlends() {
        // Held for most of each 28-second stretch, then blended into the next.
        assertEquals(valleyPalette(0f).ground, valleyPalette(15f).ground)
        assertNotEquals(valleyPalette(15f).ground, valleyPalette(26f).ground)
        assertEquals(valleyPalette(28f).ground, valleyPalette(29f).ground)
    }

    @Test
    fun gaiaCameraHoldsEachViewThenEasesToTheNext() {
        assertEquals(GAIA_VIEWS[0], gaiaView(0f))
        assertEquals(GAIA_VIEWS[0], gaiaView(25f))
        assertEquals(GAIA_VIEWS[1], gaiaView(40f))
        val between = gaiaView(36f)
        assertNotEquals(GAIA_VIEWS[0], between)
        assertNotEquals(GAIA_VIEWS[1], between)
        // Round and round: after the far view, back to the close one.
        assertEquals(GAIA_VIEWS[0], gaiaView(160f))
    }

    @Test
    fun qualityLevelsAndRibbonPassesStayExact() {
        assertEquals(VisualizerQuality.BALANCED, VisualizerQuality.of("balanced"))
        assertEquals(VisualizerQuality.HIGH, VisualizerQuality.of("nonsense"))
        for (quality in VisualizerQuality.entries) {
            setRibbonPasses(quality.ribbonPasses)
            assertEquals(quality.ribbonPasses, ribbonSampleOffsets.size)
            // Every level still adds back up exactly across the passes.
            val levels = IntArray(256) { it }
            val sum = IntArray(256)
            val colors = IntArray(256)
            for (pass in ribbonSampleOffsets.indices) {
                ribbonPassColors(levels, 0, pass, colors)
                colors.forEachIndexed { index, color -> sum[index] += color ushr 24 }
            }
            assertEquals(levels.toList(), sum.toList(), "${quality.name}")
        }
    }
}
