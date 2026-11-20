package com.metrolist.shared.visualizer

import androidx.compose.ui.graphics.ImageBitmap
import com.metrolist.shared.ui.utils.pixelsToImageBitmap
import com.metrolist.shared.visualizer.gaia.surfaceMasks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GaiaMasksTest {
    private fun gray(vararg levels: Int): ImageBitmap =
        pixelsToImageBitmap(IntArray(levels.size) { (0xFF shl 24) or (levels[it] * 0x010101) }, levels.size, 1)

    @Test
    fun waterGoesToRedAndLightsToGreen() {
        val packed = surfaceMasks(gray(255, 0, 128), gray(0, 200, 64))
        val pixels = IntArray(3).also { packed.readPixels(it) }
        assertEquals(listOf(0xFFFF0000.toInt(), 0xFF00C800.toInt(), 0xFF804000.toInt()), pixels.toList())
    }

    @Test
    fun mapsOfDifferentSizesAreRejected() {
        assertFailsWith<IllegalArgumentException> { surfaceMasks(gray(1, 2), gray(1)) }
    }
}
