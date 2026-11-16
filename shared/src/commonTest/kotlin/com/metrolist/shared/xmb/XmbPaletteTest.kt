package com.metrolist.shared.xmb

import androidx.compose.ui.graphics.Color
import com.metrolist.shared.xmb.theme.XmbPalette
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XmbPaletteTest {
    @Test
    fun hasADistinctColorForEveryMonthAndWrapsOutOfRangeMonths() {
        assertEquals(12, XmbPalette.monthlyColors.size)
        assertEquals(12, XmbPalette.monthlyColors.toSet().size)
        assertEquals(Color(0xFFCBCBCB), XmbPalette.monthColor(1))
        assertEquals(Color(0xFFE3412A), XmbPalette.monthColor(12))
        assertEquals(XmbPalette.monthColor(1), XmbPalette.monthColor(13))
        assertEquals(XmbPalette.monthColor(12), XmbPalette.monthColor(0))
    }

    @Test
    fun brightnessIsFullByDayDimAtNightAndContinuous() {
        assertEquals(XmbPalette.DAY_BRIGHTNESS, XmbPalette.brightness(12f))
        assertEquals(XmbPalette.DAY_BRIGHTNESS, XmbPalette.brightness(8f))
        assertEquals(XmbPalette.NIGHT_BRIGHTNESS, XmbPalette.brightness(23f))
        assertEquals(XmbPalette.NIGHT_BRIGHTNESS, XmbPalette.brightness(3f))
        assertEquals(XmbPalette.brightness(1f), XmbPalette.brightness(25f))
        assertTrue(XmbPalette.brightness(6.5f) in XmbPalette.NIGHT_BRIGHTNESS..XmbPalette.DAY_BRIGHTNESS)
        var previous = XmbPalette.brightness(0f)
        for (minute in 1..24 * 60) {
            val current = XmbPalette.brightness(minute / 60f)
            assertTrue(abs(current - previous) < 0.01f, "Brightness jumps at minute $minute")
            previous = current
        }
    }

    @Test
    fun backgroundIsDarkerAtTheTopLikeRetroArchsRibbonAndDarkerAtNight() {
        val day = XmbPalette.backgroundColors(month = 8, hour = 12f)
        val night = XmbPalette.backgroundColors(month = 8, hour = 23f)
        assertTrue(day.top.luminance() < day.bottom.luminance())
        assertTrue(day.top.luminance() > night.top.luminance())
        assertTrue(day.bottom.luminance() > night.bottom.luminance())
    }

    private fun Color.luminance(): Float = 0.2126f * red + 0.7152f * green + 0.0722f * blue
}
